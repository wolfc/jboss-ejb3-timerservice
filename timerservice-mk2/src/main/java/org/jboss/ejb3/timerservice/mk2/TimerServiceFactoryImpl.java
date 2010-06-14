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

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;
import org.jboss.logging.Logger;

import javax.ejb.TimerService;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of {@link TimerServiceFactory}, responsible for 
 * creating and managing MK2 timer services
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceFactoryImpl implements TimerServiceFactory
{
   /**
    * Logger
    */
   private static final Logger logger = Logger.getLogger(TimerServiceFactoryImpl.class);

   /**
    * Entity manager factory for JPA backed persistence
    */
   private EntityManagerFactory emf;

   /**
    * Transaction manager for transaction management
    */
   private TransactionManager transactionManager;

   /**
    * Exceutor service for creating the scheduled timer tasks
    */
   private ScheduledExecutorService executor;

   /**
    * Creates a timer service for the passed <code>invoker</code>.
    * 
    * <p>
    *   This method also registers the created timer service, with the {@link TimerServiceRegistry}
    * </p>
    */
   public TimerService createTimerService(TimedObjectInvoker invoker)
   {
      assert emf != null : "emf is null";
      
      // TODO: inject
      executor = Executors.newScheduledThreadPool(10);

      // create the timer service
      TimerServiceImpl timerService = new TimerServiceImpl(invoker, emf.createEntityManager(), transactionManager,
            executor);
      // register this new created timer service in our registry
      TimerServiceRegistry.registerTimerService(timerService);
      return timerService;
   }

   /**
    * Restores the <code>timerService</code>.
    * 
    * <p>
    *   This involves restoring, any persisted, active timer instances
    * </p>
    * <p>
    *   This method additionally registers (if it is not already registered)
    *   the timer service with the {@link TimerServiceRegistry}
    * </p>
    * @see org.jboss.ejb3.timerservice.spi.TimerServiceFactory#restoreTimerService(javax.ejb.TimerService)
    */
   public void restoreTimerService(TimerService timerService)
   {
      TimerServiceImpl mk2TimerService = (TimerServiceImpl) timerService;
      String timedObjectId = mk2TimerService.getInvoker().getTimedObjectId();
      // if the timer service is not registered (maybe it was unregistered when it 
      // was suspended) then register it with the timer service registry
      if (TimerServiceRegistry.isRegistered(timedObjectId) == false)
      {
         TimerServiceRegistry.registerTimerService(mk2TimerService);
      }
      
      logger.debug("Restoring timerservice for timedObjectId: " + timedObjectId);
      // restore the timers
      mk2TimerService.restoreTimers();

   }

   /**
    * Set the entity manager factory responsible for managing persistence of the 
    * timers.
    * 
    * @param emf Entity manager factory
    */
   @PersistenceUnit(unitName = "timerdb")
   public void setEntityManagerFactory(EntityManagerFactory emf)
   {
      this.emf = emf;
   }

   /**
    * Sets the transaction mananger responsible for transaction management of timers
    * 
    * @param tm Transaction manager
    */
   @Inject
   public void setTransactionManager(TransactionManager tm)
   {
      this.transactionManager = tm;
   }

   /**
    * Suspends the <code>timerService</code>
    * 
    * <p>
    *   This involves suspending any scheduled timer tasks. Note that this method 
    *   does not <i>cancel</i> any timers. The timer will continue to stay active
    *   although their <i>currently scheduled tasks</i> will be cancelled. 
    * </p>
    * <p>
    *   A suspended timer service (and the associated) timers can be restored by invoking
    *   {@link #restoreTimerService(TimerService)}
    * </p>
    * <p>
    *   This method additionally unregisters the the timer service from the {@link TimerServiceRegistry}
    * </p>  
    * @see org.jboss.ejb3.timerservice.spi.TimerServiceFactory#suspendTimerService(javax.ejb.TimerService)
    */
   public void suspendTimerService(TimerService timerService)
   {
      TimerServiceImpl mk2TimerService = (TimerServiceImpl) timerService;
      try
      {
         logger.debug("Suspending timerservice for timedObjectId: " + mk2TimerService.getInvoker().getTimedObjectId());
         // suspend the timers
         mk2TimerService.suspendTimers();
      }
      finally
      {
         String timedObjectId = mk2TimerService.getInvoker().getTimedObjectId(); 
         // remove from our registry too
         if(TimerServiceRegistry.isRegistered(timedObjectId))
         {
            TimerServiceRegistry.unregisterTimerService(timedObjectId);
         }
      }
   }

}
