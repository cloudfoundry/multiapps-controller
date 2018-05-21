package com.sap.cloud.lm.sl.cf.web.init;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class CustomApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        applicationContext.getEnvironment()
            .addActiveProfile(configuration.getPlatformType()
                .toString()
                .toLowerCase());
        applicationContext.getEnvironment()
            .addActiveProfile(configuration.getDatabaseType()
                .toString()
                .toLowerCase());
    }
}
