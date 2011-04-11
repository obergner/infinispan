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

/**
 * <p>
 * A service responsible for handling {@link LargeObjectMetadata <code>LargeObjectMetadata</code>}.
 * Essentially, it is a facade for the <em>LargeObjectMetadata Cache</em>.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 * 
 * @see <a href="http://community.jboss.org/wiki/LargeObjectSupport"><em>Large Object
 *      Support</em></a>
 */
public interface LargeObjectMetadataManager {

   /**
    * Tests whether the supplied <code>LargeObjectKey</code> is already in use by a large object.
    * 
    * @param <K>
    * @param largeObjectKey
    *           A large object's key
    * @return <code>true</code> if a large object is currently stored under this key,
    *         <code>false</code> otherwise
    */
   <K> boolean alreadyUsedByLargeObject(K largeObjectKey);

   /**
    * Tries to retrieve the {@link LargeObjectMetadata <code>LargeObjectMetadata</code>}
    * corresponding to the supplied <code>largeObjectKey</code>. Will return that
    * {@code LargeObjectMetadat} in case a large object is currently stored under the supplied
    * {@code largeObjectKey}. Otherwise it will return {@code null}.
    * 
    * @param largeObjectKey
    *           A large object's key
    * @return The {@link LargeObjectMetadata <code>LargeObjectMetadata</code>} corresponding to the
    *         supplied <code>largeObjectKey</code> or {@code null}
    */
   <K> LargeObjectMetadata correspondingLargeObjectMetadata(K largeObjectKey);

   /**
    * Store the supplied {@link LargeObjectMetadata <code>largeObjectMetadata</code>} for later
    * retrieval. After calling this method, {@code LargeObjectMetadataManager} will assume that a
    * large object is stored under the key contained in {@code largeObjectMetadat}.
    * 
    * @param largeObjectMetadata
    *           {@link LargeObjectMetadata <code>LargeObjectMetadata</code>} to store
    */
   void storeLargeObjectMetadata(LargeObjectMetadata largeObjectMetadata);
}
