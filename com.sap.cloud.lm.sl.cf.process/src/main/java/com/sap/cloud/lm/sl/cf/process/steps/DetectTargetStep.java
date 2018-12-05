package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@Component("detectTargetStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectTargetStep extends SyncFlowableStep {

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_TARGET);
        try {
            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
            Platform platform = configuration.getPlatform(handlerFactory.getConfigurationParser(), handlerFactory.getMajorVersion());
            StepsUtil.setPlatform(execution.getContext(), platform);

            String space = (String) execution.getContext()
                .getVariable(Constants.VAR_SPACE);
            String org = (String) execution.getContext()
                .getVariable(Constants.VAR_ORG);
            getStepLogger().info(Messages.DEPLOYING_IN_ORG_0_AND_SPACE_1, org, space);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_TARGET);
            throw e;
        }
        return StepPhase.DONE;
    }

}
