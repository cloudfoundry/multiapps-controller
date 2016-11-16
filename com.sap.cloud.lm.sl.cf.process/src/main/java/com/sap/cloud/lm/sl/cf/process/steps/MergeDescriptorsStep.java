package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("mergeDescriptorsStep")
public class MergeDescriptorsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeDescriptorsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("mergeDescriptorsTask", "Merge Descriptors", "Merge Descriptors");
    }

    protected MtaDescriptorMerger getMtaDescriptorMerger(HandlerFactory factory, TargetPlatformType platformType, TargetPlatform platform) {
        return new MtaDescriptorMerger(factory, platformType, platform);
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.MERGING_DESCRIPTORS, LOGGER);
        try {
            String deploymentDescriptorString = StepsUtil.getDeploymentDescriptorString(context);
            List<String> extensionDescriptorStrings = StepsUtil.getExtensionDescriptorStrings(context);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            TargetPlatform platform = StepsUtil.getPlatform(context);
            TargetPlatformType platformType = StepsUtil.getPlatformType(context);

            DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platformType, platform).merge(
                deploymentDescriptorString, extensionDescriptorStrings);

            StepsUtil.setDeploymentDescriptor(context, descriptor);
            context.setVariable(Constants.PARAM_MTA_ID, descriptor.getId());
        } catch (SLException e) {
            error(context, Messages.ERROR_MERGING_DESCRIPTORS, e, LOGGER);
            throw e;
        }
        debug(context, Messages.DESCRIPTORS_MERGED, LOGGER);

        return ExecutionStatus.SUCCESS;
    }

}
