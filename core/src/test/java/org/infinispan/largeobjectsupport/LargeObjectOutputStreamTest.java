package org.infinispan.largeobjectsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.StreamingHandler;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectOutputStream}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class LargeObjectOutputStreamTest {

   @Test
   public void testThatWritingAByteStreamPlusCloseProducesCorrectChunks() throws IOException {
      Cache<Object, Object> chunkCache = newCacheWithRemoveKeyRecorder(new AtomicInteger());
      LargeObjectMetadataManager metadataManager = newLargeObjectMetadataManagerWithBackingMap();
      Object largeObjectKey = new Object();
      byte[] largeObject = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
      byte[][] expectedChunks = new byte[][] { new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 },
               new byte[] { 7, 8 } };

      LargeObjectOutputStream objectUnderTest = new LargeObjectOutputStream(largeObjectKey,
               chunkCache, metadataManager.chunkKeyGenerator(), 3L, metadataManager);
      for (byte b : largeObject) {
         objectUnderTest.write(b);
      }
      objectUnderTest.close();

      for (byte[] expectedChunk : expectedChunks) {
         assert cacheContainsChunk(chunkCache, expectedChunk) : "Chunk cache does not contain expected chunk "
                  + expectedChunk;
      }
   }

   private boolean cacheContainsChunk(Map<Object, Object> cache, byte[] expectedChunk) {
      for (Object chunk : cache.values()) {
         if (Arrays.equals(expectedChunk, ((Chunk) chunk).getData())) return true;
      }
      return false;
   }

   @Test
   public void testThatWritingAByteStreamPlusCloseStoresCorrectMetadata() throws IOException {
      Cache<Object, Object> chunkCache = newCacheWithRemoveKeyRecorder(new AtomicInteger());
      LargeObjectMetadataManager metadataManager = newLargeObjectMetadataManagerWithBackingMap();
      Object largeObjectKey = new Object();
      byte[] largeObject = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };

      LargeObjectOutputStream objectUnderTest = new LargeObjectOutputStream(largeObjectKey,
               chunkCache, metadataManager.chunkKeyGenerator(), 3L, metadataManager);
      for (byte b : largeObject) {
         objectUnderTest.write(b);
      }
      objectUnderTest.close();

      LargeObjectMetadata storedMetadata = metadataManager
               .correspondingLargeObjectMetadata(largeObjectKey);
      assert storedMetadata != null : "LargeObjectOutputStream did not store any metadata";
      assert storedMetadata.getChunkMetadata().length == 3 : "Stored metadata is invalid";
   }

   @Test
   public void testThatOverwritingAPreviouslyStoredLargeObjectCallsRemoveKey() throws IOException {
      AtomicInteger removeKeyCount = new AtomicInteger(0);
      Cache<Object, Object> chunkCache = newCacheWithRemoveKeyRecorder(removeKeyCount);
      LargeObjectMetadataManager metadataManager = newLargeObjectMetadataManagerWithBackingMap();
      Object largeObjectKey = new Object();

      byte[] firstLargeObject = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
      LargeObjectOutputStream objectUnderTest = new LargeObjectOutputStream(largeObjectKey,
               chunkCache, metadataManager.chunkKeyGenerator(), 3L, metadataManager);
      for (byte b : firstLargeObject) {
         objectUnderTest.write(b);
      }
      objectUnderTest.close();

      byte[] secondLargeObject = new byte[] { 10, 22, 30, 40, 50, 60, 70, 80 };
      LargeObjectOutputStream objectUnderTest2 = new LargeObjectOutputStream(largeObjectKey,
               chunkCache, metadataManager.chunkKeyGenerator(), 3L, metadataManager);
      for (byte b : secondLargeObject) {
         objectUnderTest2.write(b);
      }
      objectUnderTest2.close();

      assert removeKeyCount.get() >= 1 : "Overwriting a previously stored large object did NOT call StreamingHandler.removeKey(key)";
   }

   private LargeObjectMetadataManager newLargeObjectMetadataManagerWithBackingMap() {
      return new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(new ConcurrentHashMap<Object, LargeObjectMetadata>()),
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
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

   private Cache<Object, Object> newCacheWithRemoveKeyRecorder(AtomicInteger removeKeyCount) {
      return new MapBackedCache<Object>(new ConcurrentHashMap<Object, Object>(), removeKeyCount);
   }

   private static class MapBackedCache<V> implements Cache<Object, V> {

      private final ConcurrentMap<Object, V> backingMap;

      private final AtomicInteger removeKeyCount;

      MapBackedCache(ConcurrentMap<Object, V> backingMap) {
         this.backingMap = backingMap;
         this.removeKeyCount = new AtomicInteger(0);
      }

      MapBackedCache(ConcurrentMap<Object, V> backingMap, AtomicInteger removeKeyCount) {
         this.backingMap = backingMap;
         this.removeKeyCount = removeKeyCount;
      }

      @Override
      public V putIfAbsent(Object key, V value) {
         return this.backingMap.putIfAbsent(key, value);
      }

      @Override
      public boolean remove(Object key, Object value) {
         return this.backingMap.remove(key, value);
      }

      @Override
      public V replace(Object key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, V oldValue, V newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public void clear() {
         this.backingMap.clear();
      }

      @Override
      public boolean containsKey(Object key) {
         return this.backingMap.containsKey(key);
      }

      @Override
      public boolean containsValue(Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI containsValue");
      }

      @Override
      public V get(Object key) {
         return this.backingMap.get(key);
      }

      @Override
      public boolean isEmpty() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI isEmpty");
      }

      @Override
      public V put(Object key, V value) {
         return this.backingMap.put(key, value);
      }

      @Override
      public void putAll(Map<? extends Object, ? extends V> m) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V remove(Object key) {
         return this.backingMap.remove(key);
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
      public void putForExternalRead(Object key, V value) {
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
      public V put(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public V putIfAbsent(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends V> map, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V replace(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, V oldValue, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public V put(Object key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
               TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public V putIfAbsent(Object key, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends V> map, long lifespan,
               TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public V replace(Object key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
               TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public NotifyingFuture<V> putAsync(Object key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<V> putAsync(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<V> putAsync(Object key, V value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends Object, ? extends V> data) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends Object, ? extends V> data,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(Map<? extends Object, ? extends V> data,
               long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> clearAsync() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI clearAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(Object key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<V> putIfAbsentAsync(Object key, V value, long lifespan,
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
      public NotifyingFuture<V> replaceAsync(Object key, V value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> replaceAsync(Object key, V value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> replaceAsync(Object key, V value, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, V oldValue, V newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, V oldValue, V newValue,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key, V oldValue, V newValue,
               long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<V> getAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getAsync");
      }

      @Override
      public AdvancedCache<Object, V> getAdvancedCache() {
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
      public Collection<V> values() {
         return backingMap.values();
      }

      @Override
      public Set<java.util.Map.Entry<Object, V>> entrySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI entrySet");
      }

      @Override
      public StreamingHandler<Object> getStreamingHandler() {
         return new StreamingHandler<Object>() {

            @Override
            public void writeToKey(Object key, InputStream largeObject) {
               throw new org.jboss.util.NotImplementedException("FIXME NYI writeToKey");
            }

            @Override
            public OutputStream writeToKey(Object key) {
               throw new org.jboss.util.NotImplementedException("FIXME NYI writeToKey");
            }

            @Override
            public InputStream readFromKey(Object key) {
               throw new org.jboss.util.NotImplementedException("FIXME NYI readFromKey");
            }

            @Override
            public boolean removeKey(Object key) {
               removeKeyCount.incrementAndGet();
               return backingMap.remove(key) != null;
            }

            @Override
            public StreamingHandler<Object> withFlags(Flag... flags) {
               throw new org.jboss.util.NotImplementedException("FIXME NYI withFlags");
            }
         };
      }
   }
}
