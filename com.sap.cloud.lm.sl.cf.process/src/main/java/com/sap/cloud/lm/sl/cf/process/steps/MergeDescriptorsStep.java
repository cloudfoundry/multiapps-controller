package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MergeDescriptorsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeDescriptorsStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("mergeDescriptorsTask").displayName("Merge Descriptors").description("Merge Descriptors").build();
    }

    protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, Platform platform, Target target) {
        return new MtaDescriptorMerger(factory, platform, target);
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.MERGING_DESCRIPTORS, LOGGER);
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
            error(context, Messages.ERROR_MERGING_DESCRIPTORS, e, LOGGER);
            throw e;
        }
        debug(context, Messages.DESCRIPTORS_MERGED, LOGGER);

        return ExecutionStatus.SUCCESS;
    }

}
