package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.util.ResilientOperationExecutor;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ArchiveMerger;
import com.sap.cloud.lm.sl.cf.process.util.certficate_checker.MtaCertificateChecker;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    @Inject
    private MtaCertificateChecker mtaCertificateChecker;

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
        try {
            validateStartTimeout(context);
            validateAppArchiveId(context);
            validateExtDescriptorFileId(context);
            validateVersionRule(context);
            verifyArchive(context);
        } finally {
            deleteArchiveFile(context);
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

    private void validateAppArchiveId(DelegateExecution context) {
        String appArchiveId = StepsUtil.getRequiredString(context, Constants.PARAM_APP_ARCHIVE_ID);
        String[] appArchivePartsId = appArchiveId.split(",");
        List<FileEntry> archivePartEntries = getArchivePartEntries(context, appArchivePartsId);
        StepsUtil.setAsJsonBinaries(context, Constants.VAR_FILE_ENTRIES, archivePartEntries);
        getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
        new ResilientOperationExecutor().execute(createArchiveFromParts(context, archivePartEntries));
    }

    private List<FileEntry> getArchivePartEntries(DelegateExecution context, String[] appArchivePartsId) {
        List<FileEntry> archivePartEntries = new ArrayList<>();
        for (String appArchivePartId : appArchivePartsId) {
            archivePartEntries.add(findFile(context, appArchivePartId));
        }
        return archivePartEntries;
    }

    private Runnable createArchiveFromParts(DelegateExecution context, List<FileEntry> archivePartEntries) {
        return () -> {
            String archiveFileName = new ArchiveMerger(fileService, getStepLogger(), context).createArchiveFromParts(archivePartEntries);
            context.setVariable(Constants.VAR_ARCHIVE_FILE_NAME, archiveFileName);
        };
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

    private void validateExtDescriptorFileId(DelegateExecution context) {
        String extensionDescriptorFileID = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (extensionDescriptorFileID == null) {
            return;
        }

        String[] extDescriptorFileIds = extensionDescriptorFileID.split(",");
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

    private void verifyArchive(DelegateExecution context) {
        if (!StepsUtil.shouldVerifyArchiveSignature(context)) {
            return;
        }
        String archiveFileName = (String) context.getVariable(Constants.VAR_ARCHIVE_FILE_NAME);
        getStepLogger().debug(Messages.VERIFYING_ARCHIVE_0, archiveFileName);
        verifyArchive(context, archiveFileName);
        getStepLogger().info(Messages.ARCHIVE_IS_VERIFIED);
    }

    private void verifyArchive(DelegateExecution context, String archiveFileName) {
        String certificateCN = StepsUtil.getCertificateCN(context);
        List<X509Certificate> certificates = mtaCertificateChecker.readProvidedCertificates(Constants.SYMANTEC_CERTIFICATE_FILE);
        if (certificateCN != null) {
            getStepLogger().debug(Messages.CUSTOM_CERTIFICATE_CN_NAME_0, certificateCN);
            mtaCertificateChecker.checkCertificates(getArchiveFileNameURL(archiveFileName), certificates, certificateCN);
            return;
        }
        certificateCN = configuration.getCertificateCN();
        getStepLogger().debug(Messages.NO_CUSTOM_CERTIFICATE_CN_NAME_USING_DEFAULT_0, certificateCN);
        mtaCertificateChecker.checkCertificates(getArchiveFileNameURL(archiveFileName), certificates, certificateCN);
    }

    private URL getArchiveFileNameURL(String archiveFileName) {
        try {
            return new URL("file:" + archiveFileName);
        } catch (MalformedURLException e) {
            throw new SLException(e);
        }
    }

    private void deleteArchiveFile(DelegateExecution context) {
        String archiveFileName = (String) context.getVariable(Constants.VAR_ARCHIVE_FILE_NAME);
        if (archiveFileName == null) {
            return;
        }
        tryDeleteArchiveFile(archiveFileName);
    }

    private void tryDeleteArchiveFile(String archiveFileName) {
        try {
            Files.deleteIfExists(Paths.get(archiveFileName));
        } catch (IOException e) {
            logger.warn("Merged file not deleted");
        }
    }
}