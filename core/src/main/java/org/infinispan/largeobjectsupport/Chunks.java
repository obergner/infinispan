/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.largeobjectsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;

import org.infinispan.affinity.KeyGenerator;
import org.infinispan.config.Configuration;

/**
 * <p>
 * Represents a given <em>Large Object</em>'s collection of {@link Chunk <code>Chunk</code>}s.
 * </p>
 * <p>
 * This class is probably <strong>the</strong> single most important class for realizing
 * INFINISPAN's <code>LargeObjectSupport</code>. Its responsibilities are manifold:
 * <ol>
 * <li>
 * Divide a <em>Large Object</em> into {@link Chunk <code>Chunk</code>}s.</li>
 * <li>
 * Enable lazy iteration over all {@link Chunk <code>Chunk</code>}s a <em>Large Object</em> is
 * divided into. Here, &quot;lazy&quot; refers to the fact that each <code>Chunk</code> is created
 * on demand. Moreover does this class <strong>not</strong> hold references to any
 * <code>Chunk</code>s already produced since otherwise it would threaten to use up the heap space.</li>
 * <li>
 * Assign a unique <code>chunk key</code> to each newly produced <code>Chunk</code>.</li>
 * <li>
 * Make sure that no two <code>Chunk</code>s are stored on the same node in the INFINISPAN cluster.</li>
 * </ol>
 * </p>
 * <p>
 * <strong>IMPORTANT</strong> Each <code>Chunks</code> instance may be used only
 * <strong>once</strong>. More precisely, it disallows iterating over a <em>Large Object</em> more
 * that once. If a client violates this rule, an <code>IllegalStateException</code> will be thrown.<br/>
 * The reason for this restriction is that thus INFINISPAN will avoid to read a
 * <em>Large Object</em> more often than absolutely necessary.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 * 
 * @see <a href="http://community.jboss.org/wiki/LargeObjectSupport">Large Object Support</a>
 */
@NotThreadSafe
public class Chunks implements Iterable<Chunk> {

   private final Object largeObjectKey;

   private final InputStream largeObject;

   private final long maxChunkSizeInBytes;

   private final ChunkKeyProducer chunkKeyProducer;

   private long numberOfAlreadyReadBytes = 0L;

   public Chunks(Object largeObjectKey, InputStream largeObject, Configuration configuration) {
      if (!largeObject.markSupported())
         throw new IllegalArgumentException("The supplied LargeObject InputStream does not "
                  + "support mark(). This, however, is required.");
      this.largeObjectKey = largeObjectKey;
      this.largeObject = largeObject;
      this.maxChunkSizeInBytes = configuration.getMaximumChunkSizeInBytes();
      this.chunkKeyProducer = new ChunkKeyProducer(configuration.getChunkKeyPrefix());
   }

   @Override
   public Iterator<Chunk> iterator() throws IllegalStateException {
      if (allBytesRead())
         throw new IllegalStateException("This Chunks object has already been iterated over. "
                  + "More than one iteration is not supported.");

      return this.new ChunkIterator();
   }

   public LargeObjectMetadata largeObjectMetadata() throws IllegalStateException {
      if (!allBytesRead())
         throw new IllegalStateException(
                  "Cannot create LargeObjectMetadata: this Chunks object has "
                           + "not yet read all bytes from its LargeObject.");
      List<String> allChunkKeys = chunkKeyProducer.chunkKeys();

      return new LargeObjectMetadata(largeObjectKey, maxChunkSizeInBytes, numberOfAlreadyReadBytes,
               allChunkKeys.toArray(new String[allChunkKeys.size()]));
   }

   private boolean allBytesRead() {
      try {
         largeObject.mark(1);
         boolean answer = (largeObject.read() == -1);
         largeObject.reset();
         return answer;
      } catch (IOException e) {
         throw new RuntimeException("Failed to read from/reset LargeObject InputStream: "
                  + e.getMessage(), e);
      }
   }

   private final class ChunkKeyProducer {

      private final List<String> alreadyProducedChunkKeys = new ArrayList<String>();

      private final KeyGenerator<String> keyGenerator;

      ChunkKeyProducer(String keyPrefix) {
         this.keyGenerator = new PrefixingUuidBasedKeyGenerator(keyPrefix);
      }

      List<String> chunkKeys() {
         return Collections.unmodifiableList(alreadyProducedChunkKeys);
      }

      String nextChunkKey() {
         String nextKey = keyGenerator.getKey();
         alreadyProducedChunkKeys.add(nextKey);
         return nextKey;
      }
   }

   private final class ChunkIterator implements Iterator<Chunk> {

      private static final int MAX_BUFFER_SIZE_IN_BYTES = 4 * 1024;

      @Override
      public boolean hasNext() {
         return !allBytesRead();
      }

      @Override
      public Chunk next() {
         try {
            int effectiveBufferSize = effectiveBufferSize();
            ByteArrayOutputStream chunkData = new ByteArrayOutputStream();

            byte[] buffer = new byte[effectiveBufferSize];
            int n = 0;
            int numberOfBytesToReadNext = effectiveBufferSize;
            while ((n = largeObject.read(buffer, 0, numberOfBytesToReadNext)) != -1
                     && (chunkData.size() < maxChunkSizeInBytes) && (numberOfBytesToReadNext > 0)) {
               chunkData.write(buffer, 0, n);
               numberOfAlreadyReadBytes += n;
               numberOfBytesToReadNext = (maxChunkSizeInBytes - chunkData.size() >= effectiveBufferSize) ? effectiveBufferSize
                        : (int) (maxChunkSizeInBytes - chunkData.size());
            }

            String chunkKey = chunkKeyProducer.nextChunkKey();

            return new Chunk(chunkKey, chunkData.toByteArray());
         } catch (IOException e) {
            throw new RuntimeException("Failed to read Chunk from LargeObject InputStream: "
                     + e.getMessage(), e);
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException("ChunkIterator does not support remove()");
      }

      private int effectiveBufferSize() {
         return maxChunkSizeInBytes < MAX_BUFFER_SIZE_IN_BYTES ? (int) maxChunkSizeInBytes
                  : MAX_BUFFER_SIZE_IN_BYTES;
      }
   }
}
