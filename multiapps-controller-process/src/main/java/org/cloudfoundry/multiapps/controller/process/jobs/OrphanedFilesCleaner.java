package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class OrphanedFilesCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedFilesCleaner.class);
    private static final int SELECTED_INSTANCE_FOR_CLEAN_UP = 1;
    private final FileService fileService;
    private final AsyncUploadJobService asyncUploadJobService;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public OrphanedFilesCleaner(FileService fileService, AsyncUploadJobService asyncUploadJobService,
                                ApplicationConfiguration applicationConfiguration) {
        this.fileService = fileService;
        this.asyncUploadJobService = asyncUploadJobService;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void clean() {
        if (applicationConfiguration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEAN_UP) {
            return;
        }
        var currentTime = LocalDateTime.now();
        var twoHoursAgo = currentTime.minusHours(2);
        var oneHourAgo = currentTime.minusHours(1);
        try {
            List<FileEntry> entriesToDelete = fileService.listFilesCreatedAfterAndBeforeWithoutOperationId(twoHoursAgo, oneHourAgo);
            LOGGER.info(MessageFormat.format(Messages.DELETING_THE_FOLLOWING_FILE_ENTRIES_WITHOUT_CONTENT_0, entriesToDelete));
            List<String> fileIds = entriesToDelete.stream()
                                                  .map(FileEntry::getId)
                                                  .collect(Collectors.toList());
            int deleteFiles = fileService.deleteFilesByIds(fileIds);
            LOGGER.info(MessageFormat.format(Messages.DELETED_FILE_ENTRIES_0, deleteFiles));
            int deletedAsyncJobs = asyncUploadJobService.createQuery()
                                                        .withFileIds(fileIds)
                                                        .delete();
            LOGGER.info(MessageFormat.format(Messages.DELETED_FILE_UPLOAD_JOBS_0, deletedAsyncJobs));
        } catch (Exception e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_ORPHANED_FILES_MODIFIED_AFTER_0_AND_BEFORE_1, twoHoursAgo, oneHourAgo);
        }
    }
}
