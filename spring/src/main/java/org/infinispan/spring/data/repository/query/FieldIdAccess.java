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
import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

final class FieldIdAccess<T, ID extends Serializable> implements IdAccess<T, ID> {

   private final Field idField;

   FieldIdAccess(final Field idField) {
      this.idField = idField;
      ReflectionUtils.makeAccessible(idField);
   }

   /**
    * @param entity
    * @return
    * @see org.infinispan.spring.data.repository.query.IdAccess#get(java.lang.Object)
    */
   @SuppressWarnings("unchecked")
   @Override
   public ID get(final T entity) {
      return (ID) ReflectionUtils.getField(idField, entity);
   }

   /**
    * @return
    * @see org.infinispan.spring.data.repository.query.IdAccess#getIdType()
    */
   @Override
   public Class<ID> getIdType() {
      return (Class<ID>) this.idField.getType();
   }
}