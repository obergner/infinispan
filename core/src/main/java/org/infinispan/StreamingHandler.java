/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan;

import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.context.Flag;

/**
 * <p>
 * A {@code StreamingHandler} is a specialized caching service capable of handling so called
 * <em>Large Objects</em>, i.e. objects that are too big to easily fit into memory and thus need to
 * be partitioned into chunks of manageable size. Those chunks are then distributed evenly across
 * the cluster. To avoid loading such <em>Large Objects</em> into memory they are manipulated using
 * {@link java.io.InputStream <code>InputStreams</code>} and {@link java.io.OutputStream
 * <code>OutputStreams</code>}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public interface StreamingHandler<K> {

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
    * <em>Large Object Support</em>: Take the key {@code key} and return an
    * {@link java.io.OutputStream <code>OutputStream</code>} capable of storing a
    * <em>Large Object</em>. Client code <strong>must</strong> call
    * {@link java.io.OutputStream#flush() <code>flush</code>} on the {@code OutputStream} returned
    * to complete storing a <em>Large Object</em>. A <em>Large Object</em> is defined as a value
    * whose size may potentially exceed the heap size of any single JVM participating in an
    * INFINISPAN cluster. Therefore, it cannot be stored in its entirety on a single node and needs
    * to be split into chunks to be distributed across multiple nodes.
    * 
    * @param key
    *           The {@code key} which to store the desired <em>Large Object</em> under
    * @return An {@link java.io.OutputStream <code>OutputStream</code>} capable of storing a
    *         <em>Large Object</em>
    */
   OutputStream writeToKey(K key);

   /**
    * <em>Large Object Support</em>: Take the key {@code key} and return an
    * {@link java.io.InputStream <code>InputStream</code>} the caller may use to read the
    * <em>Large Object</em> identified by {@code key}. A <em>Large Object</em> is defined as a value
    * whose size may potentially exceed the heap size of any single JVM participating in an
    * INFINISPAN cluster. Therefore, it cannot be stored in its entirety on a single node and needs
    * to be split into chunks to be distributed across multiple nodes.
    * 
    * @param key
    *           The {@code key} identifying the <em>Large Object</em> the caller wants to read
    * @return An {@link java.io.InputStream <code>InputStream</code>} the caller may read the
    *         request <em>Large Object</em> from
    */
   InputStream readFromKey(K key);

   /**
    * <em>Large Object Support</em>: Remove the <em>Large Object</em> currently mapped to
    * {@code key} from the backing {@code Cache}. Return {@code true} if {@code key} did in fact map
    * to a <em>Large Object</em> when this method was called, {@code false} otherwise.
    * 
    * @param key
    *           The key to remove from the backing cache
    * @return {@code true} if {@code key} did in fact map to a <em>Large Object</em> when this
    *         method was called, {@code false} otherwise
    */
   boolean removeKey(K key);

   /**
    * A builder-style method that adds flags to any API call. For example, consider the following
    * code snippet:
    * 
    * <pre>
    * cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);
    * </pre>
    * 
    * will invoke a cache.get() with a write lock forced.
    * 
    * @param flags
    *           a set of flags to apply. See the {@link Flag} documentation.
    * @return a cache on which a real operation is to be invoked.
    */
   StreamingHandler<K> withFlags(Flag... flags);
}
