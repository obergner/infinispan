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
package org.infinispan.factories;

import org.infinispan.config.Configuration;
import org.infinispan.config.ConfigurationException;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManager;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManagerImpl;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * LargeObjectMetadataManagerFactory.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@DefaultFactoryFor(classes = LargeObjectMetadataManager.class)
public class LargeObjectMetadataManagerFactory extends AbstractNamedCacheComponentFactory implements
         AutoInstantiableFactory {

   @Override
   public <LargeObjectMetadataManager> LargeObjectMetadataManager construct(
            Class<LargeObjectMetadataManager> componentType) {
      if (log.isDebugEnabled())
         log.debug("Creating new LargeObjectMetadataManager instance ...");
      String largeObjectMetadataCacheName = configuration.getLargeObjectMetadataCacheName();
      if (log.isTraceEnabled())
         log.trace("Using [" + largeObjectMetadataCacheName
                  + "] as name of LargeObjectMetadataCache");

      EmbeddedCacheManager cm = componentRegistry.getGlobalComponentRegistry().getComponent(
               EmbeddedCacheManager.class);
      boolean useDefaultLargeObjectMetadataCache = largeObjectMetadataCacheName
               .equals(FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);

      if (useDefaultLargeObjectMetadataCache) {
         if (log.isTraceEnabled())
            log.trace("Using default LargeObjectMetadataCache");
         if (!cm.cacheExists(largeObjectMetadataCacheName)) {
            cm.defineConfiguration(largeObjectMetadataCacheName,
                     getDefaultLargeObjectMetadataCacheConfig());
         }
      } else {
         if (!cm.cacheExists(largeObjectMetadataCacheName))
            throw new ConfigurationException("The cache named [" + largeObjectMetadataCacheName
                     + "] is configured as the "
                     + "LargeObjectMetadataCache. However, this cache does not exist.");
         if (log.isTraceEnabled())
            log.trace("Using custom LargeObjectMetadataCache");
      }

      if (log.isDebugEnabled())
         log.debug("Finished creating new LargeObjectMetadataManager instance.");
      return (LargeObjectMetadataManager) new LargeObjectMetadataManagerImpl(cm,
               largeObjectMetadataCacheName);
   }

   private Configuration getDefaultLargeObjectMetadataCacheConfig() {
      Configuration config = new Configuration();
      config.fluent().clustering().mode(Configuration.CacheMode.LOCAL);
      return config;
   }
}
