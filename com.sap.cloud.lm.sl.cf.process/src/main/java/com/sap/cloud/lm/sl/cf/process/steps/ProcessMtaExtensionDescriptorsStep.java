package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.DescriptorParserFacadeFactory;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentConsumer;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.builders.ExtensionDescriptorChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;

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
            getStepLogger().debug(Messages.EXTENSION_DESCRIPTORS, SecureSerialization.toJson(extensionDescriptors));
            return extensionDescriptors;
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }

    }

}
