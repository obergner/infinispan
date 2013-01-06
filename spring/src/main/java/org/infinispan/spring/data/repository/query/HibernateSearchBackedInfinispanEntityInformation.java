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
import org.springframework.util.Assert;

/**
 * <p>
 * Infinispan-specific {@link EntityInformation} extension that allows to find out if a given domain
 * class uses an ID field annotated with {@link org.hibernate.search.annotations.DocumentId} or uses
 * an externally provided ID, then being annotated with
 * {@link org.hibernate.search.annotations.ProvidedId}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class HibernateSearchBackedInfinispanEntityInformation<T, ID extends Serializable> implements
      InfinispanEntityInformation<T, ID> {

   private static final IdAccessFactory ID_ACCESS_FACTORY = new IdAccessFactory();

   private final Class<T> domainClass;

   private final IdAccess<T, ID> idAccess;

   public HibernateSearchBackedInfinispanEntityInformation(final Class<T> domainClass) {
      Assert.notNull(domainClass, "Need to pass 'domainClass'");
      this.domainClass = domainClass;
      this.idAccess = ID_ACCESS_FACTORY.of(domainClass);
   }

   /**
    * @param entity
    * @return
    * @see org.springframework.data.repository.core.EntityInformation#isNew(java.lang.Object)
    */
   @Override
   public boolean isNew(final T entity) {
      if (usesProvidedId()) {
         throw new IllegalStateException("Cannot determine if entity " + entity + " is new since it uses a provided ID");
      }
      return this.idAccess.get(entity) == null;
   }

   /**
    * @param entity
    * @return
    * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
    */
   @Override
   public ID getId(final T entity) {
      if (usesProvidedId()) {
         throw new IllegalStateException("Cannot determine ID of entity " + entity + " since it uses a provided ID");
      }
      return this.idAccess.get(entity);
   }

   /**
    * @return
    * @see org.springframework.data.repository.core.EntityInformation#getIdType()
    */
   @Override
   public Class<ID> getIdType() {
      if (usesProvidedId()) {
         throw new IllegalStateException("Cannot determine ID type of entity " + this.domainClass.getName()
               + " since it uses a provided ID");
      }
      return this.idAccess.getIdType();
   }

   /**
    * @return
    * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
    */
   @Override
   public Class<T> getJavaType() {
      return this.domainClass;
   }

   /**
    * Return {@code true} if and only if the domain class represented by this
    * {@link EntityInformation} uses an externally provided ID, i.e. if and only if that domain
    * class is annotated with {@link org.hibernate.search.annotations.ProvidedId}.
    * 
    * @return {@code true} if and only if the domain class represented by this
    *         {@link EntityInformation} uses an externally provided ID, i.e. if and only if that
    *         domain class is annotated with {@link org.hibernate.search.annotations.ProvidedId}
    * @see org.infinispan.spring.data.repository.query.InfinispanEntityInformation#usesProvidedId()
    */
   @Override
   public boolean usesProvidedId() {
      return this.idAccess == null;
   }

   /**
    * @return
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "HibernateSearchBackedInfinispanEntityInformation [domainClass=" + domainClass + ", idAccess=" + idAccess
            + ", iddType=" + getIdType() + ", usesProvidedId=" + usesProvidedId() + "]";
   }
}
