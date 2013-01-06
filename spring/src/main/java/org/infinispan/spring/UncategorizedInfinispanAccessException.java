/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.infinispan.spring;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * A concrete subclass of Spring's abstract {@link UncategorizedDataAccessException}. This exception
 * exists solely to have a class derived from Spring's
 * {@link org.springframework.dao.DataAccessException} to map Infinispan's generic
 * {@link org.infinispan.CacheException} to.
 * 
 * @author obergner
 * @since 6.0.0
 */
public class UncategorizedInfinispanAccessException extends UncategorizedDataAccessException {

   /** The serialVersionUID */
   private static final long serialVersionUID = 3636283829070539701L;

   /**
    * Create a new UncategorizedInfinispanAccessException.
    * 
    * @param msg
    * @param cause
    */
   public UncategorizedInfinispanAccessException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
