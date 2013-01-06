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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.testng.annotations.DataProvider;

/**
 * ExceptionMappings.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class DataProviders {

   @DataProvider(name = "exceptionMappings")
   public static Object[][] exceptionMappings() {
      return new Object[][] {
            new Object[] { new CacheConfigurationException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new ConfigurationException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new EmbeddedCacheManagerStartupException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new JmxDomainConflictException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new DeadlockDetectedException(""), DeadlockLoserDataAccessException.class },
            new Object[] { new InvalidCacheUsageException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new NotSerializableException(""), InvalidDataAccessApiUsageException.class },
            new Object[] { new InvalidTransactionException(""), DataAccessResourceFailureException.class },
            new Object[] { new RpcException(""), TransientDataAccessResourceException.class },
            new Object[] { new SuspectException(""), TransientDataAccessResourceException.class },
            new Object[] { new TimeoutException(""), TransientDataAccessResourceException.class },
            new Object[] { new WriteSkewException(""), DataIntegrityViolationException.class },
            new Object[] { new CacheException(""), UncategorizedInfinispanAccessException.class }, };
   }
}
