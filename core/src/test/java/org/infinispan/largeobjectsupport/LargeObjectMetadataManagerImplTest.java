package org.infinispan.largeobjectsupport;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.StreamingHandler;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectMetadataManagerImplTest}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "largeobjectsupport.LargeObjectMetadataManagerImplTest")
public class LargeObjectMetadataManagerImplTest {

   @Test
   public void testThatAlreadyUsedByLargeObjectRecognizesThatAKeyIsAlreadyUsed() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata largeObjectMetadata = LargeObjectMetadata.newBuilder()
               .withLargeObjectKey(largeObjectKey).withMaxChunkSizeInBytes(1000L)
               .addChunk(new Object(), 3L).build();
      ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(keyToLargeObjectMetadata),
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);

      assert objectUnderTest.alreadyUsedByLargeObject(largeObjectKey) : "LargeObjectMetadataManagerImpl failed "
               + "to recognize that a key is already used";
   }

   @Test
   public void testThatCorrespondingLargeObjectMetadataReturnsCorrectMetadata() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata largeObjectMetadata = LargeObjectMetadata.newBuilder()
               .withLargeObjectKey(largeObjectKey).withMaxChunkSizeInBytes(1000L)
               .addChunk(new Object(), 3L).build();
      ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(keyToLargeObjectMetadata),
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);

      assert objectUnderTest.correspondingLargeObjectMetadata(largeObjectKey) == largeObjectMetadata : "LargeObjectMetadataManagerImpl failed "
               + "to return correct metadata";
   }

   @Test
   public void testThatStoreLargeObjectMetadataCorrectlyStoresMetadata() {
      Object largeObjectKey = new Object();
      LargeObjectMetadata largeObjectMetadata = LargeObjectMetadata.newBuilder()
               .withLargeObjectKey(largeObjectKey).withMaxChunkSizeInBytes(1000L)
               .addChunk(new Object(), 3L).build();
      ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata>(
               1);
      keyToLargeObjectMetadata.put(largeObjectKey, largeObjectMetadata);

      LargeObjectMetadataManagerImpl objectUnderTest = new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(keyToLargeObjectMetadata),
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
      objectUnderTest.storeLargeObjectMetadata(largeObjectMetadata);

      assert objectUnderTest.correspondingLargeObjectMetadata(largeObjectKey) == largeObjectMetadata : "LargeObjectMetadataManagerImpl failed "
               + "to correctly store metadata";
   }

   private CacheContainer newCacheContainerWithLargeObjectMetadataCache(
            final ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata) {
      return new CacheContainer() {
         @Override
         public void start() {
         }

         @Override
         public void stop() {
         }

         @Override
         public <K, V> Cache<K, V> getCache() {
            throw new org.jboss.util.NotImplementedException("FIXME NYI getCache");
         }

         @Override
         public <K, V> Cache<K, V> getCache(String cacheName) {
            return (Cache<K, V>) new MapBackedCache(keyToLargeObjectMetadata);
         };
      };
   };

   private static class MapBackedCache implements Cache<Object, LargeObjectMetadata> {

      private final ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata;

      MapBackedCache(ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata) {
         this.keyToLargeObjectMetadata = keyToLargeObjectMetadata;
      }

      @Override
      public LargeObjectMetadata putIfAbsent(Object key, LargeObjectMetadata value) {
         return this.keyToLargeObjectMetadata.putIfAbsent(key, value);
      }

      @Override
      public boolean remove(Object key, Object value) {
         return this.keyToLargeObjectMetadata.remove(key, value);
      }

      @Override
      public LargeObjectMetadata replace(Object key, LargeObjectMetadata value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata oldValue, LargeObjectMetadata newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public void clear() {
         this.keyToLargeObjectMetadata.clear();
      }

      @Override
      public boolean containsKey(Object key) {
         return this.keyToLargeObjectMetadata.containsKey(key);
      }

      @Override
      public boolean containsValue(Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI containsValue");
      }

      @Override
      public LargeObjectMetadata get(Object key) {
         return this.keyToLargeObjectMetadata.get(key);
      }

      @Override
      public boolean isEmpty() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI isEmpty");
      }

      @Override
      public LargeObjectMetadata put(Object key, LargeObjectMetadata value) {
         return this.keyToLargeObjectMetadata.put(key, value);
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata> m) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata remove(Object key) {
         return this.keyToLargeObjectMetadata.remove(key);
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
      public void putForExternalRead(Object key, LargeObjectMetadata value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putForExternalRead");
      }

      @Override
      public void evict(Object key) {
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
      public LargeObjectMetadata put(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public LargeObjectMetadata putIfAbsent(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata> map, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata replace(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata oldValue, LargeObjectMetadata value,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public LargeObjectMetadata put(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public LargeObjectMetadata putIfAbsent(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata> map, long lifespan,
               TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata replace(Object key, LargeObjectMetadata value, long lifespan,
               TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata oldValue, LargeObjectMetadata value,
               long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putAsync(Object key, LargeObjectMetadata value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putAsync(Object key, LargeObjectMetadata value,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putAsync(Object key, LargeObjectMetadata value,
               long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata> data) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata> data, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata> data, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> clearAsync() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI clearAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putIfAbsentAsync(Object key,
               LargeObjectMetadata value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putIfAbsentAsync(Object key,
               LargeObjectMetadata value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> putIfAbsentAsync(Object key,
               LargeObjectMetadata value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
               TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> removeAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> replaceAsync(Object key, LargeObjectMetadata value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> replaceAsync(Object key,
               LargeObjectMetadata value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> replaceAsync(Object key,
               LargeObjectMetadata value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
               TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, LargeObjectMetadata oldValue,
               LargeObjectMetadata newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, LargeObjectMetadata oldValue,
               LargeObjectMetadata newValue, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, LargeObjectMetadata oldValue,
               LargeObjectMetadata newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle,
               TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata> getAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getAsync");
      }

      @Override
      public AdvancedCache<Object, LargeObjectMetadata> getAdvancedCache() {
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
      public Set<Object> keySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI keySet");
      }

      @Override
      public Collection<LargeObjectMetadata> values() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI values");
      }

      @Override
      public Set<java.util.Map.Entry<Object, LargeObjectMetadata>> entrySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI entrySet");
      }

      @Override
      public StreamingHandler<Object> getStreamingHandler() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getStreamingHandler");
      }

   }
}