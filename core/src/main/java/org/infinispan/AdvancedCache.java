package org.infinispan;

import org.infinispan.batch.BatchContainer;
import org.infinispan.container.DataContainer;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.stats.Stats;

import javax.transaction.TransactionManager;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * An advanced interface that exposes additional methods not available on {@link Cache}.
 *
 * @author Manik Surtani
 * @since 4.0
 */
public interface AdvancedCache<K, V> extends Cache<K, V> {
   
   /**
    * <em>Large Object Support</em>: Take the key {@code key} and the supplied
    * {@link java.io.InputStream <code>largeObject</code>} and store the latter under the given key.
    * A <em>Large Object</em> is defined as a value whose size may potentially exceed the heap size
    * of any single JVM participating in an INFINISPAN cluster. Therefore, it cannot be stored in
    * its entirety on a single node and needs to be split into chunks to be distributed across
    * multiple nodes.
    * 
    * @param key
    *           The {@code key} which to store the given <code>largeObject</code> under
    * @param largeObject
    *           The <em>Large Object</em> to store, represented by an <code>InputStream</code>
    *           
    * @since 5.1
    */
   void writeToKey(K key, InputStream largeObject);

   /**
    * A builder-style method that adds flags to any API call.  For example, consider the following code snippet:
    * <pre>
    *   cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);
    * </pre>
    * will invoke a cache.get() with a write lock forced.
    * @param flags a set of flags to apply.  See the {@link Flag} documentation.
    * @return a cache on which a real operation is to be invoked.
    */
   AdvancedCache<K, V> withFlags(Flag... flags);

   /**
    * Adds a custom interceptor to the interceptor chain, at specified position, where the first interceptor in the
    * chain is at position 0 and the last one at NUM_INTERCEPTORS - 1.
    *
    * @param i        the interceptor to add
    * @param position the position to add the interceptor
    */
   void addInterceptor(CommandInterceptor i, int position);

   /**
    * Adds a custom interceptor to the interceptor chain, after an instance of the specified interceptor type. Throws a
    * cache exception if it cannot find an interceptor of the specified type.
    *
    * @param i                interceptor to add
    * @param afterInterceptor interceptor type after which to place custom interceptor
    */
   void addInterceptorAfter(CommandInterceptor i, Class<? extends CommandInterceptor> afterInterceptor);

   /**
    * Adds a custom interceptor to the interceptor chain, before an instance of the specified interceptor type. Throws a
    * cache exception if it cannot find an interceptor of the specified type.
    *
    * @param i                 interceptor to add
    * @param beforeInterceptor interceptor type before which to place custom interceptor
    */
   void addInterceptorBefore(CommandInterceptor i, Class<? extends CommandInterceptor> beforeInterceptor);

   /**
    * Removes the interceptor at a specified position, where the first interceptor in the chain is at position 0 and the
    * last one at getInterceptorChain().size() - 1.
    *
    * @param position the position at which to remove an interceptor
    */
   void removeInterceptor(int position);

   /**
    * Removes the interceptor of specified type.
    *
    * @param interceptorType type of interceptor to remove
    */
   void removeInterceptor(Class<? extends CommandInterceptor> interceptorType);

   /**
    * Retrieves the current Interceptor chain.
    *
    * @return an immutable {@link java.util.List} of {@link org.infinispan.interceptors.base.CommandInterceptor}s
    *         configured for this cache
    */
   List<CommandInterceptor> getInterceptorChain();

   /**
    * @return the eviction manager - if one is configured - for this cache instance
    */
   EvictionManager getEvictionManager();

   /**
    * @return the component registry for this cache instance
    */
   ComponentRegistry getComponentRegistry();

   /**
    * Retrieves a reference to the {@link org.infinispan.distribution.DistributionManager} if the cache is configured
    * to use Distribution.  Otherwise, returns a null.
    * @return a DistributionManager, or null.
    */
   DistributionManager getDistributionManager();

   /**
    * Locks a given key or keys eagerly across cache nodes in a cluster.
    * <p>
    * Keys can be locked eagerly in the context of a transaction only
    *
    * @param keys the keys to lock
    * @return true if the lock acquisition attempt was successful for <i>all</i> keys; false otherwise.
    */   
   boolean lock(K... keys);

   /**
    * Locks collections of keys eagerly across cache nodes in a cluster.
    * <p>
    * Collections of keys can be locked eagerly in the context of a transaction only
    * 
    * 
    * @param keys collection of keys to lock
    * @return true if the lock acquisition attempt was successful for <i>all</i> keys; false otherwise. 
    */
   boolean lock(Collection<? extends K> keys);

   RpcManager getRpcManager();

   BatchContainer getBatchContainer();

   InvocationContextContainer getInvocationContextContainer();

   DataContainer getDataContainer();

   TransactionManager getTransactionManager();

   Stats getStats();
}
