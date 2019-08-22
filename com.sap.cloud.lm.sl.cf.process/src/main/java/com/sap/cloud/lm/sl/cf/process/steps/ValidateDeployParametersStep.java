package com.sap.cloud.lm.sl.cf.process.steps;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.ResilientOperationExecutor;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ArchiveMerger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

@Component("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.VALIDATING_PARAMETERS);

        validateParameters(execution.getContext());
        String space = StepsUtil.getSpace(execution.getContext());
        String org = StepsUtil.getOrg(execution.getContext());
        getStepLogger().info(Messages.DEPLOYING_IN_ORG_0_AND_SPACE_1, org, space);

        getStepLogger().debug(Messages.PARAMETERS_VALIDATED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_VALIDATING_PARAMS;
    }

    private void validateParameters(DelegateExecution context) {
        validateStartTimeout(context);
        validateAppArchiveId(context);
        validateExtDescriptorFileId(context);
        validateVersionRule(context);
    }

    private void validateVersionRule(DelegateExecution context) {
        String versionRuleString = (String) context.getVariable(Constants.PARAM_VERSION_RULE);
        try {
            VersionRule.value(versionRuleString);
        } catch (IllegalArgumentException e) {
            throw new SLException(e,
                                  Messages.ERROR_PARAMETER_1_IS_NOT_VALID_VALID_VALUES_ARE_2,
                                  versionRuleString,
                                  Constants.PARAM_VERSION_RULE,
                                  Arrays.asList(VersionRule.values()));
        }
    }

    private void validateExtDescriptorFileId(DelegateExecution context) {
        String parameter = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (parameter == null) {
            return;
        }

        String[] extDescriptorFileIds = parameter.split(",");
        for (String extDescriptorFileId : extDescriptorFileIds) {
            FileEntry file = findFile(context, extDescriptorFileId);
            validateDescriptorSize(file);
        }
    }

    private void validateDescriptorSize(FileEntry file) {
        Long maxSizeLimit = configuration.getMaxMtaDescriptorSize();
        if (file.getSize()
                .compareTo(BigInteger.valueOf(maxSizeLimit)) > 0) {
            throw new SLException(com.sap.cloud.lm.sl.mta.message.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                  file.getSize()
                                      .toString(),
                                  file.getName(),
                                  String.valueOf(maxSizeLimit.longValue()));
        }
    }

    private void validateAppArchiveId(DelegateExecution context) {
        String appArchiveId = StepsUtil.getRequiredString(context, Constants.PARAM_APP_ARCHIVE_ID);
        String[] appArchivePartsId = appArchiveId.split(",");
        if (appArchivePartsId.length == 1) {
            findFile(context, appArchiveId);
            return;
        }

        List<FileEntry> archivePartEntries = new ArrayList<>();
        for (String appArchivePartId : appArchivePartsId) {
            archivePartEntries.add(findFile(context, appArchivePartId));
        }

        StepsUtil.setAsJsonBinaries(context, Constants.VAR_FILE_ENTRIES, archivePartEntries);
        getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
        new ResilientOperationExecutor().execute(() -> new ArchiveMerger(fileService,
                                                                         getStepLogger(),
                                                                         context,
                                                                         logger).createArchiveFromParts(archivePartEntries));
    }

    private FileEntry findFile(DelegateExecution context, String fileId) {
        try {
            String space = StepsUtil.getSpaceId(context);
            FileEntry fileEntry = fileService.getFile(space, fileId);
            if (fileEntry == null) {
                throw new SLException(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0_IN_SPACE_1, fileId, space);
            }
            return fileEntry;
        } catch (FileStorageException e) {
            throw new SLException(e, "Failed to retrieve file with id \"{0}\"", fileId);
        }
    }

    private void validateStartTimeout(DelegateExecution context) {
        Object parameter = context.getVariable(Constants.PARAM_START_TIMEOUT);
        if (parameter == null) {
            return;
        }
        int startTimeout = (Integer) parameter;
        if (startTimeout < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeout, Constants.PARAM_START_TIMEOUT);
        }
    }
}