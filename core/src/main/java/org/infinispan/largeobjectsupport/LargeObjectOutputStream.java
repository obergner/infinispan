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
import java.io.OutputStream;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;

import org.infinispan.affinity.KeyGenerator;

/**
 * <p>
 * An {@link java.io.OutputStream <code>OutputStream</code>} for writing <em>Large Object</em>s to a
 * {@link org.infinispan.Cache <code>Cache</code>}. At construction time it takes
 * <ol>
 * <li>
 * a {@code largeObjectKey},</li>
 * <li>
 * a {@code cache},</li>
 * <li>
 * a {@code maxChunkSizeInBytes},</li>
 * <li>
 * a {@code chunkKeyGenerator} and</li>
 * <li>
 * a {@code metadataManager}</li>
 * </ol>
 * and later partitions the byte stream written into chunks of at most {@code maxChunkSizeInBytes}
 * length, assigning each of those chunks a {@code chunkKey} using {@code chunkKeyGenerator},
 * storing it in {@code cache}, generating {@code largeObjectMetadata} and storing it in
 * {@code metadataManager}.
 * </p>
 * <p>
 * <strong>Usage</strong> Use {@link #write(int)} or one of its overloaded variants to store your
 * <em>Large Objects</em> as a sequence of bytes in {@code LargeObjectOutputStream}. When all bytes
 * have been stored you <strong>must</strong> call {@link #close()} since otherwise the last chunk
 * won't be written into the {@code cache}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@NotThreadSafe
public class LargeObjectOutputStream extends OutputStream {

   private final Object largeObjectKey;

   private final Map<Object, Object> cache;

   private final KeyGenerator<Object> chunkKeyGenerator;

   private final long maxChunkSizeInBytes;

   private final LargeObjectMetadataManager metadataManager;

   private final LargeObjectMetadata.Builder metadataBuilder = LargeObjectMetadata.newBuilder();

   private final ByteArrayOutputStream currentChunk = new ByteArrayOutputStream();

   private boolean atLeastOneByteWritten = false;

   public LargeObjectOutputStream(Object largeObjectKey, Map<Object, Object> cache,
            KeyGenerator<Object> chunkKeyGenerator, long maxChunkSizeInBytes,
            LargeObjectMetadataManager metadataManager) {
      this.largeObjectKey = largeObjectKey;
      this.cache = cache;
      this.chunkKeyGenerator = chunkKeyGenerator;
      this.maxChunkSizeInBytes = maxChunkSizeInBytes;
      this.metadataManager = metadataManager;
      this.metadataBuilder.withLargeObjectKey(largeObjectKey).withMaxChunkSizeInBytes(
               maxChunkSizeInBytes);
   }

   @Override
   public void write(int arg0) throws IOException {
      atLeastOneByteWritten = true;
      currentChunk.write(arg0);
      if (isChunkComplete(currentChunk)) {
         storeChunk(currentChunk);
      }
   }

   @Override
   public void close() throws IOException {
      if (!atLeastOneByteWritten) return;
      if (currentChunk.size() > 0) {
         storeChunk(currentChunk);
      }
      metadataManager.storeLargeObjectMetadata(metadataBuilder.build());
   }

   private boolean isChunkComplete(ByteArrayOutputStream chunk) {
      return chunk.size() == maxChunkSizeInBytes;
   }

   private void storeChunk(ByteArrayOutputStream chunk) throws IOException {
      chunk.flush();
      ChunkMetadata newChunkMetadata = new ChunkMetadata(chunkKeyGenerator.getKey(), chunk.size());
      metadataBuilder.addChunkMetadata(newChunkMetadata);
      cache.put(newChunkMetadata.getKey(), chunk.toByteArray());
      chunk.reset();
   }

   @Override
   public String toString() {
      return "LargeObjectOutputStream [largeObjectKey=" + largeObjectKey + ", maxChunkSizeInBytes="
               + maxChunkSizeInBytes + ", atLeastOneByteWritten=" + atLeastOneByteWritten + "]";
   }
}
