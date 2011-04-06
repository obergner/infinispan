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

/**
 * <p>
 * Represents a single chunk/fragment of a <em>Large Object</em> to be stored in INFINISPAN. Since a
 * <em>Large Object</em> is defined as an object the size of which exceeds any given JVM's heap in
 * the cluster we cannot store it as is. Instead, we have to dissect it into smaller chunks, each of
 * which may be stored on a node in the cluster.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class Chunk {

   private final String chunkKey;

   private final byte[] data;

   Chunk(String chunkKey, byte[] data) {
      this.chunkKey = chunkKey;
      this.data = data;
   }

   /**
    * Get the chunkKey.
    * 
    * @return the chunkKey.
    */
   public final String getChunkKey() {
      return chunkKey;
   }

   /**
    * Get the data.
    * 
    * @return the data.
    */
   public final byte[] getData() {
      return data;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((chunkKey == null) ? 0 : chunkKey.hashCode());
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
      Chunk other = (Chunk) obj;
      if (chunkKey == null) {
         if (other.chunkKey != null)
            return false;
      } else if (!chunkKey.equals(other.chunkKey))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Chunk [chunkKey=" + chunkKey + ", data=" + data + "]";
   }
}
