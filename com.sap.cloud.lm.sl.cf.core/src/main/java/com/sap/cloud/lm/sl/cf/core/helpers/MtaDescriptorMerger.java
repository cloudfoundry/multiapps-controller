package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorMerger;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorValidator;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class MtaDescriptorMerger {

    private HandlerFactory handlerFactory;
    private Platform platform;
    private Target target;

    public MtaDescriptorMerger(HandlerFactory handlerFactory, Platform platform, Target target) {
        this.handlerFactory = handlerFactory;
        this.platform = platform;
        this.target = target;
    }

    public DeploymentDescriptor merge(String deploymentDescriptorString, List<String> extensionDescriptorStrings) throws ContentException {
        DescriptorParser parser = handlerFactory.getDescriptorParser();

        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorString, parser);
        // TODO log without plain text sensitive content
        // LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR, JsonUtil.toJson(deploymentDescriptor, true)));

        List<ExtensionDescriptor> extensionDescriptors = parseExtensionDescriptors(extensionDescriptorStrings, parser);
        deploymentDescriptor = parser.parseDeploymentDescriptorYaml(deploymentDescriptorString);
        for (int i = 0; i < extensionDescriptors.size(); i++) {
            // TODO log without plain text sensitive content
            // LOGGER.debug(format(Messages.EXTENSION_DESCRIPTOR, i, JsonUtil.toJson(extensionDescriptors.get(i), true)));
        }

        // Build an extension descriptor chain:
        extensionDescriptors = handlerFactory.getDescriptorHandler()
            .getExtensionDescriptorChain(deploymentDescriptor, extensionDescriptors, false);

        DescriptorValidator validator = handlerFactory.getDescriptorValidator();
        validator.validateDeploymentDescriptor(deploymentDescriptor, platform);
        validator.validateExtensionDescriptors(extensionDescriptors, deploymentDescriptor);

        DescriptorMerger merger = handlerFactory.getDescriptorMerger();

        // Merge the passed set of descriptors into one deployment descriptor. The deployment descriptor at the root of
        // the chain is merged in as well:
        Pair<DeploymentDescriptor, List<String>> mergedDescriptor = merger.merge(deploymentDescriptor, extensionDescriptors);
        validator.validateMergedDescriptor(mergedDescriptor, target);

        deploymentDescriptor = mergedDescriptor._1;
        // TODO log without plain text sensitive content
        // LOGGER.debug(format(Messages.MERGED_DESCRIPTOR, JsonUtil.toJson(deploymentDescriptor, true)));

        return deploymentDescriptor;
    }

    private List<ExtensionDescriptor> parseExtensionDescriptors(List<String> extensionDescriptorStrings, DescriptorParser parser)
        throws ContentException {
        List<ExtensionDescriptor> extensionDescriptors = new ArrayList<>();
        for (int i = 0; i < extensionDescriptorStrings.size(); i++) {
            ExtensionDescriptor extensionDescriptor = parser.parseExtensionDescriptorYaml(extensionDescriptorStrings.get(i));
            extensionDescriptors.add(extensionDescriptor);
        }
        return extensionDescriptors;
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorString, DescriptorParser parser)
        throws ContentException {
        return parser.parseDeploymentDescriptorYaml(deploymentDescriptorString);
    }

}
