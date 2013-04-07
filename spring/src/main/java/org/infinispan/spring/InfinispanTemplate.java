/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.infinispan.spring;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.api.BasicCache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * <p>
 * Helper class that simplifies interfacing Infinispan-agnostic business code with Infinispan's
 * {@link BasicCache}. Mainly serves to shield callers from having to deal with Infinispan's
 * {@link CacheException} hierarchy.
 * </p>
 * <p>
 * Central method is {@link #execute(InfinispanCallback)} which encapsulates exception handling.
 * Otherwise, InfinispanTemplate's interface closely mirrors {@link BasicCache}. Note, though, that
 * it does not manage its {@link BasicCache}'s lifecycle, i.e. it does neither expose
 * {@link BasicCache#start()} nor {@link BasicCache#stop()}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class InfinispanTemplate extends AbstractBasicCacheAccessor {

   /**
    * @see java.util.concurrent.ConcurrentMap#putIfAbsent(java.lang.Object, java.lang.Object)
    */
   public <K, V> V putIfAbsent(final K arg0, final V arg1) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.putIfAbsent(arg0, arg1);
         }
      });
   }

   /**
    * @see java.util.concurrent.ConcurrentMap#remove(java.lang.Object, java.lang.Object)
    */
   public boolean remove(final Object arg0, final Object arg1) {
      return execute(new InfinispanCallback<Boolean>() {
         @Override
         public Boolean doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return cache.remove(arg0, arg1);
         }
      });
   }

   /**
    * @see java.util.concurrent.ConcurrentMap#replace(java.lang.Object, java.lang.Object)
    */
   public <K, V> V replace(final K arg0, final V arg1) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "rawtypes", "unchecked" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.replace(arg0, arg1);
         }
      });
   }

   /**
    * @see java.util.concurrent.ConcurrentMap#replace(java.lang.Object, java.lang.Object,
    *      java.lang.Object)
    */
   public <K, V> boolean replace(final K arg0, final V arg1, final V arg2) {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "rawtypes", "unchecked" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replace(arg0, arg1, arg2);
         }
      });
   }

   /**
    * @see java.util.Map#clear()
    */
   public void clear() {
      execute(new InfinispanCallback<Void>() {
         @Override
         public Void doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            cache.clear();
            return null;
         }
      });
   }

   /**
    * @see java.util.Map#containsKey(java.lang.Object)
    */
   public boolean containsKey(final Object arg0) {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "rawtypes" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.containsKey(arg0);
         }
      });
   }

   /**
    * @see java.util.Map#containsValue(java.lang.Object)
    */
   public boolean containsValue(final Object arg0) {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "rawtypes" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.containsValue(arg0);
         }
      });
   }

   /**
    * @see java.util.Map#entrySet()
    */
   public <K, V> Set<java.util.Map.Entry<K, V>> entrySet() {
      return execute(new InfinispanCallback<Set<java.util.Map.Entry<K, V>>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Set<Entry<K, V>> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.entrySet();
         }
      });
   }

   /**
    * @see java.util.Map#get(java.lang.Object)
    */
   public <V> V get(final Object arg0) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings("unchecked")
         @Override
         public V doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return (V) cache.get(arg0);
         }
      });
   }

   /**
    * @see java.util.Map#isEmpty()
    */
   public boolean isEmpty() {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "rawtypes" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.isEmpty();
         }
      });
   }

   /**
    * @see java.util.Map#keySet()
    */
   public <K> Set<K> keySet() {
      return execute(new InfinispanCallback<Set<K>>() {
         @SuppressWarnings("unchecked")
         @Override
         public Set<K> doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return (Set<K>) cache.keySet();
         }
      });
   }

   /**
    * @see java.util.Map#putAll(java.util.Map)
    */
   public <K, V> void putAll(final Map<? extends K, ? extends V> arg0) {
      execute(new InfinispanCallback<Void>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Void doInInfinispan(final BasicCache cache) throws CacheException {
            cache.putAll(arg0);
            return null;
         }
      });
   }

   /**
    * @see java.util.Map#size()
    */
   public int size() {
      return execute(new InfinispanCallback<Integer>() {
         @Override
         public Integer doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return cache.size();
         }
      });
   }

   /**
    * @see java.util.Map#values()
    */
   public <V> Collection<V> values() {
      return execute(new InfinispanCallback<Collection<V>>() {
         @SuppressWarnings("unchecked")
         @Override
         public Collection<V> doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return (Collection<V>) cache.values();
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#getName()
    */
   public String getName() {
      return getBasicCache().getName();
   }

   /**
    * @see org.infinispan.api.BasicCache#getVersion()
    */
   public String getVersion() {
      return getBasicCache().getVersion();
   }

   /**
    * @see org.infinispan.api.BasicCache#put(java.lang.Object, java.lang.Object)
    */
   public <K, V> V put(final K key, final V value) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.put(key, value);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#put(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> V put(final K key, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.put(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putIfAbsent(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> V putIfAbsent(final K key, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.putIfAbsent(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAll(java.util.Map, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> void putAll(final Map<? extends K, ? extends V> map, final long lifespan, final TimeUnit unit) {
      execute(new InfinispanCallback<Void>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Void doInInfinispan(final BasicCache cache) throws CacheException {
            cache.putAll(map, lifespan, unit);
            return null;
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replace(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> V replace(final K key, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.replace(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replace(java.lang.Object, java.lang.Object,
    *      java.lang.Object, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> boolean replace(final K key, final V oldValue, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replace(key, oldValue, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#put(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> V put(final K key, final V value, final long lifespan, final TimeUnit lifespanUnit,
         final long maxIdleTime, final TimeUnit maxIdleTimeUnit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.put(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putIfAbsent(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> V putIfAbsent(final K key, final V value, final long lifespan, final TimeUnit lifespanUnit,
         final long maxIdleTime, final TimeUnit maxIdleTimeUnit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.putIfAbsent(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAll(java.util.Map, long, java.util.concurrent.TimeUnit,
    *      long, java.util.concurrent.TimeUnit)
    */
   public <K, V> void putAll(final Map<? extends K, ? extends V> map, final long lifespan, final TimeUnit lifespanUnit,
         final long maxIdleTime, final TimeUnit maxIdleTimeUnit) {
      execute(new InfinispanCallback<Void>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Void doInInfinispan(final BasicCache cache) throws CacheException {
            cache.putAll(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
            return null;
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replace(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> V replace(final K key, final V value, final long lifespan, final TimeUnit lifespanUnit,
         final long maxIdleTime, final TimeUnit maxIdleTimeUnit) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public V doInInfinispan(final BasicCache cache) throws CacheException {
            return (V) cache.replace(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replace(java.lang.Object, java.lang.Object,
    *      java.lang.Object, long, java.util.concurrent.TimeUnit, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> boolean replace(final K key, final V oldValue, final V value, final long lifespan,
         final TimeUnit lifespanUnit, final long maxIdleTime, final TimeUnit maxIdleTimeUnit) {
      return execute(new InfinispanCallback<Boolean>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Boolean doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replace(key, oldValue, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAsync(java.lang.Object, java.lang.Object)
    */
   public <K, V> NotifyingFuture<V> putAsync(final K key, final V value) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAsync(key, value);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> putAsync(final K key, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAsync(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> putAsync(final K key, final V value, final long lifespan,
         final TimeUnit lifespanUnit, final long maxIdle, final TimeUnit maxIdleUnit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAllAsync(java.util.Map)
    */
   public <K, V> NotifyingFuture<Void> putAllAsync(final Map<? extends K, ? extends V> data) {
      return execute(new InfinispanCallback<NotifyingFuture<Void>>() {

         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Void> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAllAsync(data);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAllAsync(java.util.Map, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<Void> putAllAsync(final Map<? extends K, ? extends V> data, final long lifespan,
         final TimeUnit unit) {
      return execute(new InfinispanCallback<NotifyingFuture<Void>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Void> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAllAsync(data, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putAllAsync(java.util.Map, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<Void> putAllAsync(final Map<? extends K, ? extends V> data, final long lifespan,
         final TimeUnit lifespanUnit, final long maxIdle, final TimeUnit maxIdleUnit) {
      return execute(new InfinispanCallback<NotifyingFuture<Void>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Void> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putAllAsync(data, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#clearAsync()
    */
   public NotifyingFuture<Void> clearAsync() {
      return execute(new InfinispanCallback<NotifyingFuture<Void>>() {
         @Override
         public NotifyingFuture<Void> doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return cache.clearAsync();
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putIfAbsentAsync(java.lang.Object, java.lang.Object)
    */
   public <K, V> NotifyingFuture<V> putIfAbsentAsync(final K key, final V value) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putIfAbsentAsync(key, value);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putIfAbsentAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> putIfAbsentAsync(final K key, final V value, final long lifespan,
         final TimeUnit unit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putIfAbsentAsync(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#putIfAbsentAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> putIfAbsentAsync(final K key, final V value, final long lifespan,
         final TimeUnit lifespanUnit, final long maxIdle, final TimeUnit maxIdleUnit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.putIfAbsentAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#remove(java.lang.Object)
    */
   public <V> V remove(final Object key) {
      return execute(new InfinispanCallback<V>() {
         @SuppressWarnings("unchecked")
         @Override
         public V doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return (V) cache.remove(key);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#removeAsync(java.lang.Object)
    */
   public <V> NotifyingFuture<V> removeAsync(final Object key) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings("unchecked")
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return (NotifyingFuture<V>) cache.removeAsync(key);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#removeAsync(java.lang.Object, java.lang.Object)
    */
   public NotifyingFuture<Boolean> removeAsync(final Object key, final Object value) {
      return execute(new InfinispanCallback<NotifyingFuture<Boolean>>() {
         @Override
         public NotifyingFuture<Boolean> doInInfinispan(final BasicCache<?, ?> cache) throws CacheException {
            return cache.removeAsync(key, value);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object)
    */
   public <K, V> NotifyingFuture<V> replaceAsync(final K key, final V value) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, value);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> replaceAsync(final K key, final V value, final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, value, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object, long,
    *      java.util.concurrent.TimeUnit, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<V> replaceAsync(final K key, final V value, final long lifespan,
         final TimeUnit lifespanUnit, final long maxIdle, final TimeUnit maxIdleUnit) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object,
    *      java.lang.Object)
    */
   public <K, V> NotifyingFuture<Boolean> replaceAsync(final K key, final V oldValue, final V newValue) {
      return execute(new InfinispanCallback<NotifyingFuture<Boolean>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Boolean> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, oldValue, newValue);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object,
    *      java.lang.Object, long, java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<Boolean> replaceAsync(final K key, final V oldValue, final V newValue,
         final long lifespan, final TimeUnit unit) {
      return execute(new InfinispanCallback<NotifyingFuture<Boolean>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Boolean> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, oldValue, newValue, lifespan, unit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#replaceAsync(java.lang.Object, java.lang.Object,
    *      java.lang.Object, long, java.util.concurrent.TimeUnit, long,
    *      java.util.concurrent.TimeUnit)
    */
   public <K, V> NotifyingFuture<Boolean> replaceAsync(final K key, final V oldValue, final V newValue,
         final long lifespan, final TimeUnit lifespanUnit, final long maxIdle, final TimeUnit maxIdleUnit) {
      return execute(new InfinispanCallback<NotifyingFuture<Boolean>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<Boolean> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
         }
      });
   }

   /**
    * @see org.infinispan.api.BasicCache#getAsync(java.lang.Object)
    */
   public <K, V> NotifyingFuture<V> getAsync(final K key) {
      return execute(new InfinispanCallback<NotifyingFuture<V>>() {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public NotifyingFuture<V> doInInfinispan(final BasicCache cache) throws CacheException {
            return cache.getAsync(key);
         }
      });
   }

   /**
    * Take the supplied {@link InfinispanCallback action} and execute it against this template's
    * {@link BasicCache}. Translate any {@link CacheException}s thrown by that {@literal action}
    * into corresponding exceptions from Spring's {@link DataAccessException} hierarchy.
    * 
    * @param action
    *           The {@link InfinispanCallback} to execute. Must not be {@literal null}.
    * @return Whatever {@literal action} chooses to return. May be null.
    * @throws DataAccessException
    *            If a {@link CacheException} is thrown from
    *            {@link InfinispanCallback#doInInfinispan(BasicCache)}.
    */
   public <T> T execute(final InfinispanCallback<T> action) throws DataAccessException {
      Assert.notNull(action, "Callback 'action' must not be null");
      try {
         return action.doInInfinispan(getBasicCache());
      } catch (final CacheException ce) {
         throw getPersistenceExceptionTranslator().translateExceptionIfPossible(ce);
      }
   }

   public <V> List<V> query(final InfinispanQueryCallback<V> queryBuilderCallback, final Class<V> resultClass)
         throws DataAccessException {
      Assert.notNull(queryBuilderCallback, "Callback 'queryBuilderCallback' must not be null");
      try {
         // FIXME: Remove potentially unsafe cast from BasicCache to Cache
         final SearchManager searchManager = Search.getSearchManager((Cache<?, ?>) getBasicCache());
         final QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(resultClass).get();
         final Query luceneQuery = queryBuilderCallback.doWithQueryBuilder(queryBuilder);
         final CacheQuery cacheQuery = searchManager.getQuery(luceneQuery, resultClass);
         return (List<V>) cacheQuery.list();
      } catch (final CacheException ce) {
         throw getPersistenceExceptionTranslator().translateExceptionIfPossible(ce);
      }
   }
}
