package org.infinispan.largeobjectsupport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectMetadataManagerImplTest}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "largeobjectsupport.LargeObjectMetadataManagerImplTest")
public class LargeObjectMetadataManagerImplTest {

   @Test
   public void testThatAlreadyUsedByLargeObjectRecognizesThatAKeyIsAlreadyUsed() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata<Object> largeObjectMetadata = new LargeObjectMetadata<Object>(
               largeObjectKey, 3L, new String[0]);
      ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata<Object>>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               keyToLargeObjectMetadata);

      assert objectUnderTest.alreadyUsedByLargeObject(largeObjectKey) : "LargeObjectMetadataManagerImpl failed "
               + "to recognize that a key is already used";
   }

   @Test
   public void testThatCorrespondingLargeObjectMetadataReturnsCorrectMetadata() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata<Object> largeObjectMetadata = new LargeObjectMetadata<Object>(
               largeObjectKey, 3L, new String[0]);
      ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata<Object>>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               keyToLargeObjectMetadata);

      assert objectUnderTest.correspondingLargeObjectMetadata(largeObjectKey) == largeObjectMetadata : "LargeObjectMetadataManagerImpl failed "
               + "to return correct metadata";
   }

   @Test
   public void testThatStoreLargeObjectMetadataCorrectlyStoresMetadata() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata<Object> largeObjectMetadata = new LargeObjectMetadata<Object>(
               largeObjectKey, 3L, new String[0]);
      ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata<Object>>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               keyToLargeObjectMetadata);
      objectUnderTest.storeLargeObjectMetadata(largeObjectMetadata);

      assert objectUnderTest.correspondingLargeObjectMetadata(largeObjectKey) == largeObjectMetadata : "LargeObjectMetadataManagerImpl failed "
               + "to correctly store metadata";
   }
}
