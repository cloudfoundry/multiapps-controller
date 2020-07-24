package org.cloudfoundry.multiapps.controller.web.listeners;

import java.text.MessageFormat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.cloudfoundry.multiapps.controller.core.liquibase.RecoveringLockService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;

public class LiquibaseServicesRegistrator implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseServicesRegistrator.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LockService lockService = new RecoveringLockService();
        LockServiceFactory.getInstance()
                          .register(lockService);
        LOGGER.info(MessageFormat.format(Messages.REGISTERED_0_AS_LIQUIBASE_LOCK_SERVICE, lockService.getClass()
                                                                                                     .getName()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Nothing to do...
    }

}
