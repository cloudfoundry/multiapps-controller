package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("pollStageAppStatusOnCfStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollStageAppStatusOnCfStep extends PollStageAppStatusStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("pollStageAppStatusOnCfTask").displayName("Poll Stage App Status On CF").description(
            "Poll Stage App Status On CF").build();
    }

    @Override
    public String getLogicalStepName() {
        // Staging is not a standalone operation on CF, but is instead part of the start.
        return StartAppStep.class.getSimpleName();
    }

}
