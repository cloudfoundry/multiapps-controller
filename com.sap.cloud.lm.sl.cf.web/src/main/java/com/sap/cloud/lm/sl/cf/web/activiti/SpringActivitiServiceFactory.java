package com.sap.cloud.lm.sl.cf.web.activiti;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.slp.activiti.ActivitiService;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiServiceFactory;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

@Component
public class SpringActivitiServiceFactory implements ActivitiServiceFactory, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public ActivitiService createActivitiService(ServiceMetadata serviceMetadata) {
        return applicationContext.getBean(ActivitiService.class, serviceMetadata);
    }

}
