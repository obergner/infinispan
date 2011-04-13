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

import java.io.InputStream;
import java.util.Set;

import org.infinispan.commands.Visitor;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.notifications.cachelistener.CacheNotifier;

/**
 * <p>
 * Command object corresponding to
 * {@link org.infinispan.AdvancedCache#writeToKey(Object, java.io.InputStream)
 * <code>writeToKey(Object, java.io.InputStream)</code>}.
 * </p>
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class PutKeyLargeObjectCommand extends PutKeyValueCommand {

   public static final byte COMMAND_ID = 21;

   /**
    * Create a new PutKeyLargeObjectCommand.
    * 
    */
   public PutKeyLargeObjectCommand() {
      // Noop
   }

   /**
    * Create a new PutKeyLargeObjectCommand.
    * 
    * @param key
    * @param largeObject
    * @param putIfAbsent
    * @param notifier
    * @param lifespanMillis
    * @param maxIdleTimeMillis
    * @param flags
    */
   public PutKeyLargeObjectCommand(Object key, InputStream largeObject, boolean putIfAbsent,
            CacheNotifier notifier, long lifespanMillis, long maxIdleTimeMillis, Set<Flag> flags) {
      // FIXME PutKeyLargeObjectCommand constructor
      super(key, largeObject, putIfAbsent, notifier, lifespanMillis, maxIdleTimeMillis, flags);
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      if (!(value instanceof byte[]))
         throw new IllegalStateException(
                  "This PutKeyValueCommand ["
                           + this
                           + "] is configured as a command handling a large object. However, the supplied value ["
                           + value + "] is not a byte array/chunk");
      MVCCEntry e = (MVCCEntry) ctx.lookupEntry(key);
      e.setValue(value);
      if (e.isRemoved()) {
         e.setRemoved(false);
         e.setValid(true);
      }
      // We write a chunk of a large object. We don't need to return
      // anything.
      return null;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPutKeyLargeObjectCommand(ctx, this);
   }
   
   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      PutKeyValueCommand that = (PutKeyValueCommand) o;

      if (lifespanMillis != that.lifespanMillis) return false;
      if (maxIdleTimeMillis != that.maxIdleTimeMillis) return false;
      if (putIfAbsent != that.putIfAbsent) return false;
      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (putIfAbsent ? 1 : 0);
      result = 31 * result + (int) (lifespanMillis ^ (lifespanMillis >>> 32));
      result = 31 * result + (int) (maxIdleTimeMillis ^ (maxIdleTimeMillis >>> 32));
      return result;
   }
   
   @Override
   public String toString() {
      return new StringBuilder()
            .append("PutKeyLargeObjectCommand{key=")
            .append(key)
            .append(", value=").append(value)
            .append(", flags=").append(flags)
            .append(", putIfAbsent=").append(putIfAbsent)
            .append(", lifespanMillis=").append(lifespanMillis)
            .append(", maxIdleTimeMillis=").append(maxIdleTimeMillis)
            .append("}")
            .toString();
   }
}
