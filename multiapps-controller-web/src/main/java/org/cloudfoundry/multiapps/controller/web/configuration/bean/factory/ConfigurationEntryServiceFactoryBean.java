package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService.ConfigurationEntryMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

@Named("configurationEntryService")
public class ConfigurationEntryServiceFactoryBean implements FactoryBean<ConfigurationEntryService>, InitializingBean {

    @Inject
    protected EntityManagerFactory entityManagerFactory;
    @Inject
    @Qualifier("configurationEntryObjectMapper")
    protected ConfigurationEntryMapper entryMapper;

    protected ConfigurationEntryService configurationEntryService;

    @Override
    public void afterPropertiesSet() {
        this.configurationEntryService = new ConfigurationEntryService(entityManagerFactory, entryMapper);
    }

    @Override
    public ConfigurationEntryService getObject() {
        return configurationEntryService;
    }

    @Override
    public Class<?> getObjectType() {
        return ConfigurationEntryService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
