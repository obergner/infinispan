package org.infinispan.largeobjectsupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyLargeObjectCommand;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

/**
 * LargeObjectSupportIntegrationTest.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 * 
 *        FIXME This does not work in replicated mode. Find out why and fix the problem.
 */
@Test(groups = "functional", testName = "LargeObjectSupportIntegrationTest")
public class LargeObjectSupportIntegrationTest extends MultipleCacheManagersTest {

   private static final String TEST_CACHE_NAME = "largeObjectSupportingCache";

   @Override
   protected void createCacheManagers() throws Throwable {
      Configuration clusteredConfig = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      clusteredConfig.fluent().locking().useLockStriping(false).clustering().stateRetrieval()
               .timeout(1000L);
      createCluster(clusteredConfig, false, 2);

      List<Cache<?, ?>> largeObjectCaches = new ArrayList<Cache<?, ?>>(2);
      manager(0).defineConfiguration(TEST_CACHE_NAME, clusteredConfig);
      largeObjectCaches.add(manager(0).getCache(TEST_CACHE_NAME));
      manager(1).defineConfiguration(TEST_CACHE_NAME, clusteredConfig);
      largeObjectCaches.add(manager(1).getCache(TEST_CACHE_NAME));
      TestingUtil.blockUntilViewsReceived(30000, largeObjectCaches);

      List<Cache<?, ?>> metadataCaches = new ArrayList<Cache<?, ?>>(2);
      manager(0).defineConfiguration(
               Configuration.LargeObjectSupportType.DEFAULT_LARGEOBJECT_METADATA_CACHE,
               clusteredConfig);
      metadataCaches.add(manager(0).getCache(
               Configuration.LargeObjectSupportType.DEFAULT_LARGEOBJECT_METADATA_CACHE));
      manager(1).defineConfiguration(
               Configuration.LargeObjectSupportType.DEFAULT_LARGEOBJECT_METADATA_CACHE,
               clusteredConfig);
      metadataCaches.add(manager(1).getCache(
               Configuration.LargeObjectSupportType.DEFAULT_LARGEOBJECT_METADATA_CACHE));
      TestingUtil.blockUntilViewsReceived(30000, metadataCaches);
   }

   @Test
   public void testThatWriteToKeyCorrectlyWritesLargeObjectMetadata() {
      String largeObjectKey = "testThatWriteToKeyCorrectlyWritesLargeObjectMetadata";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      cache1.getStreamingHandler().writeToKey(largeObjectKey, largeObject);

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(largeObjectKey);
      assert writtenMetadata != null : "writeToKey(" + largeObjectKey + ", " + largeObject
               + ") did not store key metadata";
      assert writtenMetadata.getLargeObjectKey() == largeObjectKey : "writeToKey(" + largeObjectKey
               + ", " + largeObject + ") wrote wrong large object metadata [" + writtenMetadata
               + "] - wrong large object key";
      assert writtenMetadata.getTotalSizeInBytes() == bytes.length : "writeToKey(" + largeObjectKey
               + ", " + largeObject + ") wrote wrong large object metadata [" + writtenMetadata
               + "] - wrong total size in bytes";
      assert writtenMetadata.getChunkMetadata().length == 1 : "writeToKey(" + largeObjectKey + ", "
               + largeObject + ") wrote wrong large object metadata [" + writtenMetadata
               + "] - wrong number of chunk keys";
   }

   @Test
   public void testThatWriteToKeyCorrectlyStoresChunks() {
      String largeObjectKey = "testThatWriteToKeyCorrectlyStoresChunks";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      cache1.getStreamingHandler().writeToKey(largeObjectKey, largeObject);

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(largeObjectKey);
      ChunkMetadata chunkMetadata = writtenMetadata.getChunkMetadata()[0];
      byte[] chunk = (byte[]) cache1.get(chunkMetadata.getKey());

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

      replListener(cache2).expect(PutKeyLargeObjectCommand.class);
      cache1.getStreamingHandler().writeToKey(largeObjectKey, largeObject);
      replListener(cache2).waitForRpc();

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(largeObjectKey);
      ChunkMetadata chunkMetadata = writtenMetadata.getChunkMetadata()[0];

      byte[] chunk = (byte[]) cache2.get(chunkMetadata.getKey());

      assert chunk != null : "writeToKey(" + largeObjectKey + ", " + largeObject
               + ") did not replicate chunks";
   }

   @Test
   public void testThatReadFromKeyReturnsLargeObjectStoredThroughWriteToKey() throws IOException {
      String largeObjectKey = "testThatReadFromKeyReturnsLargeObjectStoredThroughWriteToKey";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      cache1.getStreamingHandler().writeToKey(largeObjectKey, largeObject);

      InputStream largeObjectInputStream = cache1.getStreamingHandler().readFromKey(largeObjectKey);

      assert largeObjectInputStream != null : "readFromKey(" + largeObjectKey
               + ") returned null for a key just written";

      byte[] readLargeObject = readLargeObjectFrom(largeObjectInputStream);
      assert Arrays.equals(bytes, readLargeObject) : "The large object written [" + bytes
               + "] differs from the large object read [" + readLargeObject + "]";
   }

   @Test
   public void testThatLargeObjectMayBeReadFromReplicatedCache() throws IOException {
      String largeObjectKey = "testThatLargeObjectMayBeReadFromReplicatedCache";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);

      Cache<Object, Object> largeObjectCache1 = cache(0, TEST_CACHE_NAME);
      Cache<Object, Object> largeObjectCache2 = cache(1, TEST_CACHE_NAME);

      replListener(largeObjectCache2).expect(PutKeyLargeObjectCommand.class);
      largeObjectCache1.getStreamingHandler().writeToKey(largeObjectKey, largeObject);
      replListener(largeObjectCache2).waitForRpc();

      InputStream largeObjectInputStream = largeObjectCache2.getStreamingHandler().readFromKey(
               largeObjectKey);
      byte[] readLargeObject = readLargeObjectFrom(largeObjectInputStream);
      largeObjectInputStream.close();

      assert largeObjectInputStream != null : "readFromKey("
               + largeObjectKey
               + ") returned null for a key just written to its companion cache configured in synchronous replication mode";

      assert Arrays.equals(bytes, readLargeObject) : "The large object written [" + bytes
               + "] differs from the large object read [" + readLargeObject + "]";
   }

   @Test
   public void testThatWriteToKeyUsingOutputStreamCorrectlyStoresChunks() throws IOException {
      String largeObjectKey = "testThatWriteToKeyUsingOutputStreamCorrectlyStoresChunks";
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };

      Cache<Object, Object> cache1 = cache(0, TEST_CACHE_NAME);
      OutputStream largeObjectOutputStream = cache1.getStreamingHandler().writeToKey(largeObjectKey);
      for (byte b : bytes)
         largeObjectOutputStream.write(b);
      largeObjectOutputStream.close();

      LargeObjectMetadata writtenMetadata = defaultLargeObjectMetadataCache().get(largeObjectKey);
      ChunkMetadata chunkMetadata = writtenMetadata.getChunkMetadata()[0];
      byte[] chunk = (byte[]) cache1.get(chunkMetadata.getKey());

      assert chunk != null : "writeToKey(" + largeObjectKey + ") did not store chunk";
   }

   private Cache<Object, LargeObjectMetadata> defaultLargeObjectMetadataCache() {
      return manager(0).getCache(
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
   }

   private byte[] readLargeObjectFrom(InputStream inputStream) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int currentByte = -1;
      while ((currentByte = inputStream.read()) != -1) {
         buffer.write(currentByte);
      }

      return buffer.toByteArray();
   }
}
