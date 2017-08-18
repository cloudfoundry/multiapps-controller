package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("mergeDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MergeDescriptorsStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("mergeDescriptorsTask").displayName("Merge Descriptors").description("Merge Descriptors").build();
    }

    protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform, Target target) {
        return new MtaDescriptorMerger(factory, platform, target);
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        getStepLogger().info(Messages.MERGING_DESCRIPTORS);
        try {
            String deploymentDescriptorString = StepsUtil.getDeploymentDescriptorString(context);
            List<String> extensionDescriptorStrings = StepsUtil.getExtensionDescriptorStrings(context);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            Target target = StepsUtil.getTarget(context);
            Platform platform = StepsUtil.getPlatform(context);

            DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platform, target).merge(deploymentDescriptorString,
                extensionDescriptorStrings);

            StepsUtil.setUnresolvedDeploymentDescriptor(context, descriptor);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_MERGING_DESCRIPTORS);
            throw e;
        }
        getStepLogger().debug(Messages.DESCRIPTORS_MERGED);

        return ExecutionStatus.SUCCESS;
    }

}
