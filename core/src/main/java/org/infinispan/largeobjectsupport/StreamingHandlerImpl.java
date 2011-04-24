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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.Cache;
import org.infinispan.StreamingHandler;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.write.PutKeyLargeObjectCommand;
import org.infinispan.commands.write.RemoveLargeObjectCommand;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.context.PreInvocationContext;
import org.infinispan.interceptors.InterceptorChain;

/**
 * StreamingHandlerImpl.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class StreamingHandlerImpl<K> implements StreamingHandler<K> {

   private final InterceptorChain invoker;

   private final CommandsFactory commandsFactory;

   private final LargeObjectMetadataManager largeObjectMetadataManager;

   private final long defaultLifespan;

   private final long defaultMaxIdleTime;

   private final InvocationContextContainer invocationContextContainer;

   private final Configuration configuration;

   private final Cache<K, ?> backingCache;

   private final ThreadLocal<PreInvocationContext> flagHolder = new ThreadLocal<PreInvocationContext>() ;

   public StreamingHandlerImpl(InterceptorChain invoker, CommandsFactory commandsFactory,
            LargeObjectMetadataManager largeObjectMetadataManager, long defaultLifespan,
            long defaultMaxIdleTime, InvocationContextContainer invocationContextContainer,
            Configuration configuration, Cache<K, ?> backingCache) {
      this.invoker = invoker;
      this.commandsFactory = commandsFactory;
      this.largeObjectMetadataManager = largeObjectMetadataManager;
      this.defaultLifespan = defaultLifespan;
      this.defaultMaxIdleTime = defaultMaxIdleTime;
      this.invocationContextContainer = invocationContextContainer;
      this.configuration = configuration;
      this.backingCache = backingCache;
   }

   @Override
   public void writeToKey(K key, InputStream largeObject) {
      assertKeyNotNull(key);
      InvocationContext ctx = getInvocationContext(false);
      PutKeyLargeObjectCommand command = commandsFactory.buildPutKeyLargeObjectCommand(key,
               largeObject, MILLISECONDS.toMillis(defaultLifespan),
               MILLISECONDS.toMillis(defaultMaxIdleTime), ctx.getFlags());
      invoker.invoke(ctx, command);
   }

   @Override
   public OutputStream writeToKey(K key) {
      assertKeyNotNull(key);
      return new LargeObjectOutputStream(key, (Cache<Object, Object>) backingCache,
               largeObjectMetadataManager.chunkKeyGenerator(),
               configuration.getMaximumChunkSizeInBytes(), largeObjectMetadataManager);
   }

   @Override
   public InputStream readFromKey(K key) {
      assertKeyNotNull(key);
      LargeObjectMetadata largeObjectMetadata = largeObjectMetadataManager
               .correspondingLargeObjectMetadata(key);
      if (largeObjectMetadata == null) return null;
      return new LargeObjectInputStream(largeObjectMetadata, backingCache);
   }

   private void assertKeyNotNull(Object key) {
      if (key == null) {
         throw new NullPointerException("Null keys are not supported!");
      }
   }

   private InvocationContext getInvocationContext(boolean forceNonTransactional) {
      InvocationContext ctx = forceNonTransactional ? invocationContextContainer
               .createNonTxInvocationContext() : invocationContextContainer
               .createInvocationContext();
      return withInvocationContextFlags(ctx);
   }

   private InvocationContext withInvocationContextFlags(InvocationContext ctx) {
      PreInvocationContext pic = flagHolder.get();
      if (pic != null && !pic.getFlags().isEmpty()) {
         ctx.setFlags(pic.getFlags());
      }
      flagHolder.remove();
      return ctx;
   }

   @Override
   public boolean removeKey(K key) {
      assertKeyNotNull(key);
      InvocationContext ctx = getInvocationContext(false);
      RemoveLargeObjectCommand command = commandsFactory.buildRemoveLargeObjectCommand(key,
               ctx.getFlags());
      return Boolean.class.cast(invoker.invoke(ctx, command));
   }

   @Override
   public StreamingHandler<K> withFlags(Flag... flags) {
      if (flags != null && flags.length > 0) {
         PreInvocationContext pic = flagHolder.get();
         // we will also have a valid PIC value because of initialValue()
         pic.add(flags);
      }
      return this;
   }

}
