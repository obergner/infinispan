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
package org.infinispan.commands.write;

import java.util.Collections;
import java.util.Set;

import org.infinispan.commands.Visitor;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * RemoveLargeObjectCommand.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class RemoveLargeObjectCommand extends AbstractDataWriteCommand {

   public static final byte COMMAND_ID = 22;

   private final Log log = LogFactory.getLog(getClass());

   private CacheNotifier notifier;

   private Object largeObjectKey;

   private boolean successful = true;

   public RemoveLargeObjectCommand() {
   }

   public RemoveLargeObjectCommand(Object key, CacheNotifier notifier, Set<Flag> flags) {
      super(key, flags);
      this.notifier = notifier;
   }

   public void init(CacheNotifier notifier) {
      this.notifier = notifier;
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return false;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitRemoveLargeObjectCommand(ctx, this);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      // FIXME: This is very likely insufficient
      return Boolean.TRUE;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void setParameters(int commandId, Object[] parameters) {
      if (commandId != getCommandId()) throw new IllegalStateException("Invalid method id");
      key = parameters[0];
      // TODO remove conditional check in future - eases migration for now
      flags = (Set<Flag>) (parameters.length > 1 ? parameters[1] : Collections.EMPTY_SET);
   }

   @Override
   public Object[] getParameters() {
      return new Object[] { key, flags };
   }

}
