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

/**
 * <p>
 * Represents a {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk</code>}'s metadata, i.e.
 * its {@code key} and its {@code size}. In the future further metadata might be added.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public final class ChunkMetadata implements Serializable {

   /** The serialVersionUID */
   private static final long serialVersionUID = -2869886534571482016L;

   private final Object key;

   private final long sizeInBytes;

   ChunkMetadata(Object key, long sizeInBytes) {
      this.key = key;
      this.sizeInBytes = sizeInBytes;
   }

   public Object getKey() {
      return key;
   }

   public long getSizeInBytes() {
      return sizeInBytes;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + (int) (sizeInBytes ^ (sizeInBytes >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ChunkMetadata other = (ChunkMetadata) obj;
      if (key == null) {
         if (other.key != null) return false;
      } else if (!key.equals(other.key)) return false;
      if (sizeInBytes != other.sizeInBytes) return false;
      return true;
   }

   @Override
   public String toString() {
      return "ChunkMetadata [key=" + key + ", sizeInBytes=" + sizeInBytes + "]";
   }
}
