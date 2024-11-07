package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.builders.ExtensionDescriptorChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("processMtaExtensionDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaExtensionDescriptorsStep extends SyncFlowableStep {

    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;
    protected ExtensionDescriptorChainBuilder extensionDescriptorChainBuilder = new ExtensionDescriptorChainBuilder(false);

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.PROCESSING_MTA_EXTENSION_DESCRIPTORS);
        List<String> extensionDescriptorFileIds = getExtensionDescriptorFileIds(context);
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);

        List<ExtensionDescriptor> extensionDescriptors = parseExtensionDescriptors(spaceId, extensionDescriptorFileIds);
        List<ExtensionDescriptor> extensionDescriptorChain = extensionDescriptorChainBuilder.build(deploymentDescriptor,
                                                                                                   extensionDescriptors);

        logUsageOfExtensionDescriptors(extensionDescriptors, extensionDescriptorChain);

        context.setVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, extensionDescriptorChain);
        getStepLogger().debug(Messages.MTA_EXTENSION_DESCRIPTORS_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_EXTENSION_DESCRIPTORS;
    }

    private List<String> getExtensionDescriptorFileIds(ProcessContext context) {
        String parameter = context.getVariable(Variables.EXT_DESCRIPTOR_FILE_ID);
        if (parameter == null || parameter.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(parameter.split(","));
    }

    private List<ExtensionDescriptor> parseExtensionDescriptors(String spaceId, List<String> fileIds) {
        try {
            DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
            List<ExtensionDescriptor> extensionDescriptors = new ArrayList<>();

            FileContentConsumer extensionDescriptorConsumer = extensionDescriptorStream -> {
                ExtensionDescriptor extensionDescriptor = descriptorParserFacade.parseExtensionDescriptor(extensionDescriptorStream);
                extensionDescriptors.add(extensionDescriptor);
            };
            for (String extensionDescriptorFileId : fileIds) {
                fileService.consumeFileContent(spaceId, extensionDescriptorFileId, extensionDescriptorConsumer);
            }
            getStepLogger().debug(Messages.PROVIDED_EXTENSION_DESCRIPTORS, SecureSerialization.toJson(extensionDescriptors));
            return extensionDescriptors;
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }

    }

    private void logUsageOfExtensionDescriptors(List<ExtensionDescriptor> providedExtensionDescriptors,
                                                List<ExtensionDescriptor> extensionDescriptorChain) {
        if (CollectionUtils.isEmpty(extensionDescriptorChain)) {
            getStepLogger().debug(Messages.NO_EXTENSION_DESCRIPTORS_IN_USE);
            return;
        }

        List<String> usedExtensionDescriptorIds = extensionDescriptorChain.stream()
                                                                          .map(ExtensionDescriptor::getId)
                                                                          .collect(Collectors.toList());

        if (usedExtensionDescriptorIds.size() == 1) {
            getStepLogger().info(format(Messages.USING_EXTENSION_DESCRIPTOR, usedExtensionDescriptorIds.get(0)));
        } else {
            getStepLogger().info(format(Messages.USING_EXTENSION_DESCRIPTORS_IN_SEQUENCE, String.join(",", usedExtensionDescriptorIds)));
        }

        List<String> unusedExtensionDescriptors = providedExtensionDescriptors.stream()
                                                                              .map(ExtensionDescriptor::getId)
                                                                              .filter(id -> !usedExtensionDescriptorIds.contains(id))
                                                                              .collect(Collectors.toList());

        if (!unusedExtensionDescriptors.isEmpty()) {
            getStepLogger().info(format(Messages.PROVIDED_AND_UNUSED_EXTENSION_DESCRIPTORS, String.join(",", usedExtensionDescriptorIds)));
        }
    }

}
