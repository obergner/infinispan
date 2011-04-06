package org.infinispan.largeobjectsupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.DistributionManagerImpl;
import org.infinispan.distribution.TestAddress;
import org.infinispan.distribution.ch.NodeTopologyInfo;
import org.infinispan.distribution.ch.TopologyInfo;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.testng.annotations.Test;

/**
 * Test {@link Chunks}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "largeobjectsupport.ChunksTest")
public class ChunksTest {

   private static final int NUM_NODES_IN_CLUSTER = 20;

   private static final Address[] NODE_ADDRESSES;

   static {
      List<Address> nodeAddresses = new ArrayList<Address>(20);
      for (int i = 0; i < NUM_NODES_IN_CLUSTER; i++)
         nodeAddresses.add(new TestAddress(i));
      NODE_ADDRESSES = nodeAddresses.toArray(new Address[NUM_NODES_IN_CLUSTER]);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void testThatChunksConstructorRejectsInputStreamNotSupportingMark() {
      InputStream inputStreamNotSupportingMark = new InputStream() {
         @Override
         public int read() throws IOException {
            return -1;
         }

         @Override
         public boolean markSupported() {
            return false;
         }
      };
      new Chunks<Object>(new Object(), inputStreamNotSupportingMark, null, null, null);
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceCannotBeIteratedOverMoreThanOnce() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(NUM_NODES_IN_CLUSTER),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, 2L), newEmbeddedCacheManager());

      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }
      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }
   }

   @Test
   public void testThatChunksInstanceCorrectlyIteratesOverLargeObject() {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      long maxChunkSizeInBytes = 3L;
      InputStream largeObject = new ByteArrayInputStream(bytes);
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(1000),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, maxChunkSizeInBytes),
               newEmbeddedCacheManager());
      List<Chunk> allChunks = new ArrayList<Chunk>();

      for (Chunk chunk : objectUnderTest) {
         allChunks.add(chunk);
      }

      assert Arrays.equals(allChunks.get(0).getData(), new byte[] { 1, 2, 3 });
      assert Arrays.equals(allChunks.get(1).getData(), new byte[] { 4, 5, 6 });
      assert Arrays.equals(allChunks.get(2).getData(), new byte[] { 7 });
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatLargeObjectMetadataCannotBeCalledIfIterationNotYetFinished() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(NUM_NODES_IN_CLUSTER),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, 2L), newEmbeddedCacheManager());
      objectUnderTest.iterator().next(); // Should hold more than one chunk

      objectUnderTest.largeObjectMetadata();
   }

   @Test
   public void testThatChunksInstanceProducesCorrectLargeObjectMetadataAfterIteration() {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      long maxChunkSizeInBytes = 3L;
      InputStream largeObject = new ByteArrayInputStream(bytes);
      Object largeObjectKey = new Object();
      Chunks<Object> objectUnderTest = new Chunks<Object>(largeObjectKey, largeObject,
               newDistributionManagerWithNumNodesInCluster(NUM_NODES_IN_CLUSTER),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, maxChunkSizeInBytes),
               newEmbeddedCacheManager());
      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }

      LargeObjectMetadata<Object> largeObjectMetadata = objectUnderTest.largeObjectMetadata();

      assert largeObjectMetadata.getLargeObjectKey() == largeObjectKey : "Unexpected largeObjectKey in LargeObjectMetadat returned";
      assert largeObjectMetadata.getTotalSizeInBytes() == bytes.length : "Unexpected totalSizeInBytes in LargeObjectMetadat returned";
      assert largeObjectMetadata.getChunkKeys().length == 3 : "Unexpected number of chunk keys in LargeObjectMetadata returned: was "
               + largeObjectMetadata.getChunkKeys().length + " - should have been 3";
   }

   @Test(expectedExceptions = UnsupportedOperationException.class)
   public void testThatChunkIteratorDoesNotSupportRemove() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(NUM_NODES_IN_CLUSTER),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, 2L), newEmbeddedCacheManager());

      objectUnderTest.iterator().remove();
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceRejectsNumberOfClusterNodesZero() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(0),
               newConfigurationWithNumOwnersAndMaxChunkSize(1, 2L), newEmbeddedCacheManager());

      objectUnderTest.iterator().next();
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceRejectsNumOwnersZero() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(NUM_NODES_IN_CLUSTER),
               newConfigurationWithNumOwnersAndMaxChunkSize(0, 2L), newEmbeddedCacheManager());

      objectUnderTest.iterator().next();
   }

   @Test(expectedExceptions = LargeObjectExceedsSizeLimitException.class)
   public void testThatIterationFailsAsSoonAsLargeObjectSizeLimitIsExceeded() {
      long maximumChunkSizeInBytes = 8;
      int numberOfClusterNodes = 2;
      int numOwners = 4;
      long largeObjectSizeLimitInBytes = (maximumChunkSizeInBytes * numberOfClusterNodes)
               / numOwners;
      byte[] largeObjectData = new byte[(int) largeObjectSizeLimitInBytes + 1];
      Arrays.fill(largeObjectData, (byte) 16);
      InputStream largeObject = new ByteArrayInputStream(largeObjectData);
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               newDistributionManagerWithNumNodesInCluster(numberOfClusterNodes),
               newConfigurationWithNumOwnersAndMaxChunkSize(numOwners, 2L),
               newEmbeddedCacheManager());

      for (Chunk chunk : objectUnderTest)
         chunk.getChunkKey(); // Whatever
   }

   private DistributionManager newDistributionManagerWithNumNodesInCluster(int numNodesInCluster) {
      TopologyInfo ti = new TopologyInfo();

      for (int i = 0; i < numNodesInCluster; i++) {
         Address nodeAddress = new Address() {
         };
         NodeTopologyInfo nti = new NodeTopologyInfo(String.valueOf(i), String.valueOf(i),
                  String.valueOf(i), nodeAddress);
         ti.addNodeTopologyInfo(nodeAddress, nti);
      }

      DistributionManager distributionManager = new DistributionManagerImpl() {

         private Random randomGenerator = new Random();

         @Override
         public List<Address> locate(Object key) {
            int clusterNodeIndex = randomGenerator.nextInt(NUM_NODES_IN_CLUSTER);
            return Collections.singletonList(NODE_ADDRESSES[clusterNodeIndex]);
         }

      };
      distributionManager.setTopologyInfo(ti);

      return distributionManager;
   }

   private Configuration newConfigurationWithNumOwnersAndMaxChunkSize(final int numOwners,
            final long maxChunkSize) {
      return new Configuration() {
         @Override
         public int getNumOwners() {
            return numOwners;
         }

         @Override
         public long getMaximumChunkSizeInBytes() {
            return maxChunkSize;
         }
      };
   }

   private EmbeddedCacheManager newEmbeddedCacheManager() {
      return new DefaultCacheManager() {

         @Override
         public <K, V> Cache<K, V> getCache(String cacheName, boolean create) {
            // It is rather improbable that a given cache already contains
            // a randomly generated key.
            boolean containsKey = Math.random() < 1 / 100000;
            return newCacheMaybeContainingKey(containsKey);
         }

         @Override
         public Set<String> getCacheNames() {
            int numberOfCaches = 100;
            Set<String> cacheNames = new HashSet<String>(numberOfCaches);
            for (int i = 0; i < numberOfCaches; i++) {
               cacheNames.add("test.cache-" + i);
            }
            return cacheNames;
         }

      };
   }

   private <K, V> Cache<K, V> newCacheMaybeContainingKey(boolean containsKey) {
      return new MockCache<K, V>(containsKey);
   }

   private static class MockCache<K, V> implements Cache<K, V> {

      private final boolean containsKey;

      /**
       * Create a new MockCache.
       * 
       * @param containsKey
       */
      MockCache(boolean containsKey) {
         this.containsKey = containsKey;
      }

      @Override
      public V putIfAbsent(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public boolean remove(Object key, Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI remove");
      }

      @Override
      public V replace(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public void clear() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI clear");
      }

      @Override
      public boolean containsKey(Object key) {
         return containsKey;
      }

      @Override
      public boolean containsValue(Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI containsValue");
      }

      @Override
      public V get(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI get");
      }

      @Override
      public boolean isEmpty() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI isEmpty");
      }

      @Override
      public V put(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V remove(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI remove");
      }

      @Override
      public int size() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI size");
      }

      @Override
      public void start() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI start");
      }

      @Override
      public void stop() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI stop");
      }

      @Override
      public void addListener(Object listener) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI addListener");
      }

      @Override
      public void removeListener(Object listener) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeListener");
      }

      @Override
      public Set<Object> getListeners() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getListeners");
      }

      @Override
      public void putForExternalRead(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putForExternalRead");
      }

      @Override
      public void evict(K key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI evict");
      }

      @Override
      public Configuration getConfiguration() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getConfiguration");
      }

      @Override
      public boolean startBatch() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI startBatch");
      }

      @Override
      public void endBatch(boolean successful) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI endBatch");
      }

      @Override
      public String getName() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getName");
      }

      @Override
      public String getVersion() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getVersion");
      }

      @Override
      public EmbeddedCacheManager getCacheManager() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getCacheManager");
      }

      @Override
      public V put(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V replace(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
               TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
               TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit,
               long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
               TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public NotifyingFuture<V> putAsync(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> clearAsync() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI clearAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<V> removeAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<V> replaceAsync(K key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> getAsync(K key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getAsync");
      }

      @Override
      public AdvancedCache<K, V> getAdvancedCache() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getAdvancedCache");
      }

      @Override
      public void compact() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI compact");
      }

      @Override
      public ComponentStatus getStatus() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getStatus");
      }

      @Override
      public Set<K> keySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI keySet");
      }

      @Override
      public Collection<V> values() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI values");
      }

      @Override
      public Set<java.util.Map.Entry<K, V>> entrySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI entrySet");
      }

   }
}
