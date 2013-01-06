package org.infinispan.spring;

import org.infinispan.CacheException;
import org.springframework.dao.DataAccessException;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "spring.InfinispanPersistenceExceptionTranslatorTest")
public class InfinispanPersistenceExceptionTranslatorTest {

   private final InfinispanPersistenceExceptionTranslator objectUnderTest = new InfinispanPersistenceExceptionTranslator();

   @Test(dataProvider = "exceptionMappings", dataProviderClass = DataProviders.class)
   public void testTranslateExceptionIfPossible(CacheException input,
         Class<? extends DataAccessException> expectedOutputType) {
      final DataAccessException mappedException = this.objectUnderTest.translateExceptionIfPossible(input);

      assert expectedOutputType == mappedException.getClass() : "Expected " + input + " to be translated into "
            + expectedOutputType + ", but got: " + mappedException.getClass();
   }
}
