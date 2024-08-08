package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Comparator;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveStreamWithName;
import org.cloudfoundry.multiapps.controller.process.stream.ImmutableArchiveStreamWithName;
import org.cloudfoundry.multiapps.controller.process.stream.LazyArchiveInputStream;

public class MergedArchiveStreamCreator {

    private static final String PART_POSTFIX = ".part.";

    private final FileService fileService;
    private final StepLogger stepLogger;
    private final List<FileEntry> archiveParts;
    private final int archiveSize;
// i guess that int type for archiveSize is preventing us to deploy application with size bigger than int max value, can we have this data type changed to sth bigger?
    public MergedArchiveStreamCreator(FileService fileService, StepLogger stepLogger, List<FileEntry> archiveParts, int archiveSize) { 
        this.fileService = fileService;
        this.stepLogger = stepLogger;
        this.archiveParts = archiveParts;
        this.archiveSize = archiveSize;
    }

    public ArchiveStreamWithName createArchiveStream() {
        return ImmutableArchiveStreamWithName.builder()
                                             .archiveName(getArchiveName())
                                             .archiveStream(new LazyArchiveInputStream(fileService,
                                                                                       getSortedArchiveParts(),
                                                                                       stepLogger,
                                                                                       archiveSize))
                                             .build();
    }

    List<FileEntry> getSortedArchiveParts() {
        return archiveParts.stream()
                           .sorted(Comparator.comparingInt(this::getArchivePartIndex))
                           .toList();
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

    private String getArchiveName() {
        String archivePartName = archiveParts.get(0)
                                             .getName();
        if (!archivePartName.contains(PART_POSTFIX)) {
            return archivePartName;
        }
        return archivePartName.substring(0, archivePartName.indexOf(PART_POSTFIX));
    }

}
