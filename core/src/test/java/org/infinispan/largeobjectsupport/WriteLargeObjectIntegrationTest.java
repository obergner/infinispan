package org.infinispan.largeobjectsupport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
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
      Configuration largeObjectCacheConfig = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      largeObjectCacheConfig.fluent().locking().useLockStriping(false).clustering()
               .stateRetrieval().timeout(1000L);
      createClusteredCaches(2, TEST_CACHE_NAME, largeObjectCacheConfig);
   }

   @Test
   public void testThatWriteToKeyCorrectlyWritesLargeObjectMetadata() {
      String largeObjectKey = "testThatWriteToKeyCorrectlyWritesLargeObjectMetadata";
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
      String largeObjectKey = "testThatWriteToKeyCorrectlyStoresChunks";
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

   @Test
   public void testThatWriteToKeyCorrectlyReplicatesChunksInSyncReplicationMode() {
      String largeObjectKey = "testThatWriteToKeyCorrectlyReplicatesChunksInSyncReplicationMode";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      Cache<Object, Object> cache2 = cache(1, TEST_CACHE_NAME);

      replListener(cache2).expect(PutKeyValueCommand.class);
      cache1.getAdvancedCache().writeToKey(largeObjectKey, largeObject);
      replListener(cache2).waitForRpc();

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(
               largeObjectKey);
      Object chunkKey = writtenMetadata.getChunkMetadata()[0].getKey();

      byte[] chunk = (byte[]) cache2.get(chunkKey);

      assert chunk != null : "writeToKey(" + largeObjectKey + ", " + largeObject
               + ") did not replicate chunks";
   }

   private Cache<Object, LargeObjectMetadata> defaultLargeObjectMetadataCache() {
      return manager(0).getCache(
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
   }
}
