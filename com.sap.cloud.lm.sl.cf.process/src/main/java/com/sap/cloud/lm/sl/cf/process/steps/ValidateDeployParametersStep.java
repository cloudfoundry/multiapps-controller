package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;
import com.sap.cloud.lm.sl.slp.resources.Configuration;

@Component("validateDeployParametersStep")
public class ValidateDeployParametersStep extends AbstractXS2ProcessStep {

    private static final String PART_POSTFIX = ".part.";

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateDeployParametersStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("validateParametersTask", "Validate Parameters", "Validate Parameters");
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.VALIDATING_PARAMETERS, LOGGER);

            validateParameters(context);

            debug(context, Messages.PARAMETERS_VALIDATED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_VALIDATING_PARAMS, e, LOGGER);
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
        Long maxSizeLimit = ConfigurationUtil.getMaxMtaDescriptorSize();
        if (file.getSize().compareTo(BigInteger.valueOf(maxSizeLimit)) > 0) {
            throw new SLException(com.sap.cloud.lm.sl.mta.message.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                file.getSize().toString(), file.getName(), String.valueOf(maxSizeLimit.longValue()));
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
            debug(context, Messages.BUILDING_ARCHIVE_FROM_PARTS, LOGGER);
            createArchiveFromParts(context, archivePartEntries);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.ERROR_PROCESSING_ARCHIVE_PARTS_CONTENT);
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_MERGING_ARCHIVE_PARTS);
        }
    }

    private void createArchiveFromParts(DelegateExecution context, List<FileEntry> archivePartEntries)
        throws FileStorageException, IOException {
        List<FileEntry> sortedParts = sort(archivePartEntries);
        String archiveName = getArchiveName(sortedParts.get(0));
        FilePartsMerger archiveMerger = getArchiveMerger(archiveName);
        FileContentProcessor archivePartProcessor = (appArchivePartInputStream) -> {
            archiveMerger.merge(appArchivePartInputStream);
        };
        try {
            for (FileEntry fileEntry : sortedParts) {
                fileService.processFileContent(
                    new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), fileEntry.getId(), archivePartProcessor));
            }
        } finally {
            archiveMerger.close();
            try {
                deleteParts(sortedParts);
            } catch (FileStorageException e) {
                LOGGER.error(Messages.ERROR_DELETING_ARCHIVE_PARTS_CONTENT, e);
            }
        }
        persistMergedArchive(archiveMerger.getMergedFilePath(), context);
    }

    protected FilePartsMerger getArchiveMerger(String archiveName) throws IOException {
        return new FilePartsMerger(archiveName);
    }

    private void deleteParts(List<FileEntry> fileEntries) throws FileStorageException {
        for (FileEntry fileEntry : fileEntries) {
            fileService.deleteFile(fileEntry.getSpace(), fileEntry.getId());
        }
    }

    private String getArchiveName(FileEntry fileEntry) {
        String fileEntryName = fileEntry.getName();
        return fileEntryName.substring(0, fileEntryName.indexOf(PART_POSTFIX));
    }

    private void persistMergedArchive(Path archivePath, DelegateExecution context) throws FileStorageException, IOException {
        Configuration configuration = ConfigurationUtil.getSlpConfiguration();
        String name = archivePath.getFileName().toString();
        FileEntry uploadedArchive = fileService.addFile(StepsUtil.getSpaceId(context), StepsUtil.getServiceId(context), name,
            configuration.getFileUploadProcessor(name), Files.newInputStream(archivePath));
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private List<FileEntry> sort(List<FileEntry> archivePartEntries) {
        return archivePartEntries.stream().sorted((FileEntry entry1, FileEntry entry2) -> {
            String entry1IndexString = entry1.getName().substring(entry1.getName().indexOf(PART_POSTFIX) + PART_POSTFIX.length());
            String entry2IndexString = entry2.getName().substring(entry2.getName().indexOf(PART_POSTFIX) + PART_POSTFIX.length());
            int entry1Index = Integer.parseInt(entry1IndexString);
            int entry2Index = Integer.parseInt(entry2IndexString);
            return Integer.compare(entry1Index, entry2Index);
        }).collect(Collectors.toList());
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
            throw new SLException(
                MessageFormat.format(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, startTimeout, Constants.PARAM_START_TIMEOUT));
        }
    }

}
