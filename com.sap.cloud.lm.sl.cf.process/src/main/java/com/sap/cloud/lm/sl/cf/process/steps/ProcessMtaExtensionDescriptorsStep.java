package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("processMtaExtensionDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaExtensionDescriptorsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().info(Messages.PROCESSING_MTA_EXTENSION_DESCRIPTORS);
        List<String> extensionDescriptorFileIds = getExtensionDescriptorFileIds(execution.getContext());
        try {
            String spaceId = StepsUtil.getSpaceId(execution.getContext());

            StepsUtil.setArrayVariableFromCollection(execution.getContext(), Constants.VAR_MTA_EXTENSION_DESCRIPTOR_STRINGS,
                getExtensionDescriptors(spaceId, extensionDescriptorFileIds));
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_PROCESSING_MTA_EXTENSION_DESCRIPTORS);
            throw e;
        }
        getStepLogger().debug(Messages.MTA_EXTENSION_DESCRIPTORS_PROCESSED);
        return StepPhase.DONE;
    }

    private List<String> getExtensionDescriptors(String spaceId, List<String> fileIds) throws SLException {
        try {
            final List<String> extensionDescriptorStrings = new ArrayList<String>();

            FileContentProcessor extensionDescriptorProcessor = new FileContentProcessor() {
                @Override
                public void processFileContent(InputStream is) throws IOException {
                    extensionDescriptorStrings.add(IOUtils.toString(is));
                }
            };
            for (String extensionDescriptorFileId : fileIds) {
                fileService
                    .processFileContent(new DefaultFileDownloadProcessor(spaceId, extensionDescriptorFileId, extensionDescriptorProcessor));
            }
            getStepLogger().debug(Messages.EXTENSION_DESCRIPTOR, extensionDescriptorStrings);
            return extensionDescriptorStrings;
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_EXTENSION_DESCRIPTOR);
        }
    }

    private List<String> getExtensionDescriptorFileIds(DelegateExecution context) {
        String parameter = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (parameter == null || parameter.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(parameter.split(","));
    }

}
