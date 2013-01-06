package org.infinispan.spring.data.repository.mapping;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.data.repository.mapping.InfinispanMappingContextTest")
public class InfinispanMappingContextTest {

   private final InfinispanMappingContext objectUnderTest = new InfinispanMappingContext();

   @Test
   public void createPersistentEntityShouldReturnNonNullPersistentEntity() {
      final InfinispanPersistentEntity<?> persistentEntity = objectUnderTest
            .createPersistentEntity(ClassTypeInformation.from(Sample.class));

      assert persistentEntity != null : "createPersistentEntity(...) returned null for " + Sample.class.getName();
   }

   @Test
   public void createPersistentPropertyShouldReturnNonNullPersistentProperty() throws Exception {
      final BeanInfo sampleBeanInfo = Introspector.getBeanInfo(Sample.class);
      PropertyDescriptor idPropertyDescriptor = null;
      for (final PropertyDescriptor candidate : sampleBeanInfo.getPropertyDescriptors()) {
         if (candidate.getName().equals("id")) {
            idPropertyDescriptor = candidate;
            break;
         }
      }
      assert idPropertyDescriptor != null;
      final InfinispanPersistentEntity<?> owner = objectUnderTest.createPersistentEntity(ClassTypeInformation
            .from(Sample.class));
      final InfinispanPersistentProperty persistentProperty = objectUnderTest.createPersistentProperty(
            Sample.class.getDeclaredField("id"), idPropertyDescriptor, owner, new SimpleTypeHolder());

      assert persistentProperty != null : "createPersistentProperty(...) returned null for property \"id\" in class "
            + Sample.class.getName();
   }

   private static class Sample {

      @Id
      private Integer id;

      private int intProperty;

      @SuppressWarnings("unused")
      public Integer getId() {
         return id;
      }

      @SuppressWarnings("unused")
      public void setId(final Integer id) {
         this.id = id;
      }

      @SuppressWarnings("unused")
      public int getIntProperty() {
         return intProperty;
      }

      @SuppressWarnings("unused")
      public void setIntProperty(final int intProperty) {
         this.intProperty = intProperty;
      }
   }
}
