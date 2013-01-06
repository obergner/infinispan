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

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.ProvidedId;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * IdAccessFactory.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
final class IdAccessFactory {

   <T, ID extends Serializable> IdAccess<T, ID> of(final Class<T> domainClass) {
      @SuppressWarnings("unchecked")
      final Field fieldAnnotatedWithId = uniqueFieldAnnotatedWithOneOf(domainClass, DocumentId.class, Id.class);
      @SuppressWarnings("unchecked")
      final Method methodAnnotatedWithId = uniqueGetterAnnotatedWithOneOf(domainClass, DocumentId.class, Id.class);

      if (fieldAnnotatedWithId != null && methodAnnotatedWithId != null) {
         throw new IllegalArgumentException("In domain class " + domainClass.getName() + " field "
               + fieldAnnotatedWithId + " as well as method " + methodAnnotatedWithId
               + " are both annotated with one of "
               + Arrays.toString(new String[] { DocumentId.class.getName(), Id.class.getName() }));
      }

      final IdAccess<T, ID> answer;
      if (fieldAnnotatedWithId == null && methodAnnotatedWithId == null) {
         if (AnnotationUtils.findAnnotation(domainClass, ProvidedId.class) == null) {
            throw new IllegalArgumentException("Domain class " + domainClass.getName()
                  + " has no field/accessor annotated with either " + DocumentId.class.getName() + " or "
                  + Id.class.getName() + " AND is not annotated with " + ProvidedId.class.getName() + ", either");
         }
         answer = null;
      } else if (fieldAnnotatedWithId != null) {
         if (AnnotationUtils.findAnnotation(domainClass, ProvidedId.class) != null) {
            throw new IllegalArgumentException("Domain class " + domainClass.getName() + " has field "
                  + fieldAnnotatedWithId.getName() + " annotated with " + Id.class.getName() + "/"
                  + DocumentId.class.getName() + " AND is annotated with " + ProvidedId.class.getName());
         }
         answer = new FieldIdAccess<T, ID>(fieldAnnotatedWithId);
      } else {
         if (AnnotationUtils.findAnnotation(domainClass, ProvidedId.class) != null) {
            throw new IllegalArgumentException("Domain class " + domainClass.getName() + " has accessor "
                  + methodAnnotatedWithId.getName() + " annotated with " + DocumentId.class.getName()
                  + " AND is annotated with " + ProvidedId.class.getName());
         }
         answer = new GetterIdAccess<T, ID>(methodAnnotatedWithId);
      }

      return answer;
   }

   private <T> Field uniqueFieldAnnotatedWithOneOf(final Class<T> domainClass,
         final Class<? extends Annotation>... annotations) {
      final AtomicReference<Field> localIdField = new AtomicReference<Field>();
      ReflectionUtils.doWithFields(domainClass, new FieldCallback() {
         @Override
         public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
            boolean annotated = false;
            for (final Class<? extends Annotation> annotation : annotations) {
               if (field.getAnnotation(annotation) != null) {
                  annotated = true;
                  break;
               }
            }
            if (!annotated) {
               return;
            }
            if (!localIdField.compareAndSet(null, field)) {
               throw new IllegalArgumentException("In domain class " + domainClass.getName()
                     + " there are at least two fields annotated with one of " + Arrays.toString(annotations) + ": "
                     + localIdField.get().getName() + " and " + field.getName());
            }
         }
      });
      return localIdField.get();
   }

   private <T> Method uniqueGetterAnnotatedWithOneOf(final Class<T> domainClass,
         final Class<? extends Annotation>... annotations) {
      final AtomicReference<Method> idAccessorRef = new AtomicReference<Method>();
      ReflectionUtils.doWithMethods(domainClass, new MethodCallback() {
         @Override
         public void doWith(final Method method) throws IllegalArgumentException, IllegalAccessException {
            boolean annotated = false;
            for (final Class<? extends Annotation> annotation : annotations) {
               if (method.isAnnotationPresent(annotation)) {
                  annotated = true;
                  break;
               }
            }
            if (!annotated) {
               return;
            }
            if (!idAccessorRef.compareAndSet(null, method)) {
               throw new IllegalArgumentException("In domain class " + domainClass.getName()
                     + " there are at least two methods annotated with one of " + Arrays.toString(annotations) + ": "
                     + idAccessorRef.get().getName() + " and " + method.getName());
            }
         }
      });
      if (idAccessorRef.get() == null) {
         return null;
      }

      final Method idAccessor = idAccessorRef.get();
      final PropertyDescriptor idProperty = BeanUtils.findPropertyForMethod(idAccessor);
      if (idProperty == null) {
         throw new IllegalArgumentException("The method " + idAccessor.getName() + " in class " + domainClass.getName()
               + " is annotated with one of " + Arrays.toString(annotations)
               + ", yet it is neither a getter nor a setter");
      }

      return idProperty.getReadMethod();
   }
}
