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
package org.infinispan.interceptors;

import java.io.InputStream;

import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.config.Configuration;
import org.infinispan.container.EntryFactory;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.Chunk;
import org.infinispan.largeobjectsupport.Chunks;
import org.infinispan.largeobjectsupport.LargeObjectMetadata;
import org.infinispan.largeobjectsupport.LargeObjectMetadataManager;

/**
 * <p>
 * The {@code LargeObjectSupportInterceptor} is responsible for
 * <ol>
 * <li>
 * dividing a <em>Large Object</em> into {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk
 * </code>}s,</li>
 * <li>
 * generating a <code>chunk key</code> for each of those
 * {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk</code>}s,</li>
 * <li>
 * storing each of those {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk</code>}s under
 * its own <code>chunk key</code>, and</li>
 * <li>
 * mapping the <code>large object key</code> to the corresponding
 * {@link org.infinispan.largeobjectsupport.LargeObjectMetadata <code>LargeObjectMetadata</code>}
 * through storing it in the {@link LargeObjectMetadataManager <code>LargeObjectMetadataManager
 * </code>}.</li>
 * </ol>
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @see <a href="https://community.jboss.org/wiki/LargeObjectSupport">Large Object Support</a>
 * 
 * @since 5.1
 */
public class LargeObjectSupportInterceptor<K> extends CommandInterceptor {

   private Configuration configuration;

   private DistributionManager distributionManager;

   private EntryFactory entryFactory;

   private LargeObjectMetadataManager largeObjectMetadataManager;

   @Inject
   public void init(Configuration configuration, DistributionManager distributionManager,
            EntryFactory entryFactory, LargeObjectMetadataManager largeObjectMetadataManager) {
      this.configuration = configuration;
      this.distributionManager = distributionManager;
      this.entryFactory = entryFactory;
      this.largeObjectMetadataManager = largeObjectMetadataManager;
   }

   @Override
   public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command)
            throws Throwable {
      if (!command.isPutLargeObject())
         return invokeNextInterceptor(ctx, command);

      checkCommandValid(ctx, command);

      CacheEntry rememberedEntry = ctx.lookupEntry(command.getKey());
      ctx.clearLookedUpEntries();

      LargeObjectMetadata<K> largeObjectMetadata = chunkAndStoreEachChunk(ctx, command);
      largeObjectMetadataManager.storeLargeObjectMetadata(largeObjectMetadata);

      // Restore former CacheEntry
      ctx.putLookedUpEntry(command.getKey(), rememberedEntry);
      // We don't need a return value
      return null;
   }

   private void checkCommandValid(InvocationContext ctx, PutKeyValueCommand command)
            throws IllegalStateException {
      if (ctx.isInTxScope())
         throw new IllegalStateException(
                  "Storing Large Objects in a transactional context is not (yet) supported.");
      if (!(command.getValue() instanceof InputStream))
         throw new IllegalStateException("Value [" + command.getValue()
                  + "] to be stored is not an InputStream");
      // TODO: Remove as soon as we do support updates
      if (largeObjectMetadataManager.alreadyUsedByLargeObject(command.getKey())) {
         throw new UnsupportedOperationException("Key [" + command.getKey()
                  + "] is already in use. Updating a large object is not yet supported.");
      }
   }

   private LargeObjectMetadata<K> chunkAndStoreEachChunk(InvocationContext ctx,
            PutKeyValueCommand command) throws InterruptedException, Throwable {
      InputStream largeObject = InputStream.class.cast(command.getValue());
      Chunks<K> chunks = new Chunks<K>((K) command.getKey(), largeObject, distributionManager,
               configuration);
      for (Chunk chunk : chunks) {
         final CacheEntry chunkCacheEntry = entryFactory.wrapEntryForWriting(ctx,
                  chunk.getChunkKey(), false, false, false, false, false);
         chunkCacheEntry.setValue(chunk.getData());
         ctx.putLookedUpEntry(chunk.getChunkKey(), chunkCacheEntry);

         invokeNextInterceptor(ctx, command);

         ctx.clearLookedUpEntries(); // We would otherwise risk getting OOM
      }

      return chunks.largeObjectMetadata();
   }
}
