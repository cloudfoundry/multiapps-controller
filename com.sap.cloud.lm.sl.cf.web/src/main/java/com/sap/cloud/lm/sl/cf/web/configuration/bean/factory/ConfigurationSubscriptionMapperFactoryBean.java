package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import javax.inject.Named;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService.ConfigurationSubscriptionMapper;

@Named("configurationSubscriptionMapper")
public class ConfigurationSubscriptionMapperFactoryBean implements FactoryBean<ConfigurationSubscriptionMapper>, InitializingBean {

    protected ConfigurationSubscriptionMapper entryMapper;

    @Override
    public void afterPropertiesSet() {
        this.entryMapper = new ConfigurationSubscriptionMapper();
    }

    @Override
    public ConfigurationSubscriptionMapper getObject() {
        return entryMapper;
    }

    @Override
    public Class<?> getObjectType() {
        return ConfigurationSubscriptionMapper.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
