package com.sap.cloud.lm.sl.cf.web.init;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;

public class CustomApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getEnvironment().addActiveProfile(ConfigurationUtil.getPlatformType().toString().toLowerCase());
        applicationContext.getEnvironment().addActiveProfile(ConfigurationUtil.getDatabaseType().toString().toLowerCase());
    }
}
