/**
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *   ~
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

package org.infinispan.spring.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * <p>
 * A {@link org.springframework.beans.factory.FactoryBean <code>FactoryBean</code>} for creating a
 * native <em>default</em> Infinispan {@link org.infinispan.Cache <code>org.infinispan.Cache</code>}
 * , delegating to a {@link #setInfinispanCacheContainer(CacheContainer) <code>configurable</code>}
 * {@link org.infinispan.manager.CacheContainer <code>org.infinispan.manager.CacheContainer</code>}.
 * A default <code>Cache</code> is a <code>Cache</code> that uses its <code>CacheContainer</code>'s
 * default settings. This is contrary to a <em>named</em> <code>Cache</code> where select settings
 * from a <code>CacheContainer</code>'s default configuration may be overridden with settings
 * specific to that <code>Cache</code>.
 * </p>
 * <p>
 * In addition to creating a <code>Cache</code> this <code>FactoryBean</code> does also control that
 * <code>Cache</code>'s {@link org.infinispan.lifecycle.Lifecycle lifecycle} by shutting it down
 * when the enclosing Spring application context is closed. It is therefore advisable to
 * <em>always</em> use this <code>FactoryBean</code> when creating a <code>Cache</code>.
 * </p>
 * 
 * @author <a href="mailto:olaf DOT bergner AT gmx DOT de">Olaf Bergner</a>
 * 
 */
public class InfinispanDefaultCacheFactoryBean<K, V> implements FactoryBean<Cache<K, V>>,
         InitializingBean, DisposableBean {

   protected final Log logger = LogFactory.getLog(getClass());

   private CacheContainer infinispanCacheContainer;

   private Cache<K, V> infinispanCache;

   /**
    * <p>
    * Sets the {@link org.infinispan.manager.CacheContainer
    * <code>org.infinispan.manager.CacheContainer</code>} to be used for creating our
    * {@link org.infinispan.Cache <code>Cache</code>} instance. Note that this is a
    * <strong>mandatory</strong> property.
    * </p>
    * 
    * @param infinispanCacheContainer
    *           The {@link org.infinispan.manager.CacheContainer
    *           <code>org.infinispan.manager.CacheContainer</code>} to be used for creating our
    *           {@link org.infinispan.Cache <code>Cache</code>} instance
    */
   public void setInfinispanCacheContainer(final CacheContainer infinispanCacheContainer) {
      this.infinispanCacheContainer = infinispanCacheContainer;
   }

   /**
    * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
    */
   @Override
   public void afterPropertiesSet() throws Exception {
      if (this.infinispanCacheContainer == null) {
         throw new IllegalStateException("No Infinispan CacheContainer has been set");
      }
      this.logger.info("Initializing named Infinispan cache ...");
      this.infinispanCache = this.infinispanCacheContainer.getCache();
      this.logger.info("New Infinispan cache [" + this.infinispanCache + "] initialized");
   }

   /**
    * @see org.springframework.beans.factory.FactoryBean#getObject()
    */
   @Override
   public Cache<K, V> getObject() throws Exception {
      return this.infinispanCache;
   }

   /**
    * @see org.springframework.beans.factory.FactoryBean#getObjectType()
    */
   @Override
   public Class<? extends Cache> getObjectType() {
      return this.infinispanCache != null ? this.infinispanCache.getClass() : Cache.class;
   }

   /**
    * Always returns <code>true</code>.
    * 
    * @return Always <code>true</code>
    * 
    * @see org.springframework.beans.factory.FactoryBean#isSingleton()
    */
   @Override
   public boolean isSingleton() {
      return true;
   }

   /**
    * Shuts down the <code>org.infinispan.Cache</code> created by this <code>FactoryBean</code>.
    * 
    * @see org.springframework.beans.factory.DisposableBean#destroy()
    * @see org.infinispan.Cache#stop()
    */
   @Override
   public void destroy() throws Exception {
      // Probably being paranoid here ...
      if (this.infinispanCache != null) {
         this.infinispanCache.stop();
      }
   }
}
