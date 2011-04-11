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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * <p>
 * Collects a <em>Large Object</em>'a metadata necessary to store it in INFINISPAN. Since a
 * <em>Large Object</em> does not fit into a single JVM's heap it needs to be chunked/dissected into
 * fragments. Each chunk will be stored in a different cluster node under its own chunk key.
 * <code>LargeObjectMetadata</code> contains the mapping from a <em>Large Object</em>' own key to
 * its chunk keys, plus additional metadata such as overall size.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class LargeObjectMetadata implements Serializable, Iterable<Object> {

   /** The serialVersionUID */
   private static final long serialVersionUID = 1100258474820117922L;

   private final Object largeObjectKey;

   private final long maximumChunkSizeInBytes;

   private final long totalSizeInBytes;

   private final Object[] chunkKeys;

   public LargeObjectMetadata(Object largeObjectKey, long maximumChunkSizeInBytes,
            long totalSizeInBytes, String[] chunkKeys) {
      this.largeObjectKey = largeObjectKey;
      this.maximumChunkSizeInBytes = maximumChunkSizeInBytes;
      this.totalSizeInBytes = totalSizeInBytes;
      this.chunkKeys = chunkKeys;
   }

   @Override
   public Iterator<Object> iterator() {
      return Arrays.asList(chunkKeys).iterator();
   }

   /**
    * Get the largeObjectKey.
    * 
    * @return the largeObjectKey.
    */
   public final Object getLargeObjectKey() {
      return largeObjectKey;
   }

   /**
    * Get the maximumChunkSizeInBytes.
    * 
    * @return the maximumChunkSizeInBytes.
    */
   public long getMaximumChunkSizeInBytes() {
      return maximumChunkSizeInBytes;
   }

   /**
    * Get the totalSizeInBytes.
    * 
    * @return the totalSizeInBytes.
    */
   public final long getTotalSizeInBytes() {
      return totalSizeInBytes;
   }

   /**
    * Get the chunkKeys.
    * 
    * @return the chunkKeys.
    */
   public final Object[] getChunkKeys() {
      return chunkKeys;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((largeObjectKey == null) ? 0 : largeObjectKey.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      LargeObjectMetadata other = (LargeObjectMetadata) obj;
      if (largeObjectKey == null) {
         if (other.largeObjectKey != null)
            return false;
      } else if (!largeObjectKey.equals(other.largeObjectKey))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "LargeObjectMetadata [largeObjectKey=" + largeObjectKey + ", maximumChunkSizeInBytes="
               + maximumChunkSizeInBytes + ", totalSizeInBytes=" + totalSizeInBytes
               + ", chunkKeys=" + Arrays.toString(chunkKeys) + "]";
   }
}
