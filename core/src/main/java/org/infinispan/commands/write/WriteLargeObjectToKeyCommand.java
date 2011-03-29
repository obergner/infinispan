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

import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commands.Visitor;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.notifications.cachelistener.CacheNotifier;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * <p>
 * Implements {@link org.infinispan.AdvancedCache#writeToKey(Object, java.io.InputStream)
 * <code>AdvancedCache#writeToKey(Object, java.io.InputStream)</code>}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public final class WriteLargeObjectToKeyCommand extends AbstractDataWriteCommand {

   public static final byte COMMAND_ID = 12;

   private InputStream largeObject;

   private CacheNotifier cacheNotifier;

   public WriteLargeObjectToKeyCommand(Object key, InputStream largeObject,
            CacheNotifier cacheNotifier, Set<Flag> flags) {
      super(key, flags);
      this.largeObject = largeObject;
      this.cacheNotifier = cacheNotifier;
   }

   public void init(CacheNotifier cacheNotifier) {
      this.cacheNotifier = cacheNotifier;
   }

   /**
    * Get the largeObject.
    * 
    * @return the largeObject.
    */
   public InputStream getLargeObject() {
      return largeObject;
   }

   @Override
   public boolean isSuccessful() {
      return true;
   }

   @Override
   public boolean isConditional() {
      return false;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitWriteLargeObjectToKeyCommand(ctx, this);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      MVCCEntry e = (MVCCEntry) ctx.lookupEntry(key);
      e.setValue(largeObject);
      if (e.isRemoved()) {
         e.setRemoved(false);
         e.setValid(true);
      }
      // Writing a large object does not return the large object formerly stored under the same key
      return null;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void setParameters(int commandId, Object[] parameters) {
      if (commandId != COMMAND_ID)
         throw new IllegalArgumentException("Invalid commandId [" + commandId + "]");
      key = parameters[0];
      largeObject = (InputStream) parameters[1];
      // TODO remove conditional check in future - eases migration for now
      flags = (Set<Flag>) (parameters.length > 2 ? parameters[2] : Collections.emptySet());
   }

   @Override
   public Object[] getParameters() {
      return new Object[] { key, largeObject, flags };
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      if (!super.equals(o))
         return false;

      WriteLargeObjectToKeyCommand that = (WriteLargeObjectToKeyCommand) o;

      if (largeObject != null ? !largeObject.equals(that.largeObject) : that.largeObject != null)
         return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (largeObject != null ? largeObject.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return new StringBuilder().append("WriteLargeObjectToKeyCommand{key=").append(key)
               .append(", largeObject=").append(largeObject).append(", flags=").append(flags)
               .append("}").toString();
   }
}
