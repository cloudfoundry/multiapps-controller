package com.sap.cloud.lm.sl.cf.web.listeners;

import java.text.MessageFormat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.liquibase.RecoveringLockService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;

public class LiquibaseServicesRegistrator implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseServicesRegistrator.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        LockService lockService = createLockService(configuration);
        LockServiceFactory.getInstance()
            .register(lockService);
        LOGGER.info(MessageFormat.format(Messages.REGISTERED_0_AS_LIQUIBASE_LOCK_SERVICE, lockService.getClass()
            .getName()));
    }

    private LockService createLockService(ApplicationConfiguration configuration) {
        long changeLogLockAttempts = configuration.getChangeLogLockAttempts();
        long changeLogLockDuration = configuration.getChangeLogLockDuration();
        long changeLogLockPollRate = configuration.getChangeLogLockPollRate();
        return new RecoveringLockService(changeLogLockAttempts, changeLogLockDuration, changeLogLockPollRate);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Nothing to do...
    }

}
