package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorValidator;
import com.sap.cloud.lm.sl.mta.model.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;

public class MtaDescriptorMerger {

    private HandlerFactory handlerFactory;
    private Platform platform;
    private UserMessageLogger userMessageLogger;
    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public MtaDescriptorMerger(HandlerFactory handlerFactory, Platform platform) {
        this(handlerFactory, platform, null);
    }

    public MtaDescriptorMerger(HandlerFactory handlerFactory, Platform platform, UserMessageLogger userMessageLogger) {
        this.handlerFactory = handlerFactory;
        this.platform = platform;
        this.userMessageLogger = userMessageLogger;
    }

    public DeploymentDescriptor merge(DeploymentDescriptor deploymentDescriptor, List<ExtensionDescriptor> extensionDescriptors) {
        DescriptorValidator validator = handlerFactory.getDescriptorValidator();
        validator.validateDeploymentDescriptor(deploymentDescriptor, platform);
        validator.validateExtensionDescriptors(extensionDescriptors, deploymentDescriptor);

        DescriptorMerger merger = handlerFactory.getDescriptorMerger();

        // Merge the passed set of descriptors into one deployment descriptor. The deployment descriptor at the root of
        // the chain is merged in as well:
        DeploymentDescriptor mergedDescriptor = merger.merge(deploymentDescriptor, extensionDescriptors);
        validator.validateMergedDescriptor(mergedDescriptor);

        handlerFactory.getPlatformMerger(platform)
            .mergeInto(mergedDescriptor);

        deploymentDescriptor = mergedDescriptor;
        logDebug(Messages.MERGED_DESCRIPTOR, secureSerializer.toJson(deploymentDescriptor));

        return deploymentDescriptor;
    }

    private void logDebug(String pattern, Object... arguments) {
        if (userMessageLogger != null) {
            userMessageLogger.debug(pattern, arguments);
        }
    }

}
