package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;

import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class ArchiveMerger {

    private static final String PART_POSTFIX = ".part.";

    private FileService fileService;
    private StepLogger stepLogger;
    private DelegateExecution context;
    private Logger logger;

    public ArchiveMerger(FileService fileService, StepLogger stepLogger, DelegateExecution context, Logger logger) {
        this.fileService = fileService;
        this.stepLogger = stepLogger;
        this.context = context;
        this.logger = logger;
    }

    public void createArchiveFromParts(List<FileEntry> archivePartEntries) {
        List<FileEntry> sortedParts = sort(archivePartEntries);
        String archiveName = getArchiveName(sortedParts.get(0));
        FilePartsMerger archiveMerger = null;
        try {
            archiveMerger = getArchiveMerger(archiveName);
            mergeFileParts(sortedParts, archiveMerger);
            persistMergedArchive(archiveMerger.getMergedFilePath(), context);
        } catch (FileStorageException e) {
            stepLogger.info(Messages.ERROR_MERGING_ARCHIVE);
            throw new SLException(e, Messages.ERROR_PROCESSING_ARCHIVE_PARTS_CONTENT, e.getMessage());
        } catch (IOException e) {
            stepLogger.info(Messages.ERROR_MERGING_ARCHIVE);
            throw new SLException(e, Messages.ERROR_MERGING_ARCHIVE_PARTS, e.getMessage());
        } finally {
            deleteMergedFile(archiveMerger);
        }
    }

    List<FileEntry> sort(List<FileEntry> archivePartEntries) {
        return archivePartEntries.stream()
                                 .sorted(Comparator.comparingInt(this::getEntryIndex))
                                 .collect(Collectors.toList());
    }

    private int getEntryIndex(FileEntry fileEntry) {
        return getEntryIndex(fileEntry.getName()
                                      .substring(fileEntry.getName()
                                                          .lastIndexOf(PART_POSTFIX)
                                          + PART_POSTFIX.length()));
    }

    private int getEntryIndex(String entryIndex) {
        try {
            return Integer.parseInt(entryIndex);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(Messages.INVALID_FILE_ENTRY_NAME);
        }
    }

    private String getArchiveName(FileEntry fileEntry) {
        String fileEntryName = fileEntry.getName();
        return fileEntryName.substring(0, fileEntryName.indexOf(PART_POSTFIX));
    }

    private FilePartsMerger getArchiveMerger(String archiveName) throws IOException {
        return new FilePartsMerger(archiveName);
    }

    private void mergeFileParts(List<FileEntry> sortedParts, FilePartsMerger archiveMerger) throws FileStorageException {
        FileContentProcessor archivePartProcessor = archiveMerger::merge;
        for (FileEntry fileEntry : sortedParts) {
            stepLogger.debug(Messages.MERGING_ARCHIVE_PART, fileEntry.getId(), fileEntry.getName());
            fileService.processFileContent(createFileDownloadProcessor(archivePartProcessor, fileEntry));
        }
    }

    private DefaultFileDownloadProcessor createFileDownloadProcessor(FileContentProcessor archivePartProcessor, FileEntry fileEntry) {
        return new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), fileEntry.getId(), archivePartProcessor);
    }

    private void persistMergedArchive(Path archivePath, DelegateExecution context) throws FileStorageException {
        String name = archivePath.getFileName()
                                 .toString();
        FileEntry uploadedArchive = fileService.addFile(StepsUtil.getSpaceId(context), StepsUtil.getServiceId(context), name,
                                                        archivePath.toFile());
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, uploadedArchive.getId());
    }

    private void deleteMergedFile(FilePartsMerger archiveMerger) {
        if (archiveMerger == null) {
            return;
        }
        tryDeleteMergedFile(archiveMerger);
    }

    private void tryDeleteMergedFile(FilePartsMerger archiveMerger) {
        try {
            Files.deleteIfExists(archiveMerger.getMergedFilePath());
        } catch (IOException e) {
            logger.warn("Merged file not deleted");
        } finally {
            archiveMerger.close();
        }
    }
}