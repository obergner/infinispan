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

import org.infinispan.CacheConfigurationException;
import org.infinispan.CacheException;
import org.infinispan.InvalidCacheUsageException;
import org.infinispan.config.ConfigurationException;
import org.infinispan.jmx.JmxDomainConflictException;
import org.infinispan.manager.EmbeddedCacheManagerStartupException;
import org.infinispan.marshall.NotSerializableException;
import org.infinispan.remoting.RpcException;
import org.infinispan.remoting.transport.jgroups.SuspectException;
import org.infinispan.transaction.WriteSkewException;
import org.infinispan.transaction.xa.InvalidTransactionException;
import org.infinispan.util.concurrent.TimeoutException;
import org.infinispan.util.concurrent.locks.DeadlockDetectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Translate Infinispan's {@link CacheException} hierarchy into Spring's persistence
 * technology-agnostic {@link DataAccessException} hierarchy.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class InfinispanPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

   /**
    * @param ex
    * @return
    * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(java.lang.RuntimeException)
    */
   @Override
   public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
      if ((ex instanceof CacheConfigurationException) || (ex instanceof ConfigurationException)
            || (ex instanceof EmbeddedCacheManagerStartupException) || (ex instanceof JmxDomainConflictException)) {
         return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
      }
      if (ex instanceof DeadlockDetectedException) {
         return new DeadlockLoserDataAccessException(ex.getMessage(), ex);
      }
      if ((ex instanceof InvalidCacheUsageException) || (ex instanceof NotSerializableException)) {
         return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
      }
      if (ex instanceof InvalidTransactionException) {
         return new DataAccessResourceFailureException(ex.getMessage(), ex);
      }
      if ((ex instanceof RpcException) || (ex instanceof SuspectException) || (ex instanceof TimeoutException)) {
         return new TransientDataAccessResourceException(ex.getMessage(), ex);
      }
      if (ex instanceof WriteSkewException) {
         return new DataIntegrityViolationException(ex.getMessage(), ex);
      }
      if (ex instanceof CacheException) {
         // This is on a best-effort basis. CacheException is thrown due to several quite different reasons.
         return new UncategorizedInfinispanAccessException(ex.getMessage(), ex);
      }
      return null;
   }
}
