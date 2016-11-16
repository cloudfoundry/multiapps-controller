package com.sap.cloud.lm.sl.cf.web.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.sap.cloud.lm.sl.cf.core.liquibase.RecoveringLockService;

import liquibase.lockservice.LockServiceFactory;

public class LiquibaseServicesRegistrator implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LockServiceFactory.getInstance().register(new RecoveringLockService());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Nothing to do...
    }

}
