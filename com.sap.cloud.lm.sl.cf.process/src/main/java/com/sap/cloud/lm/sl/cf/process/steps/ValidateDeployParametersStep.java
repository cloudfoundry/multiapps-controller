package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import com.sap.cloud.lm.sl.cf.process.util.JarSignatureOperations;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    @Inject
    private JarSignatureOperations jarSignatureOperations;

    @Inject
    private ApplicationConfiguration configuration;

    private ResilientOperationExecutor resilientOperationExecutor = new ResilientOperationExecutor();

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
        Path archiveFilePath = null;
        try {
            validateStartTimeout(context);
            archiveFilePath = mergeArchiveParts(context);
            validateExtDescriptorFileId(context);
            validateVersionRule(context);
            verifyArchiveSignature(context, archiveFilePath);
            persistMergedArchive(context, archiveFilePath);
        } finally {
            deleteArchiveFile(archiveFilePath);
        }
    }

    private void validateStartTimeout(DelegateExecution context) {
        Object parameter = context.getVariable(Constants.PARAM_START_TIMEOUT);
        if (parameter == null) {
            return;
        }
        int startTimeout = (int) parameter;
        if (startTimeout < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeout, Constants.PARAM_START_TIMEOUT);
        }
    }

    private Path mergeArchiveParts(DelegateExecution context) {
        String appArchiveId = StepsUtil.getRequiredString(context, Constants.PARAM_APP_ARCHIVE_ID);
        String[] appArchivePartsId = appArchiveId.split(",");
        List<FileEntry> archivePartEntries = getArchivePartEntries(context, appArchivePartsId);
        StepsUtil.setAsJsonBinaries(context, Constants.VAR_FILE_ENTRIES, archivePartEntries);
        getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
        return resilientOperationExecutor.execute(createArchiveFromParts(context, archivePartEntries));
    }

    private List<FileEntry> getArchivePartEntries(DelegateExecution context, String[] appArchivePartsId) {
        return Arrays.stream(appArchivePartsId)
                     .map(appArchivePartId -> findFile(context, appArchivePartId))
                     .collect(Collectors.toList());
    }

    private Supplier<Path> createArchiveFromParts(DelegateExecution context, List<FileEntry> archivePartEntries) {
        return () -> new ArchiveMerger(fileService, getStepLogger(), context).createArchiveFromParts(archivePartEntries);
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
            throw new SLException(e, Messages.FAILED_TO_RETRIEVE_FILE_WITH_ID_0, fileId);
        }
    }

    private void validateExtDescriptorFileId(DelegateExecution context) {
        String extensionDescriptorFileId = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (extensionDescriptorFileId == null) {
            return;
        }

        String[] extDescriptorFileIds = extensionDescriptorFileId.split(",");
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

    private void verifyArchiveSignature(DelegateExecution context, Path archiveFilePath) {
        if (!StepsUtil.shouldVerifyArchiveSignature(context)) {
            return;
        }
        getStepLogger().debug(Messages.VERIFYING_ARCHIVE_0, archiveFilePath);
        verifyArchiveSignature(archiveFilePath);
        getStepLogger().info(Messages.ARCHIVE_IS_VERIFIED);
    }

    private void verifyArchiveSignature(Path archiveFilePath) {
        String certificateCN = configuration.getCertificateCN();
        getStepLogger().debug(Messages.WILL_LOOK_FOR_CERTIFICATE_CN, certificateCN);
        List<X509Certificate> certificates = jarSignatureOperations.readCertificates(Constants.SYMANTEC_CERTIFICATE_FILE);
        jarSignatureOperations.checkCertificates(getArchiveFilePathURL(archiveFilePath), certificates, certificateCN);
    }

    private URL getArchiveFilePathURL(Path archiveFilePath) {
        try {
            return archiveFilePath.toUri()
                                  .toURL();
        } catch (MalformedURLException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private void persistMergedArchive(DelegateExecution context, Path archiveFilePath) {
        resilientOperationExecutor.execute(() -> persistMergedArchive(archiveFilePath, context));
    }

    private void persistMergedArchive(Path archivePath, DelegateExecution context) {
        FileEntry uploadedArchive = persistArchive(archivePath, context);
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private FileEntry persistArchive(Path archivePath, DelegateExecution context) {
        try {
            return fileService.addFile(StepsUtil.getSpaceId(context), StepsUtil.getServiceId(context), archivePath.getFileName()
                                                                                                                  .toString(),
                                       archivePath.toFile());
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private void deleteArchiveFile(Path archiveFilePath) {
        if (archiveFilePath == null) {
            return;
        }
        tryDeleteArchiveFile(archiveFilePath);
    }

    private void tryDeleteArchiveFile(Path archiveFilePath) {
        try {
            Files.deleteIfExists(archiveFilePath);
        } catch (IOException e) {
            logger.warn(Messages.MERGED_FILE_NOT_DELETED);
        }
    }
}