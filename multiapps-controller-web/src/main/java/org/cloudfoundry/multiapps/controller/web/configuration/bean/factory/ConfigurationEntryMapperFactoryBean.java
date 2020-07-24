package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService.ConfigurationEntryMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

@Named("configurationEntryMapper")
public class ConfigurationEntryMapperFactoryBean implements FactoryBean<ConfigurationEntryMapper>, InitializingBean {

    protected ConfigurationEntryMapper entryMapper;

    @Override
    public void afterPropertiesSet() {
        this.entryMapper = new ConfigurationEntryMapper();
    }

    @Override
    public ConfigurationEntryMapper getObject() {
        return entryMapper;
    }

    @Override
    public Class<?> getObjectType() {
        return ConfigurationEntryMapper.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
