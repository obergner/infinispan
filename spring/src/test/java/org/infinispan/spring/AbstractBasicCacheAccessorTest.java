package org.infinispan.spring;

import org.infinispan.CacheException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.testng.annotations.Test;

public class AbstractBasicCacheAccessorTest {

   @Test(expectedExceptions = IllegalStateException.class)
   public void afterPropertiesSetShouldComplainAboutMissingBasicCache() throws Exception {
      final AbstractBasicCacheAccessor objectUnderTest = new AbstractBasicCacheAccessor() {
      };
      objectUnderTest.afterPropertiesSet();
   }

   @Test(dataProvider = "exceptionMappings", dataProviderClass = DataProviders.class)
   public void convertInfinispanCacheExceptionShouldCorrectlyConvertCacheExceptions(CacheException input,
         Class<? extends DataAccessException> expectedOutputType) {
      final AbstractBasicCacheAccessor objectUnderTest = new AbstractBasicCacheAccessor() {
      };
      final DataAccessException mappedException = objectUnderTest.convertInfinispanCacheException(input);

      assert expectedOutputType == mappedException.getClass() : "Expected " + input + " to be translated into "
            + expectedOutputType + ", but got: " + mappedException.getClass();
   }

   @Test
   public void getPersistenceExceptionTranslatorShouldReturnDefaultTranslatorIfNoneHasBeenSet() {
      final AbstractBasicCacheAccessor objectUnderTest = new AbstractBasicCacheAccessor() {
      };
      final PersistenceExceptionTranslator defaultTranslator = objectUnderTest.getPersistenceExceptionTranslator();

      assert defaultTranslator != null : "getPersistenceTranslator() return null. This should NEVER happen.";
      assert defaultTranslator instanceof InfinispanPersistenceExceptionTranslator : "getPersistenceTranslator() should return an instance of "
            + InfinispanPersistenceExceptionTranslator.class.getName()
            + " if none has been set. Yet I got: "
            + defaultTranslator;
   }
}
