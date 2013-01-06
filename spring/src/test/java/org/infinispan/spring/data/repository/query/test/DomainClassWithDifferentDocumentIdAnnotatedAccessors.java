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
package org.infinispan.spring.data.repository.query.test;

import org.hibernate.search.annotations.DocumentId;

/**
 * DomainClassWithDifferentDocumentIdAnnotatedAccessors.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 6.0.0
 */
public class DomainClassWithDifferentDocumentIdAnnotatedAccessors {

   private String firstId;

   private String secondId;

   @DocumentId
   public final String getFirstId() {
      return firstId;
   }

   public final void setFirstId(final String firstId) {
      this.firstId = firstId;
   }

   public final String getSecondId() {
      return secondId;
   }

   @DocumentId
   public final void setSecondId(final String secondId) {
      this.secondId = secondId;
   }
}
