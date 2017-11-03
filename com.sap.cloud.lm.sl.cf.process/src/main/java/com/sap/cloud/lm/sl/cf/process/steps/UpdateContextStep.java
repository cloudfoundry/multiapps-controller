package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("updateContextStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateContextStep extends AbstractProcessStep {

    @Inject
    ContextExtensionDao contextExtensionDao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();
        try {
            List<ContextExtension> contextExtensionEntries = contextExtensionDao.findAll(context.getProcessInstanceId());

            for (ContextExtension contextExtension : contextExtensionEntries) {
                context.setVariable(contextExtension.getName(), contextExtension.getValue());
                contextExtensionDao.remove(contextExtension.getId());
            }

            return ExecutionStatus.SUCCESS;
        } catch (Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPDATING_ACTIVITI_CONTEXT);
            throw new SLException(e);
        }
    }

}
