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

import org.infinispan.commands.write.WriteLargeObjectToKeyCommand;
import org.infinispan.config.Configuration;
import org.infinispan.container.EntryFactory;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.Chunk;
import org.infinispan.largeobjectsupport.Chunks;
import org.infinispan.manager.EmbeddedCacheManager;

import java.io.InputStream;

/**
 * <p>
 * The {@code LargeObjectChunkingInterceptor} is responsible for
 * <ol>
 * <li>
 * dividing a <em>Large Object</em> into {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk
 * </code>}s, and</li>
 * <li>
 * storing each of those {@link org.infinispan.largeobjectsupport.Chunk <code>Chunk</code>}s under
 * its own <code>chunk key</code>.</li>
 * </ol>
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @see <a href="https://community.jboss.org/wiki/LargeObjectSupport">Large Object Support</a>
 * 
 * @since 5.1
 */
public class LargeObjectChunkingInterceptor<K> extends CommandInterceptor {

   // TODO: Make this configurable
   private long maxChunkSizeInBytes = 1000000L;

   private Configuration configuration;

   private DistributionManager distributionManager;

   private EmbeddedCacheManager embeddedCacheManager;

   private EntryFactory entryFactory;

   @Inject
   public void init(Configuration configuration, DistributionManager distributionManager,
            EmbeddedCacheManager embeddedCacheManager, EntryFactory entryFactory,
            long maxChunkSizeInBytes) {
      this.configuration = configuration;
      this.distributionManager = distributionManager;
      this.embeddedCacheManager = embeddedCacheManager;
      this.entryFactory = entryFactory;
      this.maxChunkSizeInBytes = maxChunkSizeInBytes;
   }

   @Override
   public Object visitWriteLargeObjectToKeyCommand(InvocationContext ctx,
            WriteLargeObjectToKeyCommand command) throws Throwable {
      if (ctx.isInTxScope())
         throw new IllegalStateException(
                  "Storing Large Objects in a transactional context is not (yet) supported.");

      CacheEntry rememberedEntry = ctx.lookupEntry(command.getKey());
      ctx.clearLookedUpEntries();

      InputStream largeObject = command.getLargeObject();
      Chunks<K> chunks = new Chunks<K>((K) command.getKey(), largeObject, maxChunkSizeInBytes,
               distributionManager, configuration, embeddedCacheManager);
      for (Chunk chunk : chunks) {
         final CacheEntry chunkCacheEntry = entryFactory.wrapEntryForWriting(ctx,
                  chunk.getChunkKey(), false, false, false, false, false);
         chunkCacheEntry.setValue(chunk.getData());
         ctx.putLookedUpEntry(chunk.getChunkKey(), chunkCacheEntry);

         invokeNextInterceptor(ctx, command);

         ctx.clearLookedUpEntries(); // We would otherwise risk getting OOM
      }

      // Restore former CacheEntry
      ctx.putLookedUpEntry(command.getKey(), rememberedEntry);
      // We don't need a return value
      return null;
   }
}
