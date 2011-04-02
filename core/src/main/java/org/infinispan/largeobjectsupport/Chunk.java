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

import org.infinispan.remoting.transport.Address;

import java.util.List;

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

   private final List<Address> chunkNodeAddresses;

   private final byte[] data;

   /**
    * Create a new Chunk.
    * 
    * @param chunkKey
    * @param chunkNodeAddresses
    * @param data
    */
   Chunk(String chunkKey, List<Address> chunkNodeAddresses, byte[] data) {
      this.chunkKey = chunkKey;
      this.chunkNodeAddresses = chunkNodeAddresses;
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
    * Get the chunkNodeAddress.
    * 
    * @return the chunkNodeAddress.
    */
   public final List<Address> getChunkNodeAddresses() {
      return chunkNodeAddresses;
   }

   /**
    * Tests whether this <code>Chunk</code> is stored - or is to be stored - in the INFINISPAN node
    * having the supplied <code>nodeAddress</code>.
    * 
    * @param nodeAddress
    * @return
    */
   public final boolean isStoredInNode(Address nodeAddress) {
      return chunkNodeAddresses.equals(nodeAddress);
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
      result = prime * result + ((chunkNodeAddresses == null) ? 0 : chunkNodeAddresses.hashCode());
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
      if (chunkNodeAddresses == null) {
         if (other.chunkNodeAddresses != null)
            return false;
      } else if (!chunkNodeAddresses.equals(other.chunkNodeAddresses))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Chunk [chunkKey=" + chunkKey + ", chunkNodeAddress=" + chunkNodeAddresses + ", data="
               + data + "]";
   }
}
