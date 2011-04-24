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

import org.infinispan.commands.write.PutKeyLargeObjectCommand;
import org.infinispan.commands.write.RemoveLargeObjectCommand;
import org.infinispan.config.Configuration;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.largeobjectsupport.Chunk;
import org.infinispan.largeobjectsupport.ChunkMetadata;
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
public class LargeObjectSupportInterceptor extends CommandInterceptor {

   private Configuration configuration;

   private LargeObjectMetadataManager largeObjectMetadataManager;

   @Inject
   public void init(Configuration configuration,
            LargeObjectMetadataManager largeObjectMetadataManager) {
      this.configuration = configuration;
      this.largeObjectMetadataManager = largeObjectMetadataManager;
   }

   @Override
   public Object visitPutKeyLargeObjectCommand(InvocationContext ctx,
            PutKeyLargeObjectCommand command) throws Throwable {
      checkCommandValid(ctx, command);
      // We are dealing we a large object's chunk, i.e. the large object to be stored has already
      // been partitioned into chunks
      if (command.getValue() instanceof Chunk) return invokeNextInterceptor(ctx, command);

      deletePreviousLargeObjectIfNecessary(ctx, command);

      deletePreviousLargeObjectIfNecessary(ctx, command);

      LargeObjectMetadata largeObjectMetadata = chunkAndStoreEachChunk(ctx, command);
      largeObjectMetadataManager.storeLargeObjectMetadata(largeObjectMetadata);

      // We don't need a return value
      return null;
   }

   private void checkCommandValid(InvocationContext ctx, PutKeyLargeObjectCommand command)
            throws IllegalStateException {
      if (ctx.isInTxScope())
         throw new IllegalStateException(
                  "Storing Large Objects in a transactional context is not (yet) supported.");
      if (!(command.getValue() instanceof InputStream) && !(command.getValue() instanceof Chunk))
         throw new IllegalStateException("Value [" + command.getValue()
                  + "] to be stored is neither an InputStream nor a Chunk");
   }

   private void deletePreviousLargeObjectIfNecessary(InvocationContext ctx,
            PutKeyLargeObjectCommand command) throws Throwable {
      if (!largeObjectMetadataManager.alreadyUsedByLargeObject(command.getKey())) return;

      // FIXME: This should be delegated to CommandsFactory
      RemoveLargeObjectCommand removeCommand = new RemoveLargeObjectCommand(command.getKey(), null,
               command.getFlags());
      removeEachChunk(ctx, removeCommand);
   }

   private LargeObjectMetadata chunkAndStoreEachChunk(InvocationContext ctx,
            PutKeyLargeObjectCommand command) throws InterruptedException, Throwable {
      InputStream largeObject = InputStream.class.cast(command.getValue());
      Chunks chunks = new Chunks(command.getKey(), largeObject,
               configuration.getMaximumChunkSizeInBytes(),
               largeObjectMetadataManager.chunkKeyGenerator());

      /*
       * We need to (1) remember the key-largeObject-pair currently stored in command, (2) replace
       * that pair with the chunkKey-chunk-pair to be stored and (3) restore the former
       * key-LargeObject-pair in command once the chunk has been finished.
       */
      // FIXME: There has to be a better solution
      Object rememberedLargeObjectKey = command.getKey();
      Object rememberedLargeObject = command.getValue();
      for (Chunk chunk : chunks) {
         command.setKey(chunk.getChunkKey());
         command.setValue(chunk);

         invokeNextInterceptor(ctx, command);
      }
      command.setKey(rememberedLargeObjectKey);
      command.setValue(rememberedLargeObject);

      return chunks.largeObjectMetadata();
   }

   @Override
   public Object visitRemoveLargeObjectCommand(InvocationContext ctx,
            RemoveLargeObjectCommand command) throws Throwable {
      checkCommandValid(ctx, command);
      if (!largeObjectMetadataManager.alreadyUsedByLargeObject(command.getKey())) return false;

      removeEachChunk(ctx, command);

      return true;
   }

   /**
    * FIXME Comment this
    * 
    * @param ctx
    * @param command
    * @throws Throwable
    */
   private void removeEachChunk(InvocationContext ctx, RemoveLargeObjectCommand command)
            throws Throwable {
      LargeObjectMetadata metadata = largeObjectMetadataManager
               .correspondingLargeObjectMetadata(command.getKey());

      Object rememberedKey = command.getKey();
      for (ChunkMetadata chunkMetadata : metadata) {
         command.setKey(chunkMetadata.getKey());

         invokeNextInterceptor(ctx, command);
      }
      command.setKey(rememberedKey);

      largeObjectMetadataManager.removeLargeObjectMetadata(metadata);
   }

   private void checkCommandValid(InvocationContext ctx, RemoveLargeObjectCommand command)
            throws IllegalStateException {
      if (ctx.isInTxScope())
         throw new IllegalStateException(
                  "Storing Large Objects in a transactional context is not (yet) supported.");
   }
}
