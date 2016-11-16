package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("processMtaExtensionDescriptorsStep")
public class ProcessMtaExtensionDescriptorsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMtaExtensionDescriptorsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("processExtensionDescriptorsTask", "Process Extension Descriptors", "Process Extension Descriptors");
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.PROCESSING_MTA_EXTENSION_DESCRIPTORS, LOGGER);
        List<String> extensionDescriptorFileIds = getExtensionDescriptorFileIds(context);
        try {
            String spaceIdd = StepsUtil.getSpaceId(context);

            ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_MTA_EXTENSION_DESCRIPTOR_STRINGS,
                getExtensionDescriptorsAsStrings(spaceIdd, extensionDescriptorFileIds));
        } catch (SLException e) {
            error(context, Messages.ERROR_PROCESSING_MTA_EXTENSION_DESCRIPTORS, LOGGER);
            throw e;
        }
        debug(context, Messages.MTA_EXTENSION_DESCRIPTORS_PROCESSED, LOGGER);
        return ExecutionStatus.SUCCESS;
    }

    private List<String> getExtensionDescriptorsAsStrings(String spaceId, List<String> fileIds) throws SLException {
        try {
            final List<String> extensionDescriptorStrings = new ArrayList<String>();

            FileContentProcessor extensionDescriptorProcessor = new FileContentProcessor() {
                @Override
                public void processFileContent(InputStream is) throws IOException {
                    extensionDescriptorStrings.add(IOUtils.toString(is));
                }
            };
            for (String extensionDescriptorFileId : fileIds) {
                fileService.processFileContent(
                    new DefaultFileDownloadProcessor(spaceId, extensionDescriptorFileId, extensionDescriptorProcessor));
            }
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
