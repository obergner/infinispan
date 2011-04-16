package org.infinispan.largeobjectsupport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
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
      Map<Object, Object> chunkCache = new HashMap<Object, Object>();
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
         if (Arrays.equals(expectedChunk, (byte[]) chunk)) return true;
      }
      return false;
   }

   @Test
   public void testThatWritingAByteStreamPlusCloseStoresCorrectMetadata() throws IOException {
      Map<Object, Object> chunkCache = new HashMap<Object, Object>();
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
   }
}
