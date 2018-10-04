package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorValidator;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class MtaDescriptorMerger {

    private HandlerFactory handlerFactory;
    private Platform platform;
    private Target target;
    private UserMessageLogger userMessageLogger;
    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public MtaDescriptorMerger(HandlerFactory handlerFactory, Platform platform, Target target) {
        this(handlerFactory, platform, target, null);
    }

    public MtaDescriptorMerger(HandlerFactory handlerFactory, Platform platform, Target target, UserMessageLogger userMessageLogger) {
        this.handlerFactory = handlerFactory;
        this.platform = platform;
        this.target = target;
        this.userMessageLogger = userMessageLogger;
    }

    public DeploymentDescriptor merge(DeploymentDescriptor deploymentDescriptor, List<ExtensionDescriptor> extensionDescriptors) {
        DescriptorValidator validator = handlerFactory.getDescriptorValidator();
        validator.validateDeploymentDescriptor(deploymentDescriptor, platform);
        validator.validateExtensionDescriptors(extensionDescriptors, deploymentDescriptor);

        DescriptorMerger merger = handlerFactory.getDescriptorMerger();

        // Merge the passed set of descriptors into one deployment descriptor. The deployment descriptor at the root of
        // the chain is merged in as well:
        Pair<DeploymentDescriptor, List<String>> mergedDescriptor = merger.merge(deploymentDescriptor, extensionDescriptors);
        validator.validateMergedDescriptor(mergedDescriptor, target);

        deploymentDescriptor = mergedDescriptor._1;
        logDebug(Messages.MERGED_DESCRIPTOR, secureSerializer.toJson(deploymentDescriptor));

        return deploymentDescriptor;
    }

    private void logDebug(String pattern, Object... arguments) {
        if (userMessageLogger != null) {
            userMessageLogger.debug(pattern, arguments);
        }
    }

}
