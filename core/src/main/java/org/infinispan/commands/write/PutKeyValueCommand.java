/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
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
import java.util.Collections;
import java.util.Set;

import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commands.Visitor;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.notifications.cachelistener.CacheNotifier;

/**
 * Implements functionality defined by {@link org.infinispan.Cache#put(Object, Object)}
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class PutKeyValueCommand extends AbstractDataWriteCommand {
   public static final byte COMMAND_ID = 8;

   Object value;
   boolean putIfAbsent;
   boolean putLargeObject;
   CacheNotifier notifier;
   boolean successful = true;
   long lifespanMillis = -1;
   long maxIdleTimeMillis = -1;

   public PutKeyValueCommand() {
   }

   public PutKeyValueCommand(Object key, Object value, boolean putIfAbsent, boolean putLargeObject, CacheNotifier notifier, long lifespanMillis, long maxIdleTimeMillis, Set<Flag> flags) {
      super(key, flags);
      this.value = value;
      this.putIfAbsent = putIfAbsent;
      this.notifier = notifier;
      this.lifespanMillis = lifespanMillis;
      this.maxIdleTimeMillis = maxIdleTimeMillis;
      this.putLargeObject = putLargeObject;
   }

   public void init(CacheNotifier notifier) {
      this.notifier = notifier;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = value;
   }

   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPutKeyValueCommand(ctx, this);
   }

   public Object perform(InvocationContext ctx) throws Throwable {
      Object o;
      if (!isPutLargeObject())
         o = handleRegularInvocation(ctx);
      else
         o = handleLargeObjectInvocation(ctx);
      return o;
   }

   /**
    * Handles the case where the value to be stored is a regular object, i.e. not a large object.
    * 
    * @param ctx
    * @return
    */
   private Object handleRegularInvocation(InvocationContext ctx) {
      Object o;
      MVCCEntry e = (MVCCEntry) ctx.lookupEntry(key);
      Object entryValue = e.getValue();
      if (entryValue != null && putIfAbsent && !e.isRemoved()) {
         successful = false;
         o = entryValue;
      } else {
         notifier.notifyCacheEntryModified(key, entryValue, true, ctx);

         if (value instanceof Delta) {
            // magic
            Delta dv = (Delta) value;
            DeltaAware toMergeWith = null;
            if (entryValue instanceof DeltaAware) toMergeWith = (DeltaAware) entryValue;
            e.setValue(dv.merge(toMergeWith));
            o = entryValue;
            e.setLifespan(lifespanMillis);
            e.setMaxIdle(maxIdleTimeMillis);
         } else {
            o = e.setValue(value);
            if (e.isRemoved()) {
               e.setRemoved(false);
               e.setValid(true);
               o = null;
            }
            e.setLifespan(lifespanMillis);
            e.setMaxIdle(maxIdleTimeMillis);
         }
         notifier.notifyCacheEntryModified(key, e.getValue(), false, ctx);
      }
      return o;
   }
   
   /**
    * Handles the case where the value to be stored is a large object.
    * 
    * @param ctx
    * @return
    */
   private Object handleLargeObjectInvocation(InvocationContext ctx) {
      if (!(value instanceof InputStream))
         throw new IllegalStateException(
                  "This PutKeyValueCommand ["
                           + this
                           + "] is configured as a command handling a large object. However, the supplied value ["
                           + value + "] is not an InputStream");
      MVCCEntry e = (MVCCEntry) ctx.lookupEntry(key);
      e.setValue(value);
      if (e.isRemoved()) {
         e.setRemoved(false);
         e.setValid(true);
      }
      // Writing a large object does not return the large object formerly stored under the same key
      return null;
   }

   public byte getCommandId() {
      return COMMAND_ID;
   }

   public Object[] getParameters() {
      return new Object[]{key, value, lifespanMillis, maxIdleTimeMillis, flags};
   }

   public void setParameters(int commandId, Object[] parameters) {
      if (commandId != COMMAND_ID) throw new IllegalStateException("Invalid method id");
      key = parameters[0];
      value = parameters[1];
      lifespanMillis = (Long) parameters[2];
      maxIdleTimeMillis = (Long) parameters[3];
      flags = (Set<Flag>) (parameters.length>4 ? parameters[4] : Collections.EMPTY_SET); //TODO remove conditional check in future - eases migration for now
   }

   public boolean isPutIfAbsent() {
      return putIfAbsent;
   }

   public void setPutIfAbsent(boolean putIfAbsent) {
      this.putIfAbsent = putIfAbsent;
   }
   
   /**
    * Is this command handling a <em>Large Object</em>?
    * 
    * @return the putLargeObject.
    */
   public boolean isPutLargeObject() {
      return putLargeObject;
   }

   /**
    * Set whether this command is handling a <em>Large Object</em>.
    * 
    * @param putLargeObject The putLargeObject to set.
    */
   public void setPutLargeObject(boolean putLargeObject) {
      this.putLargeObject = putLargeObject;
   }

   public long getLifespanMillis() {
      return lifespanMillis;
   }

   public long getMaxIdleTimeMillis() {
      return maxIdleTimeMillis;
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
      if (putLargeObject != that.putLargeObject) return false;
      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (putIfAbsent ? 1 : 0);
      result = 31 * result + (putLargeObject ? 1 : 0);
      result = 31 * result + (int) (lifespanMillis ^ (lifespanMillis >>> 32));
      result = 31 * result + (int) (maxIdleTimeMillis ^ (maxIdleTimeMillis >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return new StringBuilder()
            .append("PutKeyValueCommand{key=")
            .append(key)
            .append(", value=").append(value)
            .append(", flags=").append(flags)
            .append(", putIfAbsent=").append(putIfAbsent)
            .append(", putLargeObject=").append(putLargeObject)
            .append(", lifespanMillis=").append(lifespanMillis)
            .append(", maxIdleTimeMillis=").append(maxIdleTimeMillis)
            .append("}")
            .toString();
   }

   public boolean isSuccessful() {
      return successful;
   }

   public boolean isConditional() {
      return putIfAbsent;
   }
}