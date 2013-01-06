package org.infinispan.spring.data.repository.query;

import org.infinispan.spring.data.repository.query.test.DomainClassWithDifferentDocumentIdAnnotatedAccessors;
import org.infinispan.spring.data.repository.query.test.DomainClassWithDifferentIdAndDocumentIdAnnotatedFields;
import org.infinispan.spring.data.repository.query.test.DomainClassWithDocumentIdAnnotatedAccessor;
import org.infinispan.spring.data.repository.query.test.DomainClassWithDocumentIdAnnotatedField;
import org.infinispan.spring.data.repository.query.test.DomainClassWithDocumentIdAnnotatedMethodAndIdAnnotatedField;
import org.infinispan.spring.data.repository.query.test.DomainClassWithDocumentIdAnnotatedNonPropertyMethod;
import org.infinispan.spring.data.repository.query.test.DomainClassWithIdAnnotatedField;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.data.repository.query.IdAccessFactoryTest")
public class IdAccessFactoryTest {

   private final IdAccessFactory objectUnderTest = new IdAccessFactory();

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void ofShouldComplainAboutDomainClassHavingDifferentIdAndDocumentIdAnnotatedFields() {
      this.objectUnderTest.of(DomainClassWithDifferentIdAndDocumentIdAnnotatedFields.class);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void ofShouldComplainAboutDomainClassHavingTwoDifferentDocumentIdAnnotatedMethods() {
      this.objectUnderTest.of(DomainClassWithDifferentDocumentIdAnnotatedAccessors.class);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void ofShouldComplainAboutDomainClassHavingDocumentIdAnnotatedNonPropertyMethod() {
      this.objectUnderTest.of(DomainClassWithDocumentIdAnnotatedNonPropertyMethod.class);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void ofShouldComplainAboutDomainClassHavingIdAnnotatedFieldAndDocumentIdAnnotatedGetter() {
      this.objectUnderTest.of(DomainClassWithDocumentIdAnnotatedMethodAndIdAnnotatedField.class);
   }

   @Test
   public void ofShouldCorrectlyHandleDomainClassWithDocumentIdAnnotatedField() {
      final IdAccess<DomainClassWithDocumentIdAnnotatedField, String> idAccess = this.objectUnderTest
            .of(DomainClassWithDocumentIdAnnotatedField.class);

      assert idAccess != null : "of(...) wrongly returned null";

      final String expectedId = "ofShouldCorrectlyHandleDomainClassWithDocumentIdAnnotatedField";
      final DomainClassWithDocumentIdAnnotatedField sample = new DomainClassWithDocumentIdAnnotatedField();
      sample.setDocumentId(expectedId);
      final String actualId = idAccess.get(sample);
      assert expectedId.equals(actualId) : "idAccess.get(" + sample + ") returned unexpected id";

      final Class<?> expectedIdType = String.class;
      final Class<?> actualIdType = idAccess.getIdType();
      assert expectedIdType == actualIdType : "idAccess.getIdType() returned wrong ID type";
   }

   @Test
   public void ofShouldCorrectlyHandleDomainClassWithIdAnnotatedField() {
      final IdAccess<DomainClassWithIdAnnotatedField, String> idAccess = this.objectUnderTest
            .of(DomainClassWithIdAnnotatedField.class);

      assert idAccess != null : "of(...) wrongly returned null";

      final String expectedId = "ofShouldCorrectlyHandleDomainClassWithIdAnnotatedField";
      final DomainClassWithIdAnnotatedField sample = new DomainClassWithIdAnnotatedField();
      sample.setId(expectedId);
      final String actualId = idAccess.get(sample);
      assert expectedId.equals(actualId) : "idAccess.get(" + sample + ") returned unexpected id";

      final Class<?> expectedIdType = String.class;
      final Class<?> actualIdType = idAccess.getIdType();
      assert expectedIdType == actualIdType : "idAccess.getIdType() returned wrong ID type";
   }

   @Test
   public void ofShouldCorrectlyHandleDomainClassWithDocumentIdAnnotatedAccessor() {
      final IdAccess<DomainClassWithDocumentIdAnnotatedAccessor, String> idAccess = this.objectUnderTest
            .of(DomainClassWithDocumentIdAnnotatedAccessor.class);

      assert idAccess != null : "of(...) wrongly returned null";

      final String expectedId = "ofShouldCorrectlyHandleDomainClassWithDocumentIdAnnotatedAccessor";
      final DomainClassWithDocumentIdAnnotatedAccessor sample = new DomainClassWithDocumentIdAnnotatedAccessor();
      sample.setDocumentId(expectedId);
      final String actualId = idAccess.get(sample);
      assert expectedId.equals(actualId) : "idAccess.get(" + sample + ") returned unexpected id";

      final Class<?> expectedIdType = String.class;
      final Class<?> actualIdType = idAccess.getIdType();
      assert expectedIdType == actualIdType : "idAccess.getIdType() returned wrong ID type";
   }
}
