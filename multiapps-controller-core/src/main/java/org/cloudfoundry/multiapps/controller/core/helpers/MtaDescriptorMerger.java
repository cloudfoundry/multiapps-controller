package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.HandlerFactory;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorMerger;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;

public class MtaDescriptorMerger {

    private final HandlerFactory handlerFactory;
    private final Platform platform;
    private final UserMessageLogger userMessageLogger;

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

        deploymentDescriptor = handlerFactory.getDescriptorParametersCompatabilityValidator(mergedDescriptor, userMessageLogger)
                                             .validate();
        logDebug(Messages.MERGED_DESCRIPTOR, SecureSerialization.toJson(deploymentDescriptor));

        return deploymentDescriptor;
    }

    private void logDebug(String pattern, Object... arguments) {
        if (userMessageLogger != null) {
            userMessageLogger.debug(pattern, arguments);
        }
    }

}
