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

/**
 * <p>
 * An {@code Exception} to be thrown in case a {@code LargeObject} that is about to be stored
 * exceeds the maximum possible size.
 * </p>
 * <p>
 * <strong>A LargeObject's maximum possible size</strong><br/>
 * <br/>
 * Let
 * <ol>
 * <li>
 * maxLargeObjectSize = the maximum size in bytes per <em>Large Object</em>,</li>
 * <li>
 * maxChunkSize = the maximum size in bytes per {@link Chunk <code>Chunk</code>},</li>
 * <li>
 * numChunks = the number of {@link Chunk <code>Chunk</code>}s a <em>Large Object</em> is
 * partitioned into,</li>
 * <li>
 * replFactor = the number of each {@link Chunk <code>Chunk</code>}'s (or, equivalently, each
 * <em>Large Object</em>'s) replicas, and</li>
 * <li>
 * numNodes = the number of nodes in the INFINISPAN cluster.</li>
 * </ol>
 * Then the equation
 * 
 * <pre>
 * <code>
 *   maxLargeObjectSize = (maxChunkSize * numNodes) / replFactor
 * </code>
 * </pre>
 * 
 * holds.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class LargeObjectExceedsSizeLimitException extends LargeObjectSupportException {

   /** The serialVersionUID */
   private static final long serialVersionUID = -123235647890480382L;

   private final long maximumChunkSizesInBytes;

   private final int numberOfNodes;

   private final int replicationFactor;

   /**
    * Create a new LargeObjectExceedsSizeLimitException.
    * 
    * @param maximumChunkSizesInBytes
    * @param numberOfNodes
    * @param replicationFactor
    */
   public LargeObjectExceedsSizeLimitException(long maximumChunkSizesInBytes, int numberOfNodes,
            int replicationFactor) {
      this(null, maximumChunkSizesInBytes, numberOfNodes, replicationFactor);
   }

   /**
    * Create a new LargeObjectExceedsMaximumSizeException.
    * 
    * @param msg
    */
   public LargeObjectExceedsSizeLimitException(String msg, long maximumChunkSizesInBytes,
            int numberOfNodes, int replicationFactor) {
      super(msg);
      this.maximumChunkSizesInBytes = maximumChunkSizesInBytes;
      this.numberOfNodes = numberOfNodes;
      this.replicationFactor = replicationFactor;
   }

   /**
    * Get the maximumChunkSizesInBytes.
    * 
    * @return the maximumChunkSizesInBytes.
    */
   public final long getMaximumChunkSizesInBytes() {
      return maximumChunkSizesInBytes;
   }

   /**
    * Get the numberOfNodes.
    * 
    * @return the numberOfNodes.
    */
   public final int getNumberOfNodes() {
      return numberOfNodes;
   }

   /**
    * Get the replicationFactor.
    * 
    * @return the replicationFactor.
    */
   public final int getReplicationFactor() {
      return replicationFactor;
   }
}
