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

import org.infinispan.CacheException;

/**
 * Abstract base class for all <code>Exceptions</code> signaling an error when processing a
 * <code>LargeObjec</code>.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public abstract class LargeObjectSupportException extends CacheException {

   /** The serialVersionUID */
   private static final long serialVersionUID = -1580536765480016646L;

   /**
    * Create a new LargeObjectSupportException.
    * 
    */
   public LargeObjectSupportException() {
   }

   /**
    * Create a new LargeObjectSupportException.
    * 
    * @param cause
    */
   public LargeObjectSupportException(Throwable cause) {
      super(cause);
   }

   /**
    * Create a new LargeObjectSupportException.
    * 
    * @param msg
    */
   public LargeObjectSupportException(String msg) {
      super(msg);
   }

   /**
    * Create a new LargeObjectSupportException.
    * 
    * @param msg
    * @param cause
    */
   public LargeObjectSupportException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
