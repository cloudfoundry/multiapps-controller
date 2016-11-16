package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("updateContextStep")
public class UpdateContextStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateContextStep.class);

    @Inject
    ContextExtensionDao contextExtensionDao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            List<ContextExtension> contextExtensionEntries = contextExtensionDao.findAll(context.getProcessInstanceId());

            for (ContextExtension contextExtension : contextExtensionEntries) {
                context.setVariable(contextExtension.getName(), contextExtension.getValue());
                contextExtensionDao.remove(contextExtension.getId());
            }

            return ExecutionStatus.SUCCESS;
        } catch (Exception e) {
            error(context, Messages.ERROR_UPDATING_ACTIVITI_CONTEXT, e, LOGGER);
            throw new SLException(e);
        }
    }

}
