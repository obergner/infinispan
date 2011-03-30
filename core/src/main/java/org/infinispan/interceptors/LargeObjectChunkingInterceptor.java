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
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;

import java.io.InputStream;

/**
 * LargeObjectChunkingInterceptor.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @see <a href="https://community.jboss.org/wiki/LargeObjectSupport">Large Object Support</a>
 * 
 * @since 5.1
 */
public class LargeObjectChunkingInterceptor extends CommandInterceptor {

   private final long maxChunkSizeInBytes = 1000000L;

   @Override
   public Object visitWriteLargeObjectToKeyCommand(InvocationContext ctx,
            WriteLargeObjectToKeyCommand command) throws Throwable {
      if (ctx.isInTxScope())
         throw new IllegalStateException(
                  "Storing Large Objects in a transactional context is not (yet) supported.");

      InputStream largeObject = command.getLargeObject();
      CacheEntry rememberedCacheEntry = ctx.lookupEntry(command.getKey());
      // We don't need a return value
      return null;
   }

}
