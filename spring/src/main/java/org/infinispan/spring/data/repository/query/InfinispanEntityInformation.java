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

import java.io.Serializable;

import org.springframework.data.repository.core.EntityInformation;

/**
 * <p>
 * Infinispan-specific {@link EntityInformation} extension that allows to find out if a given domain
 * class uses an ID field or an externally provided ID.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public interface InfinispanEntityInformation<T, ID extends Serializable> extends EntityInformation<T, ID> {

   /**
    * Return {@code true} if and only if the domain class represented by this
    * {@link EntityInformation} uses an externally provided ID.
    * 
    * @return {@code true} if and only if the domain class represented by this
    *         {@link EntityInformation} uses an externally provided ID
    */
   boolean usesProvidedId();
}
