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
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ArchiveMerger;
import com.sap.cloud.lm.sl.cf.process.util.JarSignatureOperations;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    @Inject
    private JarSignatureOperations jarSignatureOperations;

    private final ResilientOperationExecutor resilientOperationExecutor = new ResilientOperationExecutor();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.VALIDATING_PARAMETERS);

        validateParameters(context.getExecution());
        String space = StepsUtil.getSpace(context.getExecution());
        String org = StepsUtil.getOrg(context.getExecution());
        getStepLogger().info(Messages.DEPLOYING_IN_ORG_0_AND_SPACE_1, org, space);

        getStepLogger().debug(Messages.PARAMETERS_VALIDATED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_VALIDATING_PARAMS;
    }

    private void validateParameters(DelegateExecution execution) {
        validateStartTimeout(execution);
        validateExtensionDescriptorFileIds(execution);
        validateVersionRule(execution);
        validateArchive(execution);
    }

    private void validateStartTimeout(DelegateExecution execution) {
        Object parameter = execution.getVariable(Constants.PARAM_START_TIMEOUT);
        if (parameter == null) {
            return;
        }
        int startTimeout = (int) parameter;
        if (startTimeout < 0) {
            throw new SLException(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeout, Constants.PARAM_START_TIMEOUT);
        }
    }

    private void validateExtensionDescriptorFileIds(DelegateExecution execution) {
        String extensionDescriptorFileId = (String) execution.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        if (extensionDescriptorFileId == null) {
            return;
        }

        String[] extensionDescriptorFileIds = extensionDescriptorFileId.split(",");
        for (String fileId : extensionDescriptorFileIds) {
            FileEntry file = findFile(execution, fileId);
            validateDescriptorSize(file);
        }
    }

    private FileEntry findFile(DelegateExecution execution, String fileId) {
        try {
            String space = StepsUtil.getSpaceId(execution);
            FileEntry fileEntry = fileService.getFile(space, fileId);
            if (fileEntry == null) {
                throw new SLException(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0_IN_SPACE_1, fileId, space);
            }
            return fileEntry;
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.FAILED_TO_RETRIEVE_FILE_WITH_ID_0, fileId);
        }
    }

    private void validateDescriptorSize(FileEntry file) {
        Long maxSizeLimit = configuration.getMaxMtaDescriptorSize();
        if (file.getSize()
                .compareTo(BigInteger.valueOf(maxSizeLimit)) > 0) {
            throw new SLException(com.sap.cloud.lm.sl.mta.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                  file.getSize()
                                      .toString(),
                                  file.getName(),
                                  String.valueOf(maxSizeLimit.longValue()));
        }
    }

    private void validateVersionRule(DelegateExecution execution) {
        String versionRuleString = (String) execution.getVariable(Constants.PARAM_VERSION_RULE);
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

    private void validateArchive(DelegateExecution execution) {
        String[] archivePartIds = getArchivePartIds(execution);
        if (!StepsUtil.shouldVerifyArchiveSignature(execution) && archivePartIds.length == 1) {
            // The archive doesn't need "validation", i.e. merging or signature verification.
            // TODO The merging of chunks should be done prior to this step, since it's not really a validation, but we may need the result
            // here, if the user wants us to verify the archive's signature.
            return;
        }
        Path archive = null;
        try {
            archive = mergeArchiveParts(execution, archivePartIds);
            verifyArchiveSignature(execution, archive);
            if (archivePartIds.length != 1) {
                persistMergedArchive(execution, archive);
            }
        } finally {
            deleteArchive(archive);
        }
    }

    private String[] getArchivePartIds(DelegateExecution execution) {
        String archiveId = StepsUtil.getRequiredString(execution, Constants.PARAM_APP_ARCHIVE_ID);
        return archiveId.split(",");
    }

    private Path mergeArchiveParts(DelegateExecution execution, String[] archivePartIds) {
        List<FileEntry> archivePartEntries = getArchivePartEntries(execution, archivePartIds);
        StepsUtil.setAsJsonBinaries(execution, Constants.VAR_FILE_ENTRIES, archivePartEntries);
        getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
        return resilientOperationExecutor.execute(createArchiveFromParts(execution, archivePartEntries));
    }

    private List<FileEntry> getArchivePartEntries(DelegateExecution execution, String[] appArchivePartsId) {
        return Arrays.stream(appArchivePartsId)
                     .map(appArchivePartId -> findFile(execution, appArchivePartId))
                     .collect(Collectors.toList());
    }

    private Supplier<Path> createArchiveFromParts(DelegateExecution execution, List<FileEntry> archivePartEntries) {
        return () -> new ArchiveMerger(fileService, getStepLogger(), execution).createArchiveFromParts(archivePartEntries);
    }

    private void verifyArchiveSignature(DelegateExecution execution, Path archiveFilePath) {
        if (!StepsUtil.shouldVerifyArchiveSignature(execution)) {
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

    private void persistMergedArchive(DelegateExecution execution, Path archiveFilePath) {
        resilientOperationExecutor.execute(() -> persistMergedArchive(archiveFilePath, execution));
    }

    private void persistMergedArchive(Path archivePath, DelegateExecution execution) {
        FileEntry uploadedArchive = persistArchive(archivePath, execution);
        execution.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private FileEntry persistArchive(Path archivePath, DelegateExecution execution) {
        try {
            return fileService.addFile(StepsUtil.getSpaceId(execution), StepsUtil.getServiceId(execution), archivePath.getFileName()
                                                                                                                      .toString(),
                                       archivePath.toFile());
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private void deleteArchive(Path archiveFilePath) {
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