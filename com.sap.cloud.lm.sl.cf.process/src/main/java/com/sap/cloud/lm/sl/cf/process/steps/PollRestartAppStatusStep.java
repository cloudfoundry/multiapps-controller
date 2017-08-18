package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("pollRestartAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollRestartAppStatusStep extends PollStartAppStatusStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("pollRestartAppStatusTask").displayName("Poll Restart App Status").description(
            "Poll Restart App Status").build();
    }

    @Override
    public String getLogicalStepName() {
        return RestartAppStep.class.getSimpleName();
    }

    @Override
    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getAppToRestart(context);
    }

}
