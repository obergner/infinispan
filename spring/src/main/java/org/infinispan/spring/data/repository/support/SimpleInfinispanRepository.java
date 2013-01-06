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
package org.infinispan.spring.data.repository.support;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.spring.InfinispanTemplate;
import org.infinispan.spring.data.repository.InfinispanRepository;
import org.infinispan.spring.data.repository.query.HibernateSearchBackedInfinispanEntityInformation;
import org.infinispan.spring.data.repository.query.InfinispanEntityInformation;
import org.springframework.util.Assert;

/**
 * SimpleInfinispanRepository.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class SimpleInfinispanRepository<T, ID extends java.io.Serializable> implements InfinispanRepository<T, ID> {

   private final InfinispanTemplate infinispanTemplate;

   private final InfinispanEntityInformation<T, ID> entityInformation;

   public SimpleInfinispanRepository(final InfinispanTemplate infinispanTemplate, final Class<T> domainClass) {
      this(infinispanTemplate, new HibernateSearchBackedInfinispanEntityInformation<T, ID>(domainClass));
   }

   public SimpleInfinispanRepository(final InfinispanTemplate infinispanTemplate,
         final InfinispanEntityInformation<T, ID> entityInformation) {
      Assert.notNull(infinispanTemplate, "Must pass 'infinispanTemplate'");
      Assert.notNull(entityInformation, "Must pass 'entityInformation'");
      this.infinispanTemplate = infinispanTemplate;
      this.entityInformation = entityInformation;
   }

   /**
    * @param entity
    * @return
    * @see org.springframework.data.repository.CrudRepository#save(java.lang.Object)
    */
   @Override
   public <S extends T> S save(final S entity) {
      Assert.notNull(entity, "Must pass 'entity");
      Assert.isTrue(!this.entityInformation.usesProvidedId(), "save(S entity) may not be called since domain class "
            + this.entityInformation.getJavaType() + " uses a provided ID - use save(ID id, S entity) instead");
      return this.infinispanTemplate.put(this.entityInformation.getId(entity), entity);
   }

   /**
    * @param entities
    * @return
    * @see org.springframework.data.repository.CrudRepository#save(java.lang.Iterable)
    */
   @Override
   public <S extends T> Iterable<S> save(final Iterable<S> entities) {
      Assert.notNull(entities, "Must pass 'entities");
      for (final S entity : entities) {
         save(entity);
      }
      return entities;
   }

   /**
    * @param id
    * @return
    * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
    */
   @Override
   public T findOne(final ID id) {
      Assert.notNull(id, "Must pass 'id");
      return this.infinispanTemplate.get(id);
   }

   /**
    * @param id
    * @return
    * @see org.springframework.data.repository.CrudRepository#exists(java.io.Serializable)
    */
   @Override
   public boolean exists(final ID id) {
      Assert.notNull(id, "Must pass 'id");
      return this.infinispanTemplate.containsKey(id);
   }

   /**
    * @return
    * @see org.springframework.data.repository.CrudRepository#findAll()
    */
   @Override
   public Iterable<T> findAll() {
      return this.infinispanTemplate.values();
   }

   /**
    * @param ids
    * @return
    * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
    */
   @Override
   public Iterable<T> findAll(final Iterable<ID> ids) {
      Assert.notNull(ids, "Must pass 'ids");
      final List<T> result = new ArrayList<T>();
      for (final ID id : ids) {
         result.add(findOne(id));
      }
      return result;
   }

   /**
    * @return
    * @see org.springframework.data.repository.CrudRepository#count()
    */
   @Override
   public long count() {
      return this.infinispanTemplate.size();
   }

   /**
    * @param id
    * @see org.springframework.data.repository.CrudRepository#delete(java.io.Serializable)
    */
   @Override
   public void delete(final ID id) {
      Assert.notNull(id, "Must pass 'id");
      this.infinispanTemplate.remove(id);
   }

   /**
    * @param entity
    * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Object)
    */
   @Override
   public void delete(final T entity) {
      Assert.notNull(entity, "Must pass 'entity");
      Assert.isTrue(!this.entityInformation.usesProvidedId(), "delete(T entity) may not be called since domain class "
            + this.entityInformation.getJavaType() + " uses a provided ID - use delete(ID id) instead");
      final ID id = this.entityInformation.getId(entity);
      delete(id);
   }

   /**
    * @param entities
    * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Iterable)
    */
   @Override
   public void delete(final Iterable<? extends T> entities) {
      Assert.notNull(entities, "Must pass 'entities");
      for (final T entity : entities) {
         delete(entity);
      }
   }

   /**
    * 
    * @see org.springframework.data.repository.CrudRepository#deleteAll()
    */
   @Override
   public void deleteAll() {
      this.infinispanTemplate.clear();
   }

   /**
    * @param id
    * @param entity
    * @return
    * @see org.infinispan.spring.data.repository.InfinispanRepository#save(java.io.Serializable,
    *      java.lang.Object)
    */
   @Override
   public T save(final ID id, final T entity) {
      Assert.notNull(id, "Must pass 'id");
      Assert.notNull(entity, "Must pass 'entity");
      Assert.isTrue(this.entityInformation.usesProvidedId(),
            "save(ID id, T entity) may not be called since domain class " + this.entityInformation.getJavaType()
                  + " does NOT use a provided ID - use save(T entity) instead");
      return this.infinispanTemplate.put(id, entity);
   }
}
