package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveMerger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

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
        if (archivePartIds.length == 1) {
            // The archive doesn't need "validation", i.e. merging or signature verification.
            // TODO The merging of chunks should be done prior to this step, since it's not really a validation, but we may need the result
            // here, if the user wants us to verify the archive's signature.
            return;
        }
        Path archive = null;
        try {
            archive = mergeArchiveParts(context, archivePartIds);
            persistMergedArchive(context, archive);
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
        var archiveMerger = new ArchiveMerger(fileService, getStepLogger(), context.getExecution());
        return resilientOperationExecutor.execute((Supplier<Path>) () -> archiveMerger.createArchiveFromParts(archivePartEntries));
    }

    private List<FileEntry> getArchivePartEntries(ProcessContext context, String[] appArchivePartsId) {
        return Arrays.stream(appArchivePartsId)
                     .map(appArchivePartId -> findFile(context, appArchivePartId))
                     .collect(Collectors.toList());
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