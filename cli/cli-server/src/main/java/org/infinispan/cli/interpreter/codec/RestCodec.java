/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.infinispan.cli.interpreter.codec;

import org.infinispan.cli.interpreter.logging.Log;
import org.infinispan.remoting.MIMECacheEntry;
import org.infinispan.util.logging.LogFactory;

/**
 * RestCodec.
 *
 * @author Tristan Tarrant
 * @since 5.2
 */
public class RestCodec implements Codec {
   public static final Log log = LogFactory.getLog(RestCodec.class, Log.class);

   @Override
   public String getName() {
      return "rest";
   }

   @Override
   public Object encodeKey(Object key) throws CodecException {
      return key;
   }

   @Override
   public Object encodeValue(Object value) throws CodecException {
      if (value != null) {
         if (value instanceof MIMECacheEntry) {
            return value;
         } else if (value instanceof String) {
            return new MIMECacheEntry("text/plain", ((String)value).getBytes());
         } else if (value instanceof byte[]) {
            return new MIMECacheEntry("application/binary", (byte[])value);
         } else {
            throw log.valueEncodingFailed(value.getClass().getName(), this.getName());
         }
      } else {
         return null;
      }
   }

   @Override
   public Object decodeKey(Object key) throws CodecException {
      return key;
   }

   @Override
   public Object decodeValue(Object value) throws CodecException {
      if (value != null) {
         MIMECacheEntry e = (MIMECacheEntry)value;
         if (e.contentType.startsWith("text/") || e.contentType.equals("application/xml") || e.contentType.equals("application/json")) {
            return new String(e.data);
         } else {
            return e.data;
         }
      } else {
         return null;
      }
   }

}
