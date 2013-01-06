package org.infinispan.spring.data.repository.mapping;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.hibernate.search.annotations.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.data.repository.mapping.InfinispanPersistentPropertyTest")
public class InfinispanPersistentPropertyTest {

   @Test
   public void shouldRecognizePropertyAnnotatedWithTransientAsBeingTransient() throws Exception {
      final String fieldName = Sample.class.getDeclaredField("transientAnnotatedProperty").getName();

      final BeanInfo sampleBeanInfo = Introspector.getBeanInfo(Sample.class);
      PropertyDescriptor idPropertyDescriptor = null;
      for (final PropertyDescriptor candidate : sampleBeanInfo.getPropertyDescriptors()) {
         if (candidate.getName().equals(fieldName)) {
            idPropertyDescriptor = candidate;
            break;
         }
      }
      assert idPropertyDescriptor != null;
      final InfinispanPersistentEntity<Sample> owner = new InfinispanPersistentEntity<Sample>(
            ClassTypeInformation.from(Sample.class));

      final InfinispanPersistentProperty objectUnderTest = new InfinispanPersistentProperty(
            Sample.class.getDeclaredField(fieldName), idPropertyDescriptor, owner, new SimpleTypeHolder());

      assert objectUnderTest.isTransient() : "isTransient() should return true since this property is annotated with @Transient";
   }

   private static class Sample {

      @Id
      private Integer id;

      private boolean notAnnotatedProperty;

      @Transient
      private int transientAnnotatedProperty;

      @Field
      private String fieldAnnotatedProperty;

      @SuppressWarnings("unused")
      public Integer getId() {
         return id;
      }

      @SuppressWarnings("unused")
      public void setId(final Integer id) {
         this.id = id;
      }

      @SuppressWarnings("unused")
      public boolean isNotAnnotatedProperty() {
         return notAnnotatedProperty;
      }

      @SuppressWarnings("unused")
      public void setNotAnnotatedProperty(boolean notAnnotatedProperty) {
         this.notAnnotatedProperty = notAnnotatedProperty;
      }

      @SuppressWarnings("unused")
      public int getTransientAnnotatedProperty() {
         return transientAnnotatedProperty;
      }

      @SuppressWarnings("unused")
      public void setTransientAnnotatedProperty(int transientAnnotatedProperty) {
         this.transientAnnotatedProperty = transientAnnotatedProperty;
      }

      @SuppressWarnings("unused")
      public String getFieldAnnotatedProperty() {
         return fieldAnnotatedProperty;
      }

      @SuppressWarnings("unused")
      public void setFieldAnnotatedProperty(String fieldAnnotatedProperty) {
         this.fieldAnnotatedProperty = fieldAnnotatedProperty;
      }
   }
}
