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

import net.jcip.annotations.NotThreadSafe;

import org.infinispan.remoting.transport.Address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * Represents a given <em>Large Object</em>'s collection of {@link Chunk <code>Chunk</code>}s.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@NotThreadSafe
public class Chunks<K> implements Iterable<Chunk> {

   private final K largeObjectKey;

   private final InputStream largeObject;

   private final long maxChunkSizeInBytes;

   private final Set<ChunkKeyNodeAddressTuple> alreadyUsedChunkKeysAndNodeAddresses = new HashSet<ChunkKeyNodeAddressTuple>();

   private long numberOfAlreadyReadBytes = 0L;

   /**
    * Create a new Chunks.
    * 
    * @param largeObjectKey
    * @param largeObject
    * @param maxChunkSizeInBytes
    */
   public Chunks(K largeObjectKey, InputStream largeObject, long maxChunkSizeInBytes) {
      if (!largeObject.markSupported())
         throw new IllegalArgumentException("The supplied LargeObject InputStream does not "
                  + "support mark(). This, however, is required.");
      this.largeObjectKey = largeObjectKey;
      this.largeObject = largeObject;
      this.maxChunkSizeInBytes = maxChunkSizeInBytes;
   }

   @Override
   public Iterator<Chunk> iterator() throws IllegalStateException {
      if (allBytesRead())
         throw new IllegalStateException("This Chunks object has already been iterated over. "
                  + "More than one iteration is not supported.");

      return this.new ChunkIterator();
   }

   public LargeObjectMetadata<K> largeObjectMetadata() throws IllegalStateException {
      if (!allBytesRead())
         throw new IllegalStateException(
                  "Cannot create LargeObjectMetadata: this Chunks object has "
                           + "not yet read all bytes from its LargeObject.");
      List<String> allChunkKeys = new ArrayList<String>();
      for (ChunkKeyNodeAddressTuple chunkKeyAndNodeAddress : alreadyUsedChunkKeysAndNodeAddresses) {
         allChunkKeys.add(chunkKeyAndNodeAddress.chunkKey);
      }

      return new LargeObjectMetadata<K>(largeObjectKey, numberOfAlreadyReadBytes,
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

   private ChunkKeyNodeAddressTuple nextChunkKeyAndNodeAddress() {
      // FIXME: We need both a proper chunk key and node address
      String chunkKey = UUID.randomUUID().toString();
      Address nodeAddress = new Address() {
      };
      return new ChunkKeyNodeAddressTuple(chunkKey, nodeAddress);
   }

   private static class ChunkKeyNodeAddressTuple {

      private final String chunkKey;

      private final Address nodeAddress;

      /**
       * Create a new ChunkKeyAddressTuple.
       * 
       * @param chunkKey
       * @param nodeAddress
       */
      ChunkKeyNodeAddressTuple(String chunkKey, Address nodeAddress) {
         this.chunkKey = chunkKey;
         this.nodeAddress = nodeAddress;
      }
   }

   private class ChunkIterator implements Iterator<Chunk> {

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

            ChunkKeyNodeAddressTuple chunkKeyAndNodeAddress = nextChunkKeyAndNodeAddress();
            alreadyUsedChunkKeysAndNodeAddresses.add(chunkKeyAndNodeAddress);

            return new Chunk(chunkKeyAndNodeAddress.chunkKey, chunkKeyAndNodeAddress.nodeAddress,
                     chunkData.toByteArray());
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
