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

import java.util.concurrent.ConcurrentMap;

import net.jcip.annotations.ThreadSafe;

/**
 * LargeObjectMetadataImpl.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@ThreadSafe
public class LargeObjectMetadataManagerImpl implements LargeObjectMetadataManager {

   private ConcurrentMap<Object, LargeObjectMetadata<Object>> largeObjectKeyToMetadata;

   /**
    * Create a new LargeObjectMetadataManagerImpl.
    * 
    * @param largeObjectKeyToMetadata
    */
   public LargeObjectMetadataManagerImpl(
            ConcurrentMap<Object, LargeObjectMetadata<Object>> largeObjectKeyToMetadata) {
      this.largeObjectKeyToMetadata = largeObjectKeyToMetadata;
   }

   @Override
   public <K> boolean alreadyUsedByLargeObject(K largeObjectKey) {
      return largeObjectKeyToMetadata.containsKey(largeObjectKey);
   }

   @Override
   public <K> LargeObjectMetadata<K> correspondingLargeObjectMetadata(K largeObjectKey) {
      return (LargeObjectMetadata<K>) largeObjectKeyToMetadata.get(largeObjectKey);
   }

   @Override
   public <K> void storeLargeObjectMetadata(LargeObjectMetadata<K> largeObjectMetadata) {
      largeObjectKeyToMetadata.putIfAbsent(largeObjectMetadata.getLargeObjectKey(),
               (LargeObjectMetadata<Object>) largeObjectMetadata);
   }
}
