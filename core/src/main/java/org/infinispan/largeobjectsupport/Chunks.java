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
package org.infinispan.largeobjectsupport;

import net.jcip.annotations.NotThreadSafe;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Represents a given <em>Large Object</em>'s collection of {@link Chunk <code>Chunk</code>}s.
 * </p>
 * <p>
 * This class is probably <strong>the</strong> single most important class for realizing
 * INFINISPAN's <code>LargeObjectSupport</code>. Its responsibilities are manifold:
 * <ol>
 * <li>
 * Divide a <em>Large Object</em> into {@link Chunk <code>Chunk</code>}s.</li>
 * <li>
 * Enable lazy iteration over all {@link Chunk <code>Chunk</code>}s a <em>Large Object</em> is
 * divided into. Here, &quot;lazy&quot; refers to the fact that each <code>Chunk</code> is created
 * on demand. Moreover does this class <strong>not</strong> hold references to any
 * <code>Chunk</code>s already produced since otherwise it would threaten to use up the heap space.</li>
 * <li>
 * Assign a unique <code>chunk key</code> to each newly produced <code>Chunk</code>.</li>
 * <li>
 * Make sure that no two <code>Chunk</code>s are stored on the same node in the INFINISPAN cluster.</li>
 * </ol>
 * </p>
 * <p>
 * <strong>IMPORTANT</strong> Each <code>Chunks</code> instance may be used only
 * <strong>once</strong>. More precisely, it disallows iterating over a <em>Large Object</em> more
 * that once. If a client violates this rule, an <code>IllegalStateException</code> will be thrown.<br/>
 * The reason for this restriction is that thus INFINISPAN will avoid to read a
 * <em>Large Object</em> more often than absolutely necessary.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@NotThreadSafe
public class Chunks<K> implements Iterable<Chunk> {

   private final K largeObjectKey;

   private final InputStream largeObject;

   private final long maxChunkSizeInBytes;

   /**
    * The distributionManager: used to obtain a chunkKey for each new chunk.
    */
   private final DistributionManager distributionManager;

   /**
    * The configuration: used to obtain replication factor/num owners.
    */
   private final Configuration configuration;

   /**
    * Embedded cache manager: needed to check whether a given chunk key has already been used before
    * for a different object.
    */
   private final EmbeddedCacheManager embeddedCacheManager;

   private final ChunkKeyProducer chunkKeyProducer = new ChunkKeyProducer();

   private long numberOfAlreadyReadBytes = 0L;

   public Chunks(K largeObjectKey, InputStream largeObject,
            DistributionManager distributionManager, Configuration configuration,
            EmbeddedCacheManager embeddedCacheManager) {
      if (!largeObject.markSupported())
         throw new IllegalArgumentException("The supplied LargeObject InputStream does not "
                  + "support mark(). This, however, is required.");
      this.largeObjectKey = largeObjectKey;
      this.largeObject = largeObject;
      this.distributionManager = distributionManager;
      this.configuration = configuration;
      this.embeddedCacheManager = embeddedCacheManager;
      this.maxChunkSizeInBytes = configuration.getMaximumChunkSizeInBytes();
   }

   @Override
   public Iterator<Chunk> iterator() throws IllegalStateException {
      if (allBytesRead())
         throw new IllegalStateException("This Chunks object has already been iterated over. "
                  + "More than one iteration is not supported.");

      return this.new ChunkIterator();
   }

   public LargeObjectMetadata<K> largeObjectMetadata() throws IllegalStateException {
      if (!allBytesRead())
         throw new IllegalStateException(
                  "Cannot create LargeObjectMetadata: this Chunks object has "
                           + "not yet read all bytes from its LargeObject.");
      List<String> allChunkKeys = chunkKeyProducer.chunkKeys();

      return new LargeObjectMetadata<K>(largeObjectKey, numberOfAlreadyReadBytes,
               allChunkKeys.toArray(new String[allChunkKeys.size()]));
   }

   private boolean allBytesRead() {
      try {
         largeObject.mark(1);
         boolean answer = (largeObject.read() == -1);
         largeObject.reset();
         return answer;
      } catch (IOException e) {
         throw new RuntimeException("Failed to read from/reset LargeObject InputStream: "
                  + e.getMessage(), e);
      }
   }

   private long maximumLargeObjectSizeInBytes() {
      int numberOfNodeInCluster = distributionManager.getTopologyInfo().getAllTopologyInfo().size();
      if (numberOfNodeInCluster == 0)
         throw new IllegalStateException("The number of nodes in the cluster is 0");

      int replicationFactor = configuration.getNumOwners();
      if (replicationFactor < 1)
         throw new IllegalStateException("The replication factor is less than 1");

      return (numberOfNodeInCluster * maxChunkSizeInBytes) / replicationFactor;
   }

   private class ChunkKeyProducer {

      private final List<ChunkKeyNodeAddressesTuple> alreadyUsedUpChunkKeysAndNodeAddresses = new ArrayList<ChunkKeyNodeAddressesTuple>();

      private final Random randomGenerator = new Random();

      List<String> chunkKeys() {
         final List<String> allChunkKeys = new ArrayList<String>(
                  alreadyUsedUpChunkKeysAndNodeAddresses.size());
         for (ChunkKeyNodeAddressesTuple chunkKeyAndNodeAddresses : alreadyUsedUpChunkKeysAndNodeAddresses)
            allChunkKeys.add(chunkKeyAndNodeAddresses.chunkKey);
         return allChunkKeys;
      }

