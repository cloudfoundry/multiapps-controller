package com.sap.cloud.lm.sl.cf.web.activiti;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.slp.activiti.ActiveActivitiProcess;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiProcessFactory;
import com.sap.cloud.lm.sl.slp.activiti.FinishedActivitiProcess;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

@Component
public class SpringActivitiProcessFactory implements ActivitiProcessFactory, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public ActiveActivitiProcess create(ServiceMetadata serviceMetadata, ProcessInstance processInstance) {
        return applicationContext.getBean(ActiveActivitiProcess.class, serviceMetadata, processInstance);
    }

    @Override
    public FinishedActivitiProcess create(ServiceMetadata serviceMetadata, HistoricProcessInstance processInstance) {
        return applicationContext.getBean(FinishedActivitiProcess.class, serviceMetadata, processInstance);
    }

}
