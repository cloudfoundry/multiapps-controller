package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@Component("mergeDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MergeDescriptorsStep extends SyncFlowableStep {

    protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform) {
        return new MtaDescriptorMerger(factory, platform, getStepLogger());
    }

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.MERGING_DESCRIPTORS);
        try {
            DeploymentDescriptor deploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(execution.getContext());
            List<ExtensionDescriptor> extensionDescriptors = StepsUtil.getExtensionDescriptorChain(execution.getContext());

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());

            Platform platform = StepsUtil.getPlatform(execution.getContext());

            DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platform).merge(deploymentDescriptor,
                extensionDescriptors);

            StepsUtil.setUnresolvedDeploymentDescriptor(execution.getContext(), descriptor);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_MERGING_DESCRIPTORS);
            throw e;
        }
        getStepLogger().debug(Messages.DESCRIPTORS_MERGED);

        return StepPhase.DONE;
    }

}
