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
import org.infinispan.StreamingHandler;
import org.infinispan.commands.write.PutKeyLargeObjectCommand;
import org.infinispan.commands.write.RemoveLargeObjectCommand;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.ReadCommittedEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.context.impl.NonTxInvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.Chunk;
import org.infinispan.largeobjectsupport.ChunkMetadata;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManager;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManagerImpl;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
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
   public void testThatVisitPutKeyLargeObjectCommandRejectsTransactionalInvocationContext()
            throws Throwable {
      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object(), null));

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

      PutKeyLargeObjectCommand writeLargeObjectCommand = new PutKeyLargeObjectCommand(
               largeObjectKey, largeObject, false, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyLargeObjectCommand(txCtx, writeLargeObjectCommand);
   }

   @Test
   public void testThatVisitPutKeyLargeObjectCommandCorrectlyCallsInterceptorPipelineForEachChunk()
            throws Throwable {
      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object(), null));

      final List<byte[]> receivedChunkData = new ArrayList<byte[]>();
      CommandInterceptor recordingCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyLargeObjectCommand(InvocationContext ctx,
                  PutKeyLargeObjectCommand command) throws Throwable {
            assert command.getValue() != null : "LargeObjectSupportInterceptor did not store "
                     + "any value in PutKeyValueCommand";
            assert command.getValue() instanceof Chunk : "LargeObjectSupportInterceptor did not store "
                     + "value of type Chunk in command";
            receivedChunkData.add(Chunk.class.cast(command.getValue()).getData());

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

      PutKeyLargeObjectCommand writeLargeObjectCommand = new PutKeyLargeObjectCommand(
               largeObjectKey, largeObject, false, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyLargeObjectCommand(ctx, writeLargeObjectCommand);

      assert Arrays.equals(receivedChunkData.get(0), new byte[] { 1, 2, 3 });
      assert Arrays.equals(receivedChunkData.get(1), new byte[] { 4, 5, 6 });
      assert Arrays.equals(receivedChunkData.get(2), new byte[] { 7 });
   }

   @Test
   public void testThatVisitPutKeyLargeObjectCommandCorrectlyStoresLargeObjectMetadata()
            throws Throwable {
      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      LargeObjectMetadataManager largeObjectMetadataManager = newLargeObjectMetadataManagerWithLargeObjectMetadataStored(
               null, null);
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L), largeObjectMetadataManager);

      CommandInterceptor noopCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitPutKeyLargeObjectCommand(InvocationContext ctx,
                  PutKeyLargeObjectCommand command) throws Throwable {
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

      PutKeyLargeObjectCommand writeLargeObjectCommand = new PutKeyLargeObjectCommand(
               largeObjectKey, largeObject, false, null, 0, 0, Collections.<Flag> emptySet());

      objectUnderTest.visitPutKeyLargeObjectCommand(ctx, writeLargeObjectCommand);

      assert largeObjectMetadataManager.alreadyUsedByLargeObject(largeObjectKey) : "LargeObjectSupportInterceptor did NOT store Large Object's metadata";
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatVisitRemoveLargeObjectCommandRejectsTransactionalInvocationContext()
            throws Throwable {
      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(new Object(), null));

      LocalTxInvocationContext txCtx = new LocalTxInvocationContext() {
         @Override
         public void putLookedUpEntry(Object key, CacheEntry e) {
            // Ignore
         }
      };

      Object largeObjectKey = new Object();

      RemoveLargeObjectCommand writeLargeObjectCommand = new RemoveLargeObjectCommand(
               largeObjectKey, null, Collections.<Flag> emptySet());

      objectUnderTest.visitRemoveLargeObjectCommand(txCtx, writeLargeObjectCommand);
   }

   @Test
   public void testThatVisitRemoveLargeObjectCommandCorrectlyCallsInterceptorPipelineForEachChunk()
            throws Throwable {
      LargeObjectMetadata largeObjectMetadata = newTestLargeObjectMetadata();
      List<Object> expectedChunkKeys = new ArrayList<Object>();
      for (ChunkMetadata chunkMetadata : largeObjectMetadata)
         expectedChunkKeys.add(chunkMetadata.getKey());

      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      objectUnderTest.init(
               newConfigurationWithMaxChunkSize(3L),
               newLargeObjectMetadataManagerWithLargeObjectMetadataStored(
                        largeObjectMetadata.getLargeObjectKey(), largeObjectMetadata));

      final List<Object> receivedChunkKeys = new ArrayList<Object>();
      CommandInterceptor recordingCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitRemoveLargeObjectCommand(InvocationContext ctx,
                  RemoveLargeObjectCommand command) throws Throwable {
            assert command.getKey() != null : "LargeObjectSupportInterceptor did not store "
                     + "any value in PutKeyValueCommand";
            receivedChunkKeys.add(command.getKey());

            return null;
         }
      };
      objectUnderTest.setNext(recordingCommandInterceptor);

      InvocationContext ctx = new NonTxInvocationContext();

      RemoveLargeObjectCommand writeLargeObjectCommand = new RemoveLargeObjectCommand(
               largeObjectMetadata.getLargeObjectKey(), null, Collections.<Flag> emptySet());

      objectUnderTest.visitRemoveLargeObjectCommand(ctx, writeLargeObjectCommand);

      assert Arrays.equals(expectedChunkKeys.toArray(new Object[0]),
               receivedChunkKeys.toArray(new Object[0]));
   }

   @Test
   public void testThatVisitRemoveLargeObjectCommandCorrectlyRemovesLargeObjectMetadata()
            throws Throwable {
      LargeObjectMetadata largeObjectMetadata = newTestLargeObjectMetadata();

      LargeObjectSupportInterceptor objectUnderTest = new LargeObjectSupportInterceptor();
      LargeObjectMetadataManager largeObjectMetadataManager = newLargeObjectMetadataManagerWithLargeObjectMetadataStored(
               largeObjectMetadata.getLargeObjectKey(), largeObjectMetadata);
      objectUnderTest.init(newConfigurationWithMaxChunkSize(3L), largeObjectMetadataManager);

      CommandInterceptor noopCommandInterceptor = new CommandInterceptor() {
         @Override
         public Object visitRemoveLargeObjectCommand(InvocationContext ctx,
                  RemoveLargeObjectCommand command) throws Throwable {
            return null;
         }
      };
      objectUnderTest.setNext(noopCommandInterceptor);

      Object largeObjectKey = new Object();
      InvocationContext ctx = new NonTxInvocationContext();

      RemoveLargeObjectCommand writeLargeObjectCommand = new RemoveLargeObjectCommand(
               largeObjectKey, null, Collections.<Flag> emptySet());

      objectUnderTest.visitRemoveLargeObjectCommand(ctx, writeLargeObjectCommand);

      assert !largeObjectMetadataManager.alreadyUsedByLargeObject(largeObjectKey) : "LargeObjectSupportInterceptor did NOT remove Large Object's metadata";
   }

   private Configuration newConfigurationWithMaxChunkSize(final long maxChunkSize) {
      return new Configuration() {
         @Override
         public long getMaximumChunkSizeInBytes() {
            return maxChunkSize;
         }
      };
   }

   private LargeObjectMetadata newTestLargeObjectMetadata() {
      return LargeObjectMetadata.newBuilder().withLargeObjectKey(new Object())
               .withMaxChunkSizeInBytes(344L).addChunk(new Object(), 55L)
               .addChunk(new Object(), 55L).addChunk(new Object(), 55L).addChunk(new Object(), 55L)
               .build();
   }

   private LargeObjectMetadataManager newLargeObjectMetadataManagerWithLargeObjectMetadataStored(
            Object largeObjectKey, LargeObjectMetadata largeObjectMetadata) {
      ConcurrentMap<Object, LargeObjectMetadata> keyToLargeObjectMetadata = new ConcurrentHashMap<Object, LargeObjectMetadata>(
               1);
      if (largeObjectKey != null && largeObjectMetadata == null) {
         LargeObjectMetadata largeObjectMetadat = LargeObjectMetadata.newBuilder()
                  .withLargeObjectKey(largeObjectKey).withMaxChunkSizeInBytes(3L)
                  .addChunk(new Object(), 3L).build();
         keyToLargeObjectMetadata.put(largeObjectMetadat.getLargeObjectKey(), largeObjectMetadat);
      } else if (largeObjectKey != null && largeObjectMetadata != null) {
         keyToLargeObjectMetadata.put(largeObjectMetadata.getLargeObjectKey(), largeObjectMetadata);
      }

      return new LargeObjectMetadataManagerImpl(
               newCacheContainerWithLargeObjectMetadataCache(keyToLargeObjectMetadata),
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

      @Override
      public StreamingHandler<Object> getStreamingHandler() {
         throw new org.jboss.util.NotImplementedException("FIXME NYI getStreamingHandler");
      }
   }
}