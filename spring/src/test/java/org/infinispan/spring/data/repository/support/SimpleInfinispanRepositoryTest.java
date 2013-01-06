package org.infinispan.spring.data.repository.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.api.BasicCache;
import org.infinispan.spring.InfinispanTemplate;
import org.infinispan.spring.data.repository.query.InfinispanEntityInformation;
import org.infinispan.spring.data.repository.support.test.DomainObjectWithIdAnnotation;
import org.infinispan.spring.data.repository.support.test.DomainObjectWithProvidedIdAnnotation;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.data.repository.support.SimpleInfinispanRepositoryTest")
public class SimpleInfinispanRepositoryTest {

   @BeforeMethod
   public void beforeMethod() {
   }

   @AfterMethod
   public void afterMethod() {
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void constructorShouldRejectNullInfinispanTemplate() {
      new SimpleInfinispanRepository<Object, String>(null, Object.class);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void constructorShouldRejectNullEntityInformation() {
      new SimpleInfinispanRepository<Object, String>(new InfinispanTemplate(),
            (InfinispanEntityInformation<Object, String>) null);
   }

   @Test
   public void countShouldReturn0WhenCacheIsEmpty() {
      final BasicCache<?, ?> cache = TestCacheManagerFactory.createCacheManager().getCache();
      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);

      final long cnt = objectUnderTest.count();
      assert cnt == 0 : "Cache is empty - count() should have returned 0, yet it returned " + cnt;
   }

   private <T, ID extends Serializable> SimpleInfinispanRepository<T, ID> newSimpleInfinispanRepostitory(
         final BasicCache<?, ?> cache, final Class<T> entityType, final Class<ID> idType) {
      final InfinispanTemplate template = new InfinispanTemplate();
      template.setBasicCache(cache);
      return new SimpleInfinispanRepository<T, ID>(template, entityType);
   }

   @Test
   public void countShouldReturnNumberOfItemsStoredInCache() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(
            "countShouldReturnNumberOfItemsStoredInCache");
      cache.put(domainObject.getId(), domainObject);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);

      final long cnt = objectUnderTest.count();
      assert cnt == cache.size() : "count() should have returned " + cache.size() + ", yet it returned " + cnt;
   }

   @Test
   public void deleteByIdShouldRemoveObjectHavingSpecifiedIdFromCache() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final String id = "deleteByIdShouldRemoveObjectHavingSpecifiedIdFromCache";
      final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
      cache.put(domainObject.getId(), domainObject);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.delete(id);

      assert !cache.containsKey(id) : "delete(" + id + ") should have removed " + domainObject
            + " from cache, yet it didn't";
   }

   @Test
   public void deleteByValueShouldRemoveSpecifiedObjectFromCache() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final String id = "deleteByValueShouldRemoveSpecifiedObjectFromCache";
      final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
      cache.put(domainObject.getId(), domainObject);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.delete(domainObject);

      assert !cache.containsKey(id) : "delete(" + domainObject + ") should have removed " + domainObject
            + " from cache, yet it didn't";
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void deleteByValueShouldComplainAboutEntityWithProvidedIdAnnotation() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final DomainObjectWithProvidedIdAnnotation domainObject = new DomainObjectWithProvidedIdAnnotation();

      final SimpleInfinispanRepository<DomainObjectWithProvidedIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithProvidedIdAnnotation.class, String.class);
      objectUnderTest.delete(domainObject);
   }

   @Test
   public void deleteIterableShouldRemoveAllSpecifiedObjectsFromCache() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      final String id1 = "deleteIterableShouldRemoveAllSpecifiedObjectsFromCache-1";
      final DomainObjectWithIdAnnotation domainObject1 = new DomainObjectWithIdAnnotation(id1);
      cache.put(domainObject1.getId(), domainObject1);

      final String id2 = "deleteIterableShouldRemoveAllSpecifiedObjectsFromCache-2";
      final DomainObjectWithIdAnnotation domainObject2 = new DomainObjectWithIdAnnotation(id2);
      cache.put(domainObject2.getId(), domainObject2);

      final DomainObjectWithIdAnnotation[] all = new DomainObjectWithIdAnnotation[] { domainObject1, domainObject2 };

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.delete(Arrays.asList(all));

      assert !cache.containsKey(id1) && !cache.containsKey(id2) : "delete(" + Arrays.asList(all)
            + ") should have removed all objects from cache, yet it didn't";

   }

   @Test
   public void deleteAllShouldClearCache() {
      final int storedObjectsCount = 20;
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      for (int i = 0; i < storedObjectsCount; i++) {
         final String id = "deleteAllShouldEmptyCache-" + i;
         final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
         cache.put(domainObject.getId(), domainObject);
      }

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.deleteAll();

      assert cache.isEmpty() : "deleteAll() should have removed cleared cache, yet it didn't";
   }

   @Test
   public void existsShouldReturnTrueForPreviouslyStoredEntity() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final String id = "existsShouldReturnTrueForPreviouslyStoredEntity";
      final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
      cache.put(domainObject.getId(), domainObject);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.exists(domainObject.getId());

      assert objectUnderTest.exists(domainObject.getId()) : "exists(" + domainObject.getId()
            + ") should have returned true since an object having that id has been stored";
   }

   @Test
   public void existsShouldReturnFalseForUnknownId() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final String id = "existsShouldReturnTrueForPreviouslyStoredEntity";
      final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
      cache.put(domainObject.getId(), domainObject);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.exists(domainObject.getId());

      assert !objectUnderTest.exists("UNKNOWN") : "exists(UNKNOWN) should have returned false since no object having that id has been stored";
   }

   @Test
   public void findAllShouldReturnAllStoredEntities() {
      final int storedObjectsCount = 20;
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      for (int i = 0; i < storedObjectsCount; i++) {
         final String id = "findAllShouldReturnAllStoredEntities-" + i;
         final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
         cache.put(domainObject.getId(), domainObject);
      }

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      final Iterable<DomainObjectWithIdAnnotation> all = objectUnderTest.findAll();

      int retrievedObjectsCount = 0;
      for (final DomainObjectWithIdAnnotation entity : all) {
         retrievedObjectsCount++;
      }

      assert storedObjectsCount == retrievedObjectsCount : "findAll() should have returned " + storedObjectsCount
            + " entities, yet it returned " + retrievedObjectsCount;
   }

   @Test
   public void findAllByIdsShouldReturnAllMatchingEntities() {
      final int storedObjectsCount = 20;
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      final List<String> idsToRetrieve = new ArrayList<String>();
      for (int i = 0; i < storedObjectsCount; i++) {
         final String id = "findAllByIdsShouldReturnAllMatchingEntities-" + i;
         if (i % 2 == 0) {
            idsToRetrieve.add(id);
         }
         final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
         cache.put(domainObject.getId(), domainObject);
      }

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      final Iterable<DomainObjectWithIdAnnotation> subset = objectUnderTest.findAll(idsToRetrieve);

      final Map<String, DomainObjectWithIdAnnotation> retrievedDomainObjects = new HashMap<String, DomainObjectWithIdAnnotation>();
      for (final DomainObjectWithIdAnnotation entity : subset) {
         retrievedDomainObjects.put(entity.getId(), entity);
      }

      assert idsToRetrieve.size() == retrievedDomainObjects.size() : "findAll(" + idsToRetrieve
            + ") should have returned " + idsToRetrieve.size() + " entities, yet it returned "
            + retrievedDomainObjects.size();
      for (final String id : idsToRetrieve) {
         assert retrievedDomainObjects.containsKey(id) : "findAll(" + idsToRetrieve
               + ") should have returned object having id " + id;
      }
   }

   @Test
   public void findOneShouldReturnEntityWithMatchingId() {
      final int storedObjectsCount = 20;
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      for (int i = 0; i < storedObjectsCount; i++) {
         final String id = "findOneShouldReturnEntityWithMatchingId-" + i;
         final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
         cache.put(domainObject.getId(), domainObject);
      }

      final String idToRetrieve = "findOneShouldReturnEntityWithMatchingId-" + 12;

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      final DomainObjectWithIdAnnotation match = objectUnderTest.findOne(idToRetrieve);

      assert match != null : "findOne(" + idToRetrieve
            + ") should have returned non-null match since the specified ID has been stored";
      assert idToRetrieve.equals(match.getId()) : "findOne(" + idToRetrieve
            + ") should have returned entity with matching ID";
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void saveShouldComplainAboutEntityWithProvidedIdAnnotation() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();

      final SimpleInfinispanRepository<DomainObjectWithProvidedIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithProvidedIdAnnotation.class, String.class);
      objectUnderTest.save(new DomainObjectWithProvidedIdAnnotation());
   }

   @Test
   public void saveShouldSaveEntityUsingIdAnnotatedFieldAsKey() {
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final DomainObjectWithIdAnnotation entity = new DomainObjectWithIdAnnotation(
            "saveShouldComplainAboutEntityWithoutIdAnnotation");

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.save(entity);

      assert cache.get(entity.getId()) == entity : "save(" + entity + ") should have saved " + entity + " under key "
            + entity.getId() + " in underlying cache";
   }

   @Test
   public void saveIterableShouldSaveAllEntities() {
      final int entitiesCount = 20;

      final Set<DomainObjectWithIdAnnotation> entities = new HashSet<DomainObjectWithIdAnnotation>(entitiesCount);
      for (int i = 0; i < entitiesCount; i++) {
         final String id = "findOneShouldReturnEntityWithMatchingId-" + i;
         final DomainObjectWithIdAnnotation domainObject = new DomainObjectWithIdAnnotation(id);
         entities.add(domainObject);
      }

      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.save(entities);

      assert cache.size() == entitiesCount : "save(" + entities + ") should have saved " + entitiesCount
            + " entities, yet it save " + cache.size() + " entities";
   }

   @Test
   public void saveByIdShouldSaveEntityWithProvidedIdAnnotationUsingProvidedKey() {
      final String key = "saveByIdShouldSaveEntityWithProvidedIdAnnotationUsingProvidedKey";
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final DomainObjectWithProvidedIdAnnotation entity = new DomainObjectWithProvidedIdAnnotation();

      final SimpleInfinispanRepository<DomainObjectWithProvidedIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithProvidedIdAnnotation.class, String.class);
      objectUnderTest.save(key, entity);

      assert cache.get(key) == entity : "save(" + key + ", " + entity + ") should have saved " + entity + " under key "
            + key + " in underlying cache";
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void saveByIdShouldShouldComplainAboutEntityWithIdAnnotatedField() {
      final String key = "saveByIdShouldShouldComplainAboutEntityWithIdAnnotatedField";
      final BasicCache cache = TestCacheManagerFactory.createCacheManager().getCache();
      final DomainObjectWithIdAnnotation entity = new DomainObjectWithIdAnnotation(key);

      final SimpleInfinispanRepository<DomainObjectWithIdAnnotation, String> objectUnderTest = newSimpleInfinispanRepostitory(
            cache, DomainObjectWithIdAnnotation.class, String.class);
      objectUnderTest.save(key, entity);
   }
}
