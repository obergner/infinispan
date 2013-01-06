package org.infinispan.spring;

import org.infinispan.CacheException;
import org.infinispan.api.BasicCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.springframework.dao.DataAccessException;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.InfinispanPersistenceExceptionTranslatorTest")
public class InfinispanTemplateTest {

   private final EmbeddedCacheManager cacheManager = TestCacheManagerFactory.createCacheManager();

   @Test(dataProvider = "exceptionMappings", dataProviderClass = DataProviders.class)
   public void executeShouldProperlyTranslateCacheExceptionsIntoDataAccessExceptions(final CacheException input,
         final Class<? extends DataAccessException> expectedOutputType) throws Exception {
      try {
         final InfinispanTemplate objectUnderTest = newInfinispanTemplate();

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

   private InfinispanTemplate newInfinispanTemplate() throws Exception {
      final BasicCache<?, ?> cache = cacheManager.getCache();

      final InfinispanTemplate objectUnderTest = new InfinispanTemplate();
      objectUnderTest.setBasicCache(cache);
      objectUnderTest.afterPropertiesSet();
      return objectUnderTest;
   }
}
