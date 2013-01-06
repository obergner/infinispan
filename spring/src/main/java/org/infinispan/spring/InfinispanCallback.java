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

/**
 * Callback interface for code to be executed against an Infinispan {@link BasicCache}. Used in
 * {@link InfinispanTemplate}'s {@link InfinispanTemplate#execute(InfinispanCallback) execute()}
 * method, often as an anonymous inner class. Typical implementations will call one of the methods
 * on the provided {@code BasicCache} to do whatever they are up to do.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public interface InfinispanCallback<T> {

   /**
    * <p>
    * Method to be called by {@link InfinispanTemplate#execute(InfinispanCallback)}. Implementations
    * need not concern themselves with transaction or exception handling.
    * </p>
    * <p>
    * RuntimeExceptions that are not derived from Infinispan's {@link CacheException} will be
    * treated as application exceptions and be propagated to the caller.
    * </p>
    * 
    * @param cache
    *           The {@link BasicCache} to operate on
    * @return A result object, or {@literal null}
    * @throws CacheException
    */
   T doInInfinispan(BasicCache<?, ?> cache) throws CacheException;
}
