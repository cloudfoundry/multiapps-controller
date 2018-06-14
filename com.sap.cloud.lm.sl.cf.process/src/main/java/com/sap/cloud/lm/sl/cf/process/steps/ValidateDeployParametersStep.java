package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.util.Configuration;

@Component("validateDeployParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ValidateDeployParametersStep extends SyncActivitiStep {

    private static final String PART_POSTFIX = ".part.";

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.VALIDATING_PARAMETERS);

            validateParameters(execution.getContext());

            getStepLogger().debug(Messages.PARAMETERS_VALIDATED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_VALIDATING_PARAMS);
            throw e;
        }
    }

    private void validateParameters(DelegateExecution context) throws SLException {
        validateStartTimeout(context);
        validateAppArchiveId(context);
        validateExtDescriptorFileId(context);
        validateVersionRule(context);
    }

    private void validateVersionRule(DelegateExecution context) throws SLException {
        String parameter = (String) context.getVariable(Constants.PARAM_VERSION_RULE);
        try {
            VersionRule.valueOf(parameter);
        } catch (IllegalArgumentException e) {
            throw new SLException(e, Messages.ERROR_PARAMETER_1_IS_NOT_VALID_VALID_VALUES_ARE_2, parameter, Constants.PARAM_VERSION_RULE,
                Arrays.asList(VersionRule.values()));
        }
    }

    private void validateExtDescriptorFileId(DelegateExecution context) throws SLException {
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

    private void validateDescriptorSize(FileEntry file) throws SLException {
        Long maxSizeLimit = configuration.getMaxMtaDescriptorSize();
        if (file.getSize()
            .compareTo(BigInteger.valueOf(maxSizeLimit)) > 0) {
            throw new SLException(com.sap.cloud.lm.sl.mta.message.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                file.getSize()
                    .toString(),
                file.getName(), String.valueOf(maxSizeLimit.longValue()));
        }
    }

    private void validateAppArchiveId(DelegateExecution context) throws SLException {
        String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);

        String[] appArchivePartsId = appArchiveId.split(",");
        if (appArchivePartsId.length == 1) {
            findFile(context, appArchiveId);
            return;
        }

        List<FileEntry> archivePartEntries = new ArrayList<>();
        for (String appArchivePartId : appArchivePartsId) {
            archivePartEntries.add(findFile(context, appArchivePartId));
        }
        try {
            getStepLogger().debug(Messages.BUILDING_ARCHIVE_FROM_PARTS);
            createArchiveFromParts(context, archivePartEntries);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.ERROR_PROCESSING_ARCHIVE_PARTS_CONTENT, e.getMessage());
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_MERGING_ARCHIVE_PARTS, e.getMessage());
        }
    }

    private void createArchiveFromParts(DelegateExecution context, List<FileEntry> archivePartEntries)
        throws FileStorageException, IOException {
        List<FileEntry> sortedParts = sort(archivePartEntries);
        String archiveName = getArchiveName(sortedParts.get(0));
        FilePartsMerger archiveMerger = getArchiveMerger(archiveName);
        FileContentProcessor archivePartProcessor = appArchivePartInputStream -> archiveMerger.merge(appArchivePartInputStream);
        try {
            for (FileEntry fileEntry : sortedParts) {
                getStepLogger().debug(Messages.MERGING_ARCHIVE_PART, fileEntry.getId(), fileEntry.getName());
                fileService.processFileContent(
                    new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), fileEntry.getId(), archivePartProcessor));
                attemptToDeleteFilePart(fileEntry);
            }
        } finally {
            deleteRemainingFileParts(sortedParts);
            archiveMerger.close();
        }

        try {
            persistMergedArchive(archiveMerger.getMergedFilePath(), context);
        } finally {
            Files.deleteIfExists(archiveMerger.getMergedFilePath());
        }
    }

    private void deleteRemainingFileParts(List<FileEntry> sortedParts) {
        for (FileEntry fileEntry : sortedParts) {
            attemptToDeleteFilePart(fileEntry);
        }
    }

    private void attemptToDeleteFilePart(FileEntry fileEntry) {
        try {
            fileService.deleteFile(fileEntry.getSpace(), fileEntry.getId());
        } catch (FileStorageException e) {
            LOGGER.warn(Messages.ERROR_DELETING_ARCHIVE_PARTS_CONTENT, e);
        }
    }

    protected FilePartsMerger getArchiveMerger(String archiveName) throws IOException {
        return new FilePartsMerger(archiveName);
    }

    private String getArchiveName(FileEntry fileEntry) {
        String fileEntryName = fileEntry.getName();
        return fileEntryName.substring(0, fileEntryName.indexOf(PART_POSTFIX));
    }

    private void persistMergedArchive(Path archivePath, DelegateExecution context) throws FileStorageException, IOException {
        Configuration fileConfiguration = configuration.getFileConfiguration();
        String name = archivePath.getFileName()
            .toString();
        FileEntry uploadedArchive = fileService.addFile(StepsUtil.getSpaceId(context), StepsUtil.getServiceId(context), name,
            fileConfiguration.getFileUploadProcessor(), archivePath.toFile());
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private List<FileEntry> sort(List<FileEntry> archivePartEntries) {
        return archivePartEntries.stream()
            .sorted((FileEntry entry1, FileEntry entry2) -> {
                String entry1IndexString = entry1.getName()
                    .substring(entry1.getName()
                        .indexOf(PART_POSTFIX) + PART_POSTFIX.length());
                String entry2IndexString = entry2.getName()
                    .substring(entry2.getName()
                        .indexOf(PART_POSTFIX) + PART_POSTFIX.length());
                int entry1Index = Integer.parseInt(entry1IndexString);
                int entry2Index = Integer.parseInt(entry2IndexString);
                return Integer.compare(entry1Index, entry2Index);
            })
            .collect(Collectors.toList());
    }

    private FileEntry findFile(DelegateExecution context, String fileId) throws SLException {
        try {
            FileEntry fileEntry = fileService.getFile(StepsUtil.getSpaceId(context), fileId);
            if (fileEntry == null) {
                throw new SLException(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0, fileId);
            }
            return fileEntry;
        } catch (FileStorageException e) {
            throw new SLException(e, "Failed to retrieve file with id \"{0}\"", fileId);
        }
    }

    private void validateStartTimeout(DelegateExecution context) throws SLException {
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
