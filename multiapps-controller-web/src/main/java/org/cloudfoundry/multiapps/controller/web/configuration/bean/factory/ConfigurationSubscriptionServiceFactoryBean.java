package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService.ConfigurationSubscriptionMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

@Named("configurationSubscriptionService")
public class ConfigurationSubscriptionServiceFactoryBean implements FactoryBean<ConfigurationSubscriptionService>, InitializingBean {

    @Inject
    protected EntityManagerFactory entityManagerFactory;
    @Inject
    @Qualifier("configurationSubscriptionMapper")
    protected ConfigurationSubscriptionMapper entryMapper;

    protected ConfigurationSubscriptionService configurationSubscriptionService;

    @Override
    public void afterPropertiesSet() {
        this.configurationSubscriptionService = new ConfigurationSubscriptionService(entityManagerFactory, entryMapper);
    }

    @Override
    public ConfigurationSubscriptionService getObject() {
        return configurationSubscriptionService;
    }

    @Override
    public Class<?> getObjectType() {
        return ConfigurationSubscriptionService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
