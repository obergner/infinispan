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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * An {@link java.io.InputStream <code>InputStream</code>} for reading a <em>Large Object</em> from
 * a cache.
 * </p>
 * <p>
 * A <code>LargeObjectInputStream</code> receives at construction time references to
 * <ul>
 * <li>
 * a {@link org.infinispan.largeobjectsupport.LargeObjectMetadata <code>LargeObjectMetadata</code>}
 * instance representing the <em>Large Object</em> to read, and</li>
 * <li>
 * a {@link org.infinispan.Cache <code>Cache</code>} instance</li>
 * and subsequently uses those to read all {@link org.infinispan.largeobjectsupport.Chunk <code>
 * Chunk</code>}s from the <code>Cache</code>, returning them as a byte stream to the caller.
 * </ul>
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@NotThreadSafe
public class LargeObjectInputStream extends InputStream {

   private final LargeObjectMetadata<?> largeObjectMetadata;

   private final Map<?, ?> largeObjectCache;

   private byte[] currentChunk;

   private long numberOfBytesRead = 0L;

   private int currentChunkKeyIndex = 0;

   public LargeObjectInputStream(LargeObjectMetadata<?> largeObjectMetadata,
            Map<?, ?> largeObjectCache) {
      // FIXME: Support currentChunk sizes greater than Integer.MAX_VALUE
      if (largeObjectMetadata.getMaximumChunkSizeInBytes() > Integer.MAX_VALUE)
         throw new IllegalArgumentException("Currently, maximum currentChunk size is limited to "
                  + Integer.MAX_VALUE + " bytes. The given maximum currentChunk size of ["
                  + largeObjectMetadata.getMaximumChunkSizeInBytes() + "] exceeds this limit.");
      this.largeObjectMetadata = largeObjectMetadata;
      this.largeObjectCache = largeObjectCache;
   }

   @Override
   public int read() throws IOException {
      if (numberOfBytesRead >= largeObjectMetadata.getTotalSizeInBytes())
         return -1;
      if (isFirstCallToRead() || shouldReadNextChunk()) {
         currentChunk = nextChunk(largeObjectMetadata.getChunkKeys()[currentChunkKeyIndex++]);
      }
      return currentChunk[(int) (numberOfBytesRead++ % largeObjectMetadata
               .getMaximumChunkSizeInBytes())];
   }

   private boolean isFirstCallToRead() {
      return currentChunk == null;
   }

   private byte[] nextChunk(String chunkKey) {
      return byte[].class.cast(largeObjectCache.get(chunkKey));
   }

   private boolean shouldReadNextChunk() {
      return (numberOfBytesRead < largeObjectMetadata.getTotalSizeInBytes())
               && (numberOfBytesRead % largeObjectMetadata.getMaximumChunkSizeInBytes() == 0);
   }

   @Override
   public void close() throws IOException {
      // Even a single currentChunk might be quite big.
      this.currentChunk = null;
      super.close();
   }
}
