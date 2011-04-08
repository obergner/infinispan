package org.infinispan.interceptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.ReadCommittedEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.context.impl.NonTxInvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManager;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManagerImpl;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.BidirectionalMap;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectSupportInterceptor}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "interceptors.LargeObjectSupportInterceptorTest")
public class LargeObjectSupportInterceptorTest {

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatLargeObjectChunkingInterceptorRejectsTransactionalInvocationContext()
            throws Throwable {
      LargeObjectSupportInterceptor<Object> objectUnderTest = new LargeObjectSupportInterceptor<Object>();
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
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
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
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
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object()));

      final List<byte[]> receivedChunkData = new ArrayList<byte[]>();
      CommandInterceptor recordingCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command)
                  throws Throwable {
            assert command.getValue() != null : "LargeObjectSupportInterceptor did not store "
                     + "any value in PutKeyValueCommand";
            assert command.getValue() instanceof byte[] : "LargeObjectSupportInterceptor did not store "
                     + "value of type byte[] in command";
            receivedChunkData.add(byte[].class.cast(command.getValue()));

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
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
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
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L), largeObjectMetadataManager);

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

   private Configuration newConfigurationWithMaxChunkSize(final long maxChunkSize) {
      return new Configuration() {
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

      return new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(keyToLargeObjectMetadata),
               FluentConfiguration.LargeObjectSupportConfig.DEFAULT_LARGEOBJECT_METADATA_CACHE);
   }

   private CacheContainer newCacheContainerWithLargeObjectMetadataCache(
            final ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata) {
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

   private static class MapBackedCache implements Cache<Object, LargeObjectMetadata<Object>> {

      private final ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata;

      MapBackedCache(ConcurrentMap<Object, LargeObjectMetadata<Object>> keyToLargeObjectMetadata) {
         this.keyToLargeObjectMetadata = keyToLargeObjectMetadata;
      }

      @Override
      public LargeObjectMetadata<Object> putIfAbsent(Object key, LargeObjectMetadata<Object> value) {
         return this.keyToLargeObjectMetadata.putIfAbsent(key, value);
      }

      @Override
      public boolean remove(Object key, Object value) {
         return this.keyToLargeObjectMetadata.remove(key, value);
      }

      @Override
      public LargeObjectMetadata<Object> replace(Object key, LargeObjectMetadata<Object> value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata<Object> oldValue,
               LargeObjectMetadata<Object> newValue) {
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
      public LargeObjectMetadata<Object> get(Object key) {
         return this.keyToLargeObjectMetadata.get(key);
      }

      @Override
      public boolean isEmpty() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI isEmpty");
      }

      @Override
      public LargeObjectMetadata<Object> put(Object key, LargeObjectMetadata<Object> value) {
         return this.keyToLargeObjectMetadata.put(key, value);
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata<Object>> m) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata<Object> remove(Object key) {
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
      public void putForExternalRead(Object key, LargeObjectMetadata<Object> value) {
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
      public LargeObjectMetadata<Object> put(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public LargeObjectMetadata<Object> putIfAbsent(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata<Object>> map,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata<Object> replace(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata<Object> oldValue,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public LargeObjectMetadata<Object> put(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI put");
      }

      @Override
      public LargeObjectMetadata<Object> putIfAbsent(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsent");
      }

      @Override
      public void putAll(Map<? extends Object, ? extends LargeObjectMetadata<Object>> map,
               long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAll");
      }

      @Override
      public LargeObjectMetadata<Object> replace(Object key, LargeObjectMetadata<Object> value,
               long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public boolean replace(Object key, LargeObjectMetadata<Object> oldValue,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit lifespanUnit,
               long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replace");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putAsync(Object key,
               LargeObjectMetadata<Object> value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata<Object>> data) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata<Object>> data, long lifespan,
               TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> putAllAsync(
               Map<? extends Object, ? extends LargeObjectMetadata<Object>> data, long lifespan,
               TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putAllAsync");
      }

      @Override
      public NotifyingFuture<Void> clearAsync() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI clearAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putIfAbsentAsync(Object key,
               LargeObjectMetadata<Object> value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putIfAbsentAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> putIfAbsentAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI putIfAbsentAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> removeAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI removeAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> replaceAsync(Object key,
               LargeObjectMetadata<Object> value) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> replaceAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> replaceAsync(Object key,
               LargeObjectMetadata<Object> value, long lifespan, TimeUnit lifespanUnit,
               long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key,
               LargeObjectMetadata<Object> oldValue, LargeObjectMetadata<Object> newValue) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key,
               LargeObjectMetadata<Object> oldValue, LargeObjectMetadata<Object> newValue,
               long lifespan, TimeUnit unit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<Boolean> replaceAsync(Object key,
               LargeObjectMetadata<Object> oldValue, LargeObjectMetadata<Object> newValue,
               long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI replaceAsync");
      }

      @Override
      public NotifyingFuture<LargeObjectMetadata<Object>> getAsync(Object key) {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getAsync");
      }

      @Override
      public AdvancedCache<Object, LargeObjectMetadata<Object>> getAdvancedCache() {
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
      public Collection<LargeObjectMetadata<Object>> values() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI values");
      }

      @Override
      public Set<java.util.Map.Entry<Object, LargeObjectMetadata<Object>>> entrySet() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI entrySet");
      }
   }
}
