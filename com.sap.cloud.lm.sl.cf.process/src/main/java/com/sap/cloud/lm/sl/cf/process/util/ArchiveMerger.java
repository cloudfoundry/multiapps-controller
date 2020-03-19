package com.sap.cloud.lm.sl.cf.process.util;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class ArchiveMerger {

    private static final String PART_POSTFIX = ".part.";

    private final FileService fileService;
    private final StepLogger stepLogger;
    private final DelegateExecution execution;

    public ArchiveMerger(FileService fileService, StepLogger stepLogger, DelegateExecution execution) {
        this.fileService = fileService;
        this.stepLogger = stepLogger;
        this.execution = execution;
    }

    public Path createArchiveFromParts(List<FileEntry> archiveParts) {
        List<FileEntry> sortedArchiveParts = sort(archiveParts);
        String archiveName = getArchiveName(sortedArchiveParts.get(0));
        try (FilePartsMerger filePartsMerger = new FilePartsMerger(archiveName)) {
            mergeArchiveParts(sortedArchiveParts, filePartsMerger);
            return filePartsMerger.getMergedFilePath();
        }
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

    private void mergeArchiveParts(List<FileEntry> sortedArchiveParts, FilePartsMerger filePartsMerger) {
        try {
            mergeFileParts(sortedArchiveParts, filePartsMerger);
        } catch (Exception e) {
            stepLogger.info(Messages.ERROR_MERGING_ARCHIVE);
            filePartsMerger.cleanUp();
            throw new SLException(e, Messages.ERROR_MERGING_ARCHIVE_PARTS, e.getMessage());
        }
    }

    private void mergeFileParts(List<FileEntry> sortedArchiveParts, FilePartsMerger filePartsMerger) throws FileStorageException {
        FileContentProcessor archivePartProcessor = filePartsMerger::merge;
        for (FileEntry archivePart : sortedArchiveParts) {
            stepLogger.debug(Messages.MERGING_ARCHIVE_PART, archivePart.getId(), archivePart.getName());
            fileService.processFileContent(StepsUtil.getSpaceId(execution), archivePart.getId(), archivePartProcessor);
        }
    }

}