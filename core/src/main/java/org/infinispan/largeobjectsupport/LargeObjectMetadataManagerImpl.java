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

import org.infinispan.affinity.KeyGenerator;
import org.infinispan.manager.CacheContainer;

/**
 * Default implementation of {@link LargeObjectMetadata <code>LargeObjectMetadata</code>}, backed by
 * {@link java.util.concurrent.ConcurrentHashMap <code>ConcurrentHashMap</code>}. This will usually
 * be a <code>Cache</code>.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@ThreadSafe
public class LargeObjectMetadataManagerImpl implements LargeObjectMetadataManager {

   private final CacheContainer cacheContainer;

   private final String largeObjectMetadataCacheName;

   public LargeObjectMetadataManagerImpl(CacheContainer cacheContainer,
            final String largeObjectMetadataCacheName) {
      this.cacheContainer = cacheContainer;
      this.largeObjectMetadataCacheName = largeObjectMetadataCacheName;
   }

   @Override
   public <K> boolean alreadyUsedByLargeObject(K largeObjectKey) {
      return largeObjectKeyToMetadata().containsKey(largeObjectKey);
   }

   @Override
   public <K> LargeObjectMetadata correspondingLargeObjectMetadata(K largeObjectKey) {
      return largeObjectKeyToMetadata().get(largeObjectKey);
   }

   @Override
   public void storeLargeObjectMetadata(LargeObjectMetadata largeObjectMetadata) {
      largeObjectKeyToMetadata().putIfAbsent(largeObjectMetadata.getLargeObjectKey(),
               largeObjectMetadata);
   }

   private ConcurrentMap<Object, LargeObjectMetadata> largeObjectKeyToMetadata() {
      return cacheContainer.getCache(largeObjectMetadataCacheName);
   }

   @Override
   public KeyGenerator<Object> chunkKeyGenerator() {
      return new UuidBasedKeyGenerator();
   }
}
