package org.infinispan.interceptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.config.Configuration;
import org.infinispan.container.EntryFactory;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.container.entries.ReadCommittedEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.context.impl.NonTxInvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.DistributionManagerImpl;
import org.infinispan.distribution.TestAddress;
import org.infinispan.distribution.ch.NodeTopologyInfo;
import org.infinispan.distribution.ch.TopologyInfo;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManager;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManagerImpl;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.BidirectionalMap;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.infinispan.util.concurrent.TimeoutException;
import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectSupportInterceptor}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "interceptors.LargeObjectSupportInterceptorTest")
public class LargeObjectSupportInterceptorTest {

   private static final int NUM_NODES_IN_CLUSTER = 20;

   private static final Address[] NODE_ADDRESSES;

   static {
      List<Address> nodeAddresses = new ArrayList<Address>(20);
      for (int i = 0; i < NUM_NODES_IN_CLUSTER; i++)
         nodeAddresses.add(new TestAddress(i));
      NODE_ADDRESSES = nodeAddresses.toArray(new Address[NUM_NODES_IN_CLUSTER]);
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatLargeObjectChunkingInterceptorRejectsTransactionalInvocationContext()
            throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      objectUnderTest.init(newConfigurationWithNumOwnersAndMaxChunkSize(1, 3L),
               newDistributionManagerWithNumNodesInCluster(1000), newEntryFactory(),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object()));

      LocalTxInvocationContext txCtx = new LocalTxInvocationContext() {
         @Override
         public void putLookedUpEntry(Object key, CacheEntry e) {
            // Ignore
         }
      };

