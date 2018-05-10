package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("updateContextStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateContextStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            List<ContextExtension> contextExtensionEntries = execution.getContextExtensionDao()
                .findAll(execution.getContext()
                .getProcessInstanceId());

            for (ContextExtension contextExtension : contextExtensionEntries) {
                execution.getContext()
                    .setVariable(contextExtension.getName(), contextExtension.getValue());
                execution.getContextExtensionDao()
                    .remove(contextExtension.getId());
            }

            return StepPhase.DONE;
        } catch (Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPDATING_ACTIVITI_CONTEXT);
            throw new SLException(e);
        }
    }

}
