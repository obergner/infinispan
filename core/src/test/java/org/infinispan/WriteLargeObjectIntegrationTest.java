package org.infinispan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

/**
 * WriteLargeObjectIntegrationTest.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 * 
 *        FIXME This does not work in replicated mode. Find out why and fix the problem.
 */
@Test(groups = "functional", testName = "WriteLargeObjectIntegrationTest")
public class WriteLargeObjectIntegrationTest extends MultipleCacheManagersTest {

   private static final String TEST_CACHE_NAME = "largeObjectSupportingCache";

   @Override
   protected void createCacheManagers() throws Throwable {
      Configuration config1 = getDefaultClusteredConfig(Configuration.CacheMode.DIST_SYNC);
      config1.setGlobalConfiguration(new GlobalConfiguration());

      config1.fluent().locking().useLockStriping(false);

      EmbeddedCacheManager cacheManager1 = addClusterEnabledCacheManager(config1, false);
      cacheManager1.getGlobalConfiguration().fluent().transport().machineId("machine1")
               .clusterName("testCluster");

      Configuration config2 = config1.clone();

      EmbeddedCacheManager cacheManager2 = addClusterEnabledCacheManager(config2, false);
      config2.getGlobalConfiguration().fluent().transport().machineId("machine2")
               .clusterName("testCluster");

      cacheManager1.defineConfiguration(TEST_CACHE_NAME, config1);
      cacheManager2.defineConfiguration(TEST_CACHE_NAME, config2);
   }

   @Test
   public void testThatWriteToKeyCorrectlyWritesLargeObjectMetadata() {
      Object largeObjectKey = new Object();
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      cache1.getAdvancedCache().writeToKey(largeObjectKey, largeObject);

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(
               largeObjectKey);
      assert writtenMetadata != null : "writeToKey(" + largeObjectKey + ", " + largeObject
               + ") did not store key metadata";
      assert writtenMetadata.getLargeObjectKey() == largeObjectKey : "writeToKey(" + largeObjectKey
               + ", " + largeObject + ") wrote wrong large object metadata [" + writtenMetadata
               + "] - wrong large object key";
      assert writtenMetadata.getTotalSizeInBytes() == bytes.length : "writeToKey(" + largeObjectKey
               + ", " + largeObject + ") wrote wrong large object metadata [" + writtenMetadata
               + "] - wrong total size in bytes";
   }

   @Test
   public void testThatWriteToKeyCorrectlyStoresChunks() {
      Object largeObjectKey = new Object();
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      cache1.getAdvancedCache().writeToKey(largeObjectKey, largeObject);

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(
               largeObjectKey);
      Object chunkKey = writtenMetadata.getChunkMetadata()[0].getKey();
      byte[] chunk = (byte[]) cache1.get(chunkKey);

      assert chunk != null : "writeToKey(" + largeObjectKey + ", " + largeObject
               + ") did not store chunk";
   }

   private Cache<Object, LargeObjectMetadata> defaultLargeObjectMetadataCache() {
      return manager(0).getCache(
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
   }
}
