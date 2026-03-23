package org.cloudfoundry.multiapps.controller.web.bootstrap;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class StrictCircularReferencesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.setAllowCircularReferences(false);
    }

}

