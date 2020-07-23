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

import org.cloudfoundry.multiapps.common.SLException;
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
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    @Inject
    private JarSignatureOperations jarSignatureOperations;

    private final ResilientOperationExecutor resilientOperationExecutor = new ResilientOperationExecutor();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.VALIDATING_PARAMETERS);

        validateParameters(context);
        String spaceName = context.getVariable(Variables.SPACE_NAME);
        String organizationName = context.getVariable(Variables.ORGANIZATION_NAME);
        getStepLogger().info(Messages.DEPLOYING_IN_ORG_0_AND_SPACE_1, organizationName, spaceName);

        getStepLogger().debug(Messages.PARAMETERS_VALIDATED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_VALIDATING_PARAMS;
    }

    private void validateParameters(ProcessContext context) {
        validateExtensionDescriptorFileIds(context);
        validateArchive(context);
    }

    private void validateExtensionDescriptorFileIds(ProcessContext context) {
        String extensionDescriptorFileId = context.getVariable(Variables.EXT_DESCRIPTOR_FILE_ID);
        if (extensionDescriptorFileId == null) {
            return;
        }

        String[] extensionDescriptorFileIds = extensionDescriptorFileId.split(",");
        for (String fileId : extensionDescriptorFileIds) {
            FileEntry file = findFile(context, fileId);
            validateDescriptorSize(file);
        }
    }

    private FileEntry findFile(ProcessContext context, String fileId) {
        try {
            String spaceGuid = context.getVariable(Variables.SPACE_GUID);
            FileEntry fileEntry = fileService.getFile(spaceGuid, fileId);
            if (fileEntry == null) {
                throw new SLException(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0_IN_SPACE_1, fileId, spaceGuid);
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
            throw new SLException(org.cloudfoundry.multiapps.mta.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                  file.getSize()
                                      .toString(),
                                  file.getName(),
                                  String.valueOf(maxSizeLimit.longValue()));
        }
    }

    private void validateArchive(ProcessContext context) {
        String[] archivePartIds = getArchivePartIds(context);
        if (!context.getVariable(Variables.VERIFY_ARCHIVE_SIGNATURE) && archivePartIds.length == 1) {
            // The archive doesn't need "validation", i.e. merging or signature verification.
            // TODO The merging of chunks should be done prior to this step, since it's not really a validation, but we may need the result
            // here, if the user wants us to verify the archive's signature.
            return;
        }
        Path archive = null;
        try {
            archive = mergeArchiveParts(context, archivePartIds);
            verifyArchiveSignature(context, archive);
            if (archivePartIds.length != 1) {
                persistMergedArchive(context, archive);
            }
        } finally {
            deleteArchive(archive);
        }
    }

    private String[] getArchivePartIds(ProcessContext context) {
        String archiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        return archiveId.split(",");
    }

    private Path mergeArchiveParts(ProcessContext context, String[] archivePartIds) {
        List<FileEntry> archivePartEntries = getArchivePartEntries(context, archivePartIds);
        context.setVariable(Variables.FILE_ENTRIES, archivePartEntries);
        getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
        return resilientOperationExecutor.execute(createArchiveFromParts(context.getExecution(), archivePartEntries));
    }

    private List<FileEntry> getArchivePartEntries(ProcessContext context, String[] appArchivePartsId) {
        return Arrays.stream(appArchivePartsId)
                     .map(appArchivePartId -> findFile(context, appArchivePartId))
                     .collect(Collectors.toList());
    }

    private Supplier<Path> createArchiveFromParts(DelegateExecution execution, List<FileEntry> archivePartEntries) {
        return () -> new ArchiveMerger(fileService, getStepLogger(), execution).createArchiveFromParts(archivePartEntries);
    }

    private void verifyArchiveSignature(ProcessContext context, Path archiveFilePath) {
        if (!context.getVariable(Variables.VERIFY_ARCHIVE_SIGNATURE)) {
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

    private void persistMergedArchive(ProcessContext context, Path archiveFilePath) {
        resilientOperationExecutor.execute(() -> persistMergedArchive(archiveFilePath, context));
    }

    private void persistMergedArchive(Path archivePath, ProcessContext context) {
        FileEntry uploadedArchive = persistArchive(archivePath, context);
        context.setVariable(Variables.APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private FileEntry persistArchive(Path archivePath, ProcessContext context) {
        try {
            return fileService.addFile(context.getVariable(Variables.SPACE_GUID), context.getVariable(Variables.MTA_NAMESPACE),
                                       archivePath.getFileName()
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