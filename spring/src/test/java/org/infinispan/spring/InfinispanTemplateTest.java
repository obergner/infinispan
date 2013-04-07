package org.infinispan.spring;

import static org.infinispan.test.TestingUtil.withCacheManager;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.api.BasicCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.spring.data.repository.support.test.IndexedDomainObjectWithDocumentIdProperty;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.springframework.dao.DataAccessException;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.InfinispanTemplateTest")
public class InfinispanTemplateTest {

   @Test(dataProvider = "exceptionMappings", dataProviderClass = DataProviders.class)
   public void executeShouldProperlyTranslateCacheExceptionsIntoDataAccessExceptions(final CacheException input,
         final Class<? extends DataAccessException> expectedOutputType) throws Exception {
      withCacheManager(new CacheManagerCallable(TestCacheManagerFactory.createCacheManager()) {
         @Override
         public void call() {
            try {
               final InfinispanTemplate objectUnderTest = newInfinispanTemplate(cm.getCache());

               objectUnderTest.execute(new InfinispanCallback<Void>() {
                  @Override
                  public Void doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
                     throw input;
                  }
               });
            } catch (final DataAccessException e) {
               assert expectedOutputType == e.getClass() : "Expected " + input + " to be translated into "
                     + expectedOutputType + ", but got: " + e.getClass();
            }
         }
      });
   }

   private InfinispanTemplate newInfinispanTemplate(final BasicCache<?, ?> cache) {
      try {
         final InfinispanTemplate objectUnderTest = new InfinispanTemplate();
         objectUnderTest.setBasicCache(cache);
         objectUnderTest.afterPropertiesSet();
         return objectUnderTest;
      } catch (final Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Test
   public void queryShouldFindIndexedDomainObject() throws Exception {
      withCacheManager(new CacheManagerCallable(createHibernateSearchEnabledCacheManager()) {
         @Override
         public void call() {
            final Cache<Object, Object> cache = cm.getCache();

            final int id = 1;
            final String name = "queryShouldFindIndexedDomainObject";
            final IndexedDomainObjectWithDocumentIdProperty indexedDomainObject = new IndexedDomainObjectWithDocumentIdProperty(
                  id, name);
            cache.put(id, indexedDomainObject);

            final InfinispanTemplate objectUnderTest = newInfinispanTemplate(cache);

            final List<IndexedDomainObjectWithDocumentIdProperty> matches = objectUnderTest.query(
                  new InfinispanQueryCallback<IndexedDomainObjectWithDocumentIdProperty>() {
                     @Override
                     public Query doWithQueryBuilder(final QueryBuilder queryBuilder) {
                        return queryBuilder.keyword().onField("name").matching(name).createQuery();
                     }
                  }, IndexedDomainObjectWithDocumentIdProperty.class);

            assert matches.size() == 1 : "Expected exactly one match to be returned. Got: " + matches.size();
            final IndexedDomainObjectWithDocumentIdProperty match = matches.get(0);
            assert indexedDomainObject.equals(match) : "Query should have returned " + indexedDomainObject
                  + ". Instead, it returned " + match;
         }
      });
   }

   private EmbeddedCacheManager createHibernateSearchEnabledCacheManager() {
      final ConfigurationBuilder cfg = TestCacheManagerFactory.getDefaultCacheConfiguration(true);
      cfg.indexing().enable().indexLocalOnly(false).addProperty("default.directory_provider", "ram")
            .addProperty("lucene_version", "LUCENE_CURRENT");
      return TestCacheManagerFactory.createCacheManager(cfg);
   }
}
