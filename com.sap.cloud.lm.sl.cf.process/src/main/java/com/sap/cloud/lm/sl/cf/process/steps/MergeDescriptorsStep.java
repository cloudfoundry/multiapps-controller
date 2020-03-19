package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

@Named("mergeDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MergeDescriptorsStep extends SyncFlowableStep {

    protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform) {
        return new MtaDescriptorMerger(factory, platform, getStepLogger());
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.MERGING_DESCRIPTORS);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        List<ExtensionDescriptor> extensionDescriptors = StepsUtil.getExtensionDescriptorChain(context.getExecution());

        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        Platform platform = configuration.getPlatform();
        DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platform).merge(deploymentDescriptor,
                                                                                                 extensionDescriptors);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
        getStepLogger().debug(Messages.DESCRIPTORS_MERGED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_MERGING_DESCRIPTORS;
    }

}
