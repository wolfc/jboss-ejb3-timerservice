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
package org.jboss.ejb3.timerservice.mk2;

import java.util.UUID;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerHandleImpl implements TimerHandle
{
   private static final long serialVersionUID = 1L;

   /**
    * Id of the target {@link TimedObjectInvoker}
    */
   private String timedObjectId;
   
   /**
    * Each {@link TimedObjectInvoker} can have multiple timer instances.
    * This id corresponds to one such <i>instance</i> 
    */
   private UUID id;
   
   /**
    * The {@link TimerServiceImpl} to which this timer handle belongs to  
    */
   private transient TimerServiceImpl service;
   
   /**
    * For serialization only 
    */
   public TimerHandleImpl()
   {
      
   }
   
   /**
    * Creates a {@link TimerHandleImpl}
    * 
    * @param id The id of the timer instance
    * @param timedObjectId The id of the target {@link TimedObjectInvoker} 
    * @param service The timer service to which this timer handle belongs to
    * @throws IllegalArgumentException If either of the passed parameters is null
    */
   public TimerHandleImpl(UUID id, String timedObjectId, TimerServiceImpl service) throws IllegalArgumentException
   {
      if (id == null)
      {
         throw new IllegalArgumentException("Id cannot be null");
      }
      if (timedObjectId == null)
      {
         throw new IllegalArgumentException("Timed objectid cannot be null");
      }
      if (service == null)
      {
         throw new IllegalArgumentException("Timer service cannot be null");
      }

      this.timedObjectId = timedObjectId;
      this.id = id;
      this.service = service;
   }
   
   /**
    * Returns the {@link Timer} corresponding to this timer handle
    * 
    * {@inheritDoc}
    */
   public Timer getTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      if(service == null)
      {
         // get hold of the timer service through the use of timed object id
         service = TimerServiceRegistry.getTimerService(this.timedObjectId);
         if (service == null)
         {
            throw new EJBException("Timerservice with timedObjectId: " + timedObjectId + " is not registered");
         }
      }
      org.jboss.ejb3.timerservice.extension.Timer timer = this.service.getTimer(this);
      if (timer != null && timer.isActive() == false)
      {
         throw new NoSuchObjectLocalException("Timer for handle: " + this + " is not active");
      }
      return timer;
   }
   
   public UUID getId()
   {
      return this.id;
   }
   
   public String getTimedObjectId()
   {
      return this.timedObjectId;
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (obj instanceof TimerHandleImpl == false)
      {
         return false;
      }
      TimerHandleImpl other = (TimerHandleImpl) obj;
      if (this == other)
      {
         return true;
      }
      if(this.id.equals(other.id) && this.timedObjectId.equals(other.timedObjectId))
      {
         return true;
      }
      return false;
   }
   
   @Override
   public int hashCode()
   {
      return this.id.hashCode();
   }
   
}
