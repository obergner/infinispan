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
package org.infinispan.spring.data.repository.query;

import java.lang.reflect.Method;

import org.infinispan.spring.data.repository.InfinispanRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Infinispan specific {@link QueryMethod}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class InfinispanQueryMethod extends QueryMethod {

   /**
    * Create a new InfinispanQueryMethod.
    * 
    * @param method
    *           The {@link InfinispanRepository} method to create a Hibernate search query from.
    *           Must not be {@literal null}.
    * @param metadata
    *           The {@link RepositoryMetadata} describing the {@link InfinispanRepository} we are
    *           dealing with. Must not be {@literal null}.
    */
   public InfinispanQueryMethod(final Method method, final RepositoryMetadata metadata) {
      super(method, metadata);
   }
}