      Object largeObjectKey = new Object();
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);
      CacheEntry cacheEntry = new ReadCommittedEntry(largeObjectKey, largeObject, 0L);
      txCtx.putLookedUpEntry(largeObjectKey, cacheEntry);

      PutKeyValueCommand writeLargeObjectCommand = new PutKeyValueCommand(largeObjectKey,
               largeObject, false, true, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyValueCommand(txCtx, writeLargeObjectCommand);
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatLargeObjectChunkingInterceptorRejectsNonInputStreamValue() throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      objectUnderTest.init(newConfigurationWithNumOwnersAndMaxChunkSize(1, 3L),
               newDistributionManagerWithNumNodesInCluster(1000), newEntryFactory(),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object()));

      LocalTxInvocationContext txCtx = new LocalTxInvocationContext() {
         @Override
         public void putLookedUpEntry(Object key, CacheEntry e) {
            // Ignore
         }
      };

      Object largeObjectKey = new Object();
      Object nonInputStreamLargeObject = new Object();
      CacheEntry cacheEntry = new ReadCommittedEntry(largeObjectKey, nonInputStreamLargeObject, 0L);
      txCtx.putLookedUpEntry(largeObjectKey, cacheEntry);

      PutKeyValueCommand writeLargeObjectCommand = new PutKeyValueCommand(largeObjectKey,
               nonInputStreamLargeObject, false, true, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyValueCommand(txCtx, writeLargeObjectCommand);
   }

   @Test
   public void testThatLargeObjectChunkingInterceptorCorrectlyCallsInterceptorPipelineForEachChunk()
            throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      objectUnderTest.init(newConfigurationWithNumOwnersAndMaxChunkSize(1, 3L),
               newDistributionManagerWithNumNodesInCluster(1000), newEntryFactory(),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object()));

      final List<byte[]> receivedChunkData = new ArrayList<byte[]>();
      CommandInterceptor recordingCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command)
                  throws Throwable {
            BidirectionalMap<Object, CacheEntry> entriesStoredInCtx = ctx.getLookedUpEntries();
            assert entriesStoredInCtx.size() > 0 : "LargeObjectSupportInterceptor did not store "
                     + "any entries in InvocationContext";
            assert entriesStoredInCtx.size() == 1 : "LargeObjectSupportInterceptor did stored more "
                     + "than one entry in InvocationContext";
            CacheEntry cacheEntry = entriesStoredInCtx.values().iterator().next();
            receivedChunkData.add((byte[]) cacheEntry.getValue());

            return null;
         }
      };
      objectUnderTest.setNext(recordingCommandInterceptor);

      Object largeObjectKey = new Object();
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);
      CacheEntry cacheEntry = new ReadCommittedEntry(largeObjectKey, largeObject, 0L);
      InvocationContext ctx = new NonTxInvocationContext();
      ctx.putLookedUpEntry(largeObjectKey, cacheEntry);

      PutKeyValueCommand writeLargeObjectCommand = new PutKeyValueCommand(largeObjectKey,
               largeObject, false, true, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyValueCommand(ctx, writeLargeObjectCommand);

      assert Arrays.equals(receivedChunkData.get(0), new byte[] { 1, 2, 3 });
      assert Arrays.equals(receivedChunkData.get(1), new byte[] { 4, 5, 6 });
      assert Arrays.equals(receivedChunkData.get(2), new byte[] { 7 });
   }

   @Test
   public void testThatLargeObjectChunkingInterceptorSkipsChunkingIfPutLargeObjectIsFalse()
            throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      objectUnderTest.init(newConfigurationWithNumOwnersAndMaxChunkSize(1, 3L),
               newDistributionManagerWithNumNodesInCluster(1000), newEntryFactory(),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object()));

      final List<Object> receivedValues = new ArrayList<Object>();
      CommandInterceptor recordingCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command)
                  throws Throwable {
            BidirectionalMap<Object, CacheEntry> entriesStoredInCtx = ctx.getLookedUpEntries();
            assert entriesStoredInCtx.size() > 0 : "LargeObjectSupportInterceptor did not store "
                     + "any entries in InvocationContext";
            assert entriesStoredInCtx.size() == 1 : "LargeObjectSupportInterceptor did stored more "
                     + "than one entry in InvocationContext";
            CacheEntry cacheEntry = entriesStoredInCtx.values().iterator().next();
            receivedValues.add(cacheEntry.getValue());

            return null;
         }
      };
      objectUnderTest.setNext(recordingCommandInterceptor);

      Object largeObjectKey = new Object();
      Object largeObject = new Object();
      CacheEntry cacheEntry = new ReadCommittedEntry(largeObjectKey, largeObject, 0L);
      InvocationContext ctx = new NonTxInvocationContext();
      ctx.putLookedUpEntry(largeObjectKey, cacheEntry);

      PutKeyValueCommand writeLargeObjectCommand = new PutKeyValueCommand(largeObjectKey,
               largeObject, false, false, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyValueCommand(ctx, writeLargeObjectCommand);

      assert receivedValues.get(0) == largeObject : "LargeObjectSupportInterceptor did NOT leave "
               + "the value to be stored unaltered although the putLargeObject flag "
               + "on the PutKeyValueCommand was set to false";
   }

   @Test
   public void testThatLargeObjectSupportInterceptorSkipsCorrectlyStoresLargeObjectMetadata()
            throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      LargeObjectMetadataManager largeObjectMetadataManager = newLargeObjectMetadataManagerWithLargeObjectMetadataStored(null);
      objectUnderTest.init(newConfigurationWithNumOwnersAndMaxChunkSize(1, 3L),
               newDistributionManagerWithNumNodesInCluster(1000), newEntryFactory(),
               largeObjectMetadataManager);

      CommandInterceptor noopCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command)
                  throws Throwable {
            return null;
         }
      };
      objectUnderTest.setNext(noopCommandInterceptor);

      Object largeObjectKey = new Object();
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      InputStream largeObject = new ByteArrayInputStream(bytes);
      CacheEntry cacheEntry = new ReadCommittedEntry(largeObjectKey, largeObject, 0L);
      InvocationContext ctx = new NonTxInvocationContext();
      ctx.putLookedUpEntry(largeObjectKey, cacheEntry);

      PutKeyValueCommand writeLargeObjectCommand = new PutKeyValueCommand(largeObjectKey,
               largeObject, false, true, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyValueCommand(ctx, writeLargeObjectCommand);

      assert largeObjectMetadataManager.alreadyUsedByLargeObject(largeObjectKey) : "LargeObjectSupportInterceptor did NOT store Large Object's metadata";
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

   private LargeObjectMetadataManager newLargeObjectMetadataManagerWithLargeObjectMetadataStored(
            Object largeObjectKey) {
      LargeObjectMetadata<Object> largeObjectMetadata = new LargeObjectMetadata<Object>(
               largeObjectKey, 3L, new String[0]);
      ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata<Object>>(
               1);
      if (largeObjectKey != null)
         keyToLargeObjectMetadata.put(largeObjectMetadata.getLargeObjectKey(), largeObjectMetadata);

      return new LargeObjectMetadataManagerImpl(keyToLargeObjectMetadata);
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

   private EntryFactory newEntryFactory() {
      return new MockEntryFactory();
   }

   private static class MockEntryFactory implements EntryFactory {

      @Override
      public void releaseLock(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI releaseLock");
      }

      @Override
      public boolean acquireLock(InvocationContext ctx, Object key) throws InterruptedException,
               TimeoutException {
         throw new org.jboss.util.NotImplementedException("FIXME NYI acquireLock");
      }

      @Override
      public MVCCEntry wrapEntryForWriting(InvocationContext ctx, Object key,
               boolean createIfAbsent, boolean forceLockIfAbsent, boolean alreadyLocked,
               boolean forRemoval, boolean undeleteIfNeeded) throws InterruptedException {
         return new ReadCommittedEntry(key, null, 0L);
      }

      @Override
      public MVCCEntry wrapEntryForWriting(InvocationContext ctx, InternalCacheEntry entry,
               boolean createIfAbsent, boolean forceLockIfAbsent, boolean alreadyLocked,
               boolean forRemoval, boolean undeleteIfNeeded) throws InterruptedException {
         throw new org.jboss.util.NotImplementedException("FIXME NYI wrapEntryForWriting");
      }

      @Override
      public CacheEntry wrapEntryForReading(InvocationContext ctx, Object key)
               throws InterruptedException {
         throw new org.jboss.util.NotImplementedException("FIXME NYI wrapEntryForReading");
      }

   }
}