      ChunkKeyNodeAddressesTuple nextChunkKeyAndNodeAddresses() {
         // TODO: Shouldn't we impose a (configurable) upper limit to the number of attempts at
         // producing a valid new chunk key?
         while (true) {
            /*
             * TODO: Find a better algorithm for producing random chunk keys.
             * 
             * Requirements:
             * 
             * 1. Should distribute evenly across our constant hash ring. 2. Should produce a string
             * of a predefind length.
             */
            byte[] chunkKeyBytes = new byte[20];
            randomGenerator.nextBytes(chunkKeyBytes);
            String chunkKeyCandidate = new String(chunkKeyBytes);
            List<Address> correspondingNodeAddresses = distributionManager
                     .locate(chunkKeyCandidate);
            if (isAllowed(chunkKeyCandidate, correspondingNodeAddresses)) {
               ChunkKeyNodeAddressesTuple allowedChunkKeyAndNodeAddresses = new ChunkKeyNodeAddressesTuple(
                        chunkKeyCandidate, correspondingNodeAddresses);
               alreadyUsedUpChunkKeysAndNodeAddresses.add(allowedChunkKeyAndNodeAddresses);
               return allowedChunkKeyAndNodeAddresses;
            }
         }
      }

      private boolean isAllowed(String chunkKey, List<Address> correspondingNodeAddresses) {
         return !hasAlreadyBeenProduced(chunkKey) && !isAlreadyUsedByDifferentLargeObject(chunkKey)
                  && !isStoredInAnAlreadyUsedNode(chunkKey, correspondingNodeAddresses);
      }

      private boolean hasAlreadyBeenProduced(String chunkKey) {
         for (ChunkKeyNodeAddressesTuple chunkKeyAndNodeAddress : alreadyUsedUpChunkKeysAndNodeAddresses) {
            if (chunkKeyAndNodeAddress.chunkKey.equals(chunkKey))
               return true;
         }

         return false;
      }

      private boolean isAlreadyUsedByDifferentLargeObject(String chunkKey) {
         for (String cacheName : embeddedCacheManager.getCacheNames()) {
            Cache<?, ?> cache = embeddedCacheManager.getCache(cacheName, false);
            if (cache != null && cache.containsKey(chunkKey))
               return true;
         }

         return false;
      }

      private boolean isStoredInAnAlreadyUsedNode(String chunkKey,
               List<Address> correspondingNodeAddresses) {
         // FIXME: Is this really safe during a rehash?
         for (ChunkKeyNodeAddressesTuple chunkKeyAndNodeAddresses : alreadyUsedUpChunkKeysAndNodeAddresses) {
            if (!Collections.disjoint(correspondingNodeAddresses,
                     chunkKeyAndNodeAddresses.nodeAddresses))
               return true;
         }
         return false;
      }
   }

   private static class ChunkKeyNodeAddressesTuple {

      private final String chunkKey;

      private final List<Address> nodeAddresses;

      ChunkKeyNodeAddressesTuple(String chunkKey, List<Address> nodeAddresses) {
         this.chunkKey = chunkKey;
         this.nodeAddresses = nodeAddresses;
      }
   }

   private class ChunkIterator implements Iterator<Chunk> {

      private static final int MAX_BUFFER_SIZE_IN_BYTES = 4 * 1024;

      @Override
      public boolean hasNext() {
         return !allBytesRead();
      }

      @Override
      public Chunk next() {
         try {
            int effectiveBufferSize = effectiveBufferSize();
            ByteArrayOutputStream chunkData = new ByteArrayOutputStream();

            byte[] buffer = new byte[effectiveBufferSize];
            int n = 0;
            int numberOfBytesToReadNext = effectiveBufferSize;
            while ((n = largeObject.read(buffer, 0, numberOfBytesToReadNext)) != -1
                     && (chunkData.size() < maxChunkSizeInBytes) && (numberOfBytesToReadNext > 0)) {
               chunkData.write(buffer, 0, n);
               numberOfAlreadyReadBytes += n;
               numberOfBytesToReadNext = (maxChunkSizeInBytes - chunkData.size() >= effectiveBufferSize) ? effectiveBufferSize
                        : (int) (maxChunkSizeInBytes - chunkData.size());

               // Check if our LargeObject is bigger than allowed
               if (chunkData.size() > maximumLargeObjectSizeInBytes())
                  throw new LargeObjectExceedsSizeLimitException(
                           "The number of bytes already read [" + numberOfAlreadyReadBytes
                                    + "] exceeds the size limit ["
                                    + maximumLargeObjectSizeInBytes() + "] for large objects",
                           maxChunkSizeInBytes, distributionManager.getTopologyInfo()
                                    .getAllTopologyInfo().size(), configuration.getNumOwners());
            }

            ChunkKeyNodeAddressesTuple chunkKeyAndNodeAddress = chunkKeyProducer
                     .nextChunkKeyAndNodeAddresses();

            return new Chunk(chunkKeyAndNodeAddress.chunkKey, chunkKeyAndNodeAddress.nodeAddresses,
                     chunkData.toByteArray());
         } catch (IOException e) {
            throw new RuntimeException("Failed to read Chunk from LargeObject InputStream: "
                     + e.getMessage(), e);
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException("ChunkIterator does not support remove()");
      }

      private int effectiveBufferSize() {
         return maxChunkSizeInBytes < MAX_BUFFER_SIZE_IN_BYTES ? (int) maxChunkSizeInBytes
                  : MAX_BUFFER_SIZE_IN_BYTES;
      }
   }
}
