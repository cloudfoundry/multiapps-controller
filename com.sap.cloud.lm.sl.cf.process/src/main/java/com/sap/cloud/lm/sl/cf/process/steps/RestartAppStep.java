package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("restartAppStep")
public class RestartAppStep extends StartAppStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("restartAppTask").displayName("Restart App").description("Restart App").children(
            PollRestartAppStatusStep.getMetadata()).build();
    }

    @Override
    protected CloudApplication getAppToStart(DelegateExecution context) {
        return StepsUtil.getAppToRestart(context);
    }

}
