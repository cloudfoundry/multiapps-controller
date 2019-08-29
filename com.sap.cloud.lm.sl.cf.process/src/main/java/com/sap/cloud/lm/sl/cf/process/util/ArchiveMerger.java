package com.sap.cloud.lm.sl.cf.process.util;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class ArchiveMerger {

    private static final String PART_POSTFIX = ".part.";

    private FileService fileService;
    private StepLogger stepLogger;
    private DelegateExecution context;

    public ArchiveMerger(FileService fileService, StepLogger stepLogger, DelegateExecution context) {
        this.fileService = fileService;
        this.stepLogger = stepLogger;
        this.context = context;
    }

    public Path createArchiveFromParts(List<FileEntry> archiveParts) {
        List<FileEntry> sortedArchiveParts = sort(archiveParts);
        String archiveName = getArchiveName(sortedArchiveParts.get(0));
        FilePartsMerger filePartsMerger = null;
        try {
            filePartsMerger = new FilePartsMerger(archiveName);
            mergeFileParts(sortedArchiveParts, filePartsMerger);
        } catch (Exception e) {
            stepLogger.info(Messages.ERROR_MERGING_ARCHIVE);
            cleanUp(filePartsMerger);
            throw new SLException(e, Messages.ERROR_MERGING_ARCHIVE_PARTS, e.getMessage());
        } finally {
            closeFilePartsMerger(filePartsMerger);
        }
        return filePartsMerger.getMergedFilePath();
    }

    List<FileEntry> sort(List<FileEntry> archiveParts) {
        return archiveParts.stream()
                           .sorted(Comparator.comparingInt(this::getArchivePartIndex))
                           .collect(Collectors.toList());
    }

    private int getArchivePartIndex(FileEntry archivePart) {
        try {
            String archivePartName = archivePart.getName();
            String archivePartPostfix = archivePartName.substring(archivePartName.lastIndexOf(PART_POSTFIX) + PART_POSTFIX.length());
            return Integer.parseInt(archivePartPostfix);
        } catch (NumberFormatException e) {
            throw new SLException(e, Messages.INVALID_FILE_ENTRY_NAME, archivePart.getName());
        }
    }

    private String getArchiveName(FileEntry archivePart) {
        String archivePartName = archivePart.getName();
        if (!archivePartName.contains(PART_POSTFIX)) {
            return archivePartName;
        }
        return archivePartName.substring(0, archivePartName.indexOf(PART_POSTFIX));
    }

    private void mergeFileParts(List<FileEntry> sortedArchiveParts, FilePartsMerger filePartsMerger) throws FileStorageException {
        FileContentProcessor archivePartProcessor = filePartsMerger::merge;
        for (FileEntry archivePart : sortedArchiveParts) {
            stepLogger.debug(Messages.MERGING_ARCHIVE_PART, archivePart.getId(), archivePart.getName());
            fileService.processFileContent(createFileDownloadProcessor(archivePartProcessor, archivePart));
        }
    }

    private DefaultFileDownloadProcessor createFileDownloadProcessor(FileContentProcessor archivePartProcessor, FileEntry archivePart) {
        return new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), archivePart.getId(), archivePartProcessor);
    }

    private void cleanUp(FilePartsMerger filePartsMerger) {
        File mergedArchive = toFile(filePartsMerger.getMergedFilePath());
        FileUtils.deleteQuietly(mergedArchive);
    }

    private File toFile(Path path) {
        return path != null ? path.toFile() : null;
    }

    private void closeFilePartsMerger(FilePartsMerger filePartsMerger) {
        if (filePartsMerger != null) {
            filePartsMerger.close();
        }
    }
}