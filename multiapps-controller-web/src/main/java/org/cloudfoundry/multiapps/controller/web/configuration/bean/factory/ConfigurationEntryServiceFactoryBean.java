package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService.ConfigurationEntryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

@Named("configurationEntryService")
public class ConfigurationEntryServiceFactoryBean implements FactoryBean<ConfigurationEntryService>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntryServiceFactoryBean.class);
    @Inject
    protected EntityManagerFactory entityManagerFactory;
    @Inject
    @Qualifier("configurationEntryMapper")
    protected ConfigurationEntryMapper entryMapper;

    protected ConfigurationEntryService configurationEntryService;

    @Override
    public void afterPropertiesSet() {
        LOGGER.warn("entryMapper: " + entryMapper);
        if (entryMapper != null) {
            LOGGER.warn("entryMapper class: " + entryMapper.getClass());
        }
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
