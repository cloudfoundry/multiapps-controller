package org.cloudfoundry.multiapps.controller.process.steps;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveStreamWithName;
import org.cloudfoundry.multiapps.controller.process.util.FileSweeper;
import org.cloudfoundry.multiapps.controller.process.util.MergedArchiveStreamCreator;
import org.cloudfoundry.multiapps.controller.process.util.PriorityCallable;
import org.cloudfoundry.multiapps.controller.process.util.PriorityFuture;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncFlowableStep {

    private final ResilientOperationExecutor resilientOperationExecutor = new ResilientOperationExecutor();
    private final ExecutorService fileStorageThreadPool;

    @Inject
    public ValidateDeployParametersStep(ExecutorService fileStorageThreadPool) {
        this.fileStorageThreadPool = fileStorageThreadPool;
    }

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
        List<FileEntry> extensionDescriptors = validateExtensionDescriptorFileIds(context);
        List<FileEntry> archivePartEntries = getArchivePartEntries(context);
        validateFilesSizeLimit(context, archivePartEntries, extensionDescriptors);

        if (archivePartEntries.size() == 1) {
            getStepLogger().infoWithoutProgressMessage(Messages.ARCHIVE_WAS_NOT_SPLIT_TOTAL_SIZE_IN_BYTES_0, archivePartEntries.get(0)
                                                                                                                               .getSize());
        } else {
            mergeArchive(context, archivePartEntries);
        }
    }

    private List<FileEntry> validateExtensionDescriptorFileIds(ProcessContext context) {
        List<FileEntry> extensionDescriptors = new ArrayList<>();
        String extensionDescriptorFileId = context.getVariable(Variables.EXT_DESCRIPTOR_FILE_ID);

        if (extensionDescriptorFileId == null) {
            return List.of();
        }

        String[] extensionDescriptorFileIds = extensionDescriptorFileId.split(",");
        for (String fileId : extensionDescriptorFileIds) {
            FileEntry file = findFile(context, fileId);
            validateDescriptorSize(file);
            extensionDescriptors.add(file);
        }

        return extensionDescriptors;
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

    private void validateFilesSizeLimit(ProcessContext context, List<FileEntry> archivePartEntries, List<FileEntry> extensionDescriptors) {
        try {
            checkFileSizeOfAllFiles(context, archivePartEntries, extensionDescriptors);
        } catch (FileStorageException e) {
            throw new SLException(e, MessageFormat.format(Messages.ERROR_OCURRED_DURING_VALIDATION_OF_FILES_0, e.getMessage()));
        }

    }

    private void checkFileSizeOfAllFiles(ProcessContext context, List<FileEntry> archivePartEntries, List<FileEntry> extensionDescriptors)
        throws FileStorageException {
        long maxFileSizeLimit = configuration.getMaxUploadSize();
        long sizeOfAllArchivePartEntries = getSizeOfAllFiles(archivePartEntries);
        long sizeOfExtensionDescriptorsEntries = getSizeOfAllFiles(extensionDescriptors);
        long sizeOfAllFileEntries = sizeOfAllArchivePartEntries + sizeOfExtensionDescriptorsEntries;

        getStepLogger().infoWithoutProgressMessage(Messages.SIZE_OF_MTAR_IS_AND_SIZE_OF_EXTENSION_DESCRIPTOR_ID,
                                                   sizeOfAllArchivePartEntries, sizeOfExtensionDescriptorsEntries);

        if (sizeOfAllFileEntries > maxFileSizeLimit) {
            deleteFiles(context, archivePartEntries);
            deleteFiles(context, extensionDescriptors);
            throw new ContentException(Messages.SIZE_OF_ALL_OPERATIONS_FILES_0_EXCEEDS_MAX_UPLOAD_SIZE_1,
                                       sizeOfAllFileEntries,
                                       maxFileSizeLimit);
        }
    }

    private long getSizeOfAllFiles(List<FileEntry> fileEntries) {
        return fileEntries.stream()
                          .mapToLong(fileEntry -> fileEntry.getSize()
                                                           .longValue())
                          .sum();
    }

    private void deleteFiles(ProcessContext context, List<FileEntry> fileEntries) throws FileStorageException {
        FileSweeper fileSweeper = new FileSweeper(context.getVariable(Variables.SPACE_GUID),
                                                  fileService,
                                                  context.getVariable(Variables.CORRELATION_ID));
        fileSweeper.sweep(fileEntries);
    }

    private List<FileEntry> getArchivePartEntries(ProcessContext context) {
        String[] archivePartIds = getArchivePartIds(context);
        if (archivePartIds.length == 1) {
            // TODO The merging of chunks should be done prior to this step
            FileEntry archiveFileEntry = findFile(context, archivePartIds[0]);
            return List.of(archiveFileEntry);
        }
        List<FileEntry> archivePartEntries = getArchivePartEntries(context, archivePartIds);
        context.setVariable(Variables.FILE_ENTRIES, archivePartEntries);

        return archivePartEntries;
    }

    private void mergeArchive(ProcessContext context, List<FileEntry> archivePartEntries) {
        BigInteger archiveSize = calculateArchiveSize(archivePartEntries);
        resilientOperationExecutor.execute(() -> mergeArchive(context, archivePartEntries, archiveSize));
    }

    private void mergeArchive(ProcessContext context, List<FileEntry> archivePartEntries, BigInteger archiveSize) {
        ArchiveStreamWithName archiveStreamWithName = getMergedArchiveStreamCreator(archivePartEntries, archiveSize).createArchiveStream();
        try {
            getStepLogger().infoWithoutProgressMessage(Messages.ARCHIVE_IS_SPLIT_TO_0_PARTS_TOTAL_SIZE_IN_BYTES_1_UPLOADING,
                                                       archivePartEntries.size(), archiveSize);
            FileEntry uploadedArchive = persistArchive(archiveStreamWithName, context, archiveSize);
            context.setVariable(Variables.APP_ARCHIVE_ID, uploadedArchive.getId());
            getStepLogger().infoWithoutProgressMessage(MessageFormat.format(Messages.ARCHIVE_WITH_ID_0_AND_NAME_1_WAS_STORED,
                                                                            uploadedArchive.getId(),
                                                                            archiveStreamWithName.getArchiveName()));
        } finally {
            IOUtils.closeQuietly(archiveStreamWithName.getArchiveStream());
        }
    }

    private String[] getArchivePartIds(ProcessContext context) {
        String archiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        return archiveId.split(",");
    }

    private List<FileEntry> getArchivePartEntries(ProcessContext context, String[] appArchivePartsId) {
        return Arrays.stream(appArchivePartsId)
                     .map(appArchivePartId -> findFile(context, appArchivePartId))
                     .toList();
    }

    private BigInteger calculateArchiveSize(List<FileEntry> archivePartEntries) {
        return archivePartEntries.stream()
                                 .map(FileEntry::getSize)
                                 .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private MergedArchiveStreamCreator getMergedArchiveStreamCreator(List<FileEntry> archivePartEntries, BigInteger archiveSize) {
        return new MergedArchiveStreamCreator(fileService, getStepLogger(), archivePartEntries, Long.parseLong(archiveSize.toString()));
    }

    private FileEntry persistArchive(ArchiveStreamWithName archiveStreamWithName, ProcessContext context, BigInteger size) {
        try {
            return fileStorageThreadPool.submit(new PriorityCallable<>(PriorityFuture.Priority.HIGHEST,
                                                                       () -> doPersistArchive(archiveStreamWithName, context, size)))
                                        .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new SLException(e.getMessage(), e);
        }
    }

    private FileEntry doPersistArchive(ArchiveStreamWithName archiveStreamWithName, ProcessContext context, BigInteger size)
        throws FileStorageException {
        return fileService.addFile(ImmutableFileEntry.builder()
                                                     .name(archiveStreamWithName.getArchiveName())
                                                     .space(context.getVariable(Variables.SPACE_GUID))
                                                     .namespace(context.getVariable(Variables.MTA_NAMESPACE))
                                                     .operationId(context.getExecution()
                                                                         .getProcessInstanceId())
                                                     .size(size)
                                                     .build(),
                                   archiveStreamWithName.getArchiveStream());
    }

}
