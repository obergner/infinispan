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
package org.infinispan.spring.data.repository.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * InfinispanMappingContext.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class InfinispanMappingContext extends
      AbstractMappingContext<InfinispanPersistentEntity<?>, InfinispanPersistentProperty> {

   /**
    * @param typeInformation
    * @return
    * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
    */
   @Override
   protected <T> InfinispanPersistentEntity<?> createPersistentEntity(final TypeInformation<T> typeInformation) {
      return new InfinispanPersistentEntity<T>(typeInformation);
   }

   /**
    * @param field
    * @param descriptor
    * @param owner
    * @param simpleTypeHolder
    * @return
    * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field,
    *      java.beans.PropertyDescriptor,
    *      org.springframework.data.mapping.model.MutablePersistentEntity,
    *      org.springframework.data.mapping.model.SimpleTypeHolder)
    */
   @Override
   protected InfinispanPersistentProperty createPersistentProperty(final Field field,
         final PropertyDescriptor descriptor, final InfinispanPersistentEntity<?> owner,
         final SimpleTypeHolder simpleTypeHolder) {
      return new InfinispanPersistentProperty(field, descriptor, owner, simpleTypeHolder);
   }
}
