/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.infinispan.spring;

import org.infinispan.CacheException;
import org.infinispan.api.BasicCache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * BasicCacheAccessor.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public abstract class AbstractBasicCacheAccessor implements InitializingBean {

   private BasicCache<?, ?> basicCache;

   private PersistenceExceptionTranslator persistenceExceptionTranslator;

   /**
    * @throws Exception
    * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
    */
   @Override
   public void afterPropertiesSet() throws Exception {
      if (this.basicCache == null) {
         throw new IllegalStateException("Property 'basicCache' is required, yet has not been set");
      }
   }

   public final BasicCache<?, ?> getBasicCache() {
      return basicCache;
   }

   public final void setBasicCache(BasicCache<?, ?> basicCache) {
      this.basicCache = basicCache;
   }

   public final PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
      if (persistenceExceptionTranslator == null) {
         persistenceExceptionTranslator = new InfinispanPersistenceExceptionTranslator();
      }
      return persistenceExceptionTranslator;
   }

   public final void setPersistenceExceptionTranslator(PersistenceExceptionTranslator persistenceExceptionTranslator) {
      this.persistenceExceptionTranslator = persistenceExceptionTranslator;
   }

   protected DataAccessException convertInfinispanCacheException(CacheException ex) {
      DataAccessException converted = getPersistenceExceptionTranslator().translateExceptionIfPossible(ex);
      return converted != null ? converted : new UncategorizedInfinispanAccessException(ex.getMessage(), ex);
   }
}
