package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.builders.ExtensionDescriptorChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;

@Named("processMtaExtensionDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaExtensionDescriptorsStep extends SyncFlowableStep {

    protected final SecureSerializationFacade secureSerializationFacade = new SecureSerializationFacade();
    protected DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
    protected ExtensionDescriptorChainBuilder extensionDescriptorChainBuilder = new ExtensionDescriptorChainBuilder(false);

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();
        getStepLogger().debug(Messages.PROCESSING_MTA_EXTENSION_DESCRIPTORS);
        List<String> extensionDescriptorFileIds = getExtensionDescriptorFileIds(context);
        String spaceId = StepsUtil.getSpaceId(context);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);

        List<ExtensionDescriptor> extensionDescriptors = parseExtensionDescriptors(spaceId, extensionDescriptorFileIds);
        List<ExtensionDescriptor> extensionDescriptorChain = extensionDescriptorChainBuilder.build(deploymentDescriptor,
                                                                                                   extensionDescriptors);

        StepsUtil.setExtensionDescriptorChain(context, extensionDescriptorChain);
        getStepLogger().debug(Messages.MTA_EXTENSION_DESCRIPTORS_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_PROCESSING_MTA_EXTENSION_DESCRIPTORS;
    }

    private List<String> getExtensionDescriptorFileIds(DelegateExecution context) {
        String parameter = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (parameter == null || parameter.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(parameter.split(","));
    }

    private List<ExtensionDescriptor> parseExtensionDescriptors(String spaceId, List<String> fileIds) {
        try {
            List<ExtensionDescriptor> extensionDescriptors = new ArrayList<>();

            FileContentProcessor extensionDescriptorProcessor = extensionDescriptorStream -> {
                ExtensionDescriptor extensionDescriptor = descriptorParserFacade.parseExtensionDescriptor(extensionDescriptorStream);
                extensionDescriptors.add(extensionDescriptor);
            };
            for (String extensionDescriptorFileId : fileIds) {
                fileService.processFileContent(spaceId, extensionDescriptorFileId, extensionDescriptorProcessor);
            }
            getStepLogger().debug(Messages.EXTENSION_DESCRIPTORS, secureSerializationFacade.toJson(extensionDescriptors));
            return extensionDescriptors;
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }

    }

}
