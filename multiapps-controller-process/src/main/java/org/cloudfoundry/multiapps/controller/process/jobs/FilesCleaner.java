package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.FileDeletionResult;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import static java.text.MessageFormat.format;

@Named
@Order(20)
public class FilesCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesCleaner.class);

    private final FileService fileService;
    private final AsyncUploadJobService uploadJobService;

    @Inject
    public FilesCleaner(FileService fileService, AsyncUploadJobService uploadJobService) {
        this.fileService = fileService;
        this.uploadJobService = uploadJobService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_FILES_MODIFIED_BEFORE_0, expirationTime));
        try {
            FileDeletionResult removedOldFilesCounts = fileService.deleteModifiedBefore(expirationTime);
            if (removedOldFilesCounts.deletedFromObjectStore() > 0) {
                LOGGER.info(CleanUpJob.LOG_MARKER,
                            format(Messages.DELETED_ACTUAL_FILES_FROM_OBJECT_STORE_0_AND_DELETED_FILE_ATTRIBUTES_ENTRIES_FROM_DATABASE_1,
                                   removedOldFilesCounts.deletedFromObjectStore(), removedOldFilesCounts.deletedFromDatabase()));
            } else {
                LOGGER.info(CleanUpJob.LOG_MARKER,
                            format(Messages.DELETED_ACTUAL_FILES_FROM_DATABASE_0, removedOldFilesCounts.deletedFromDatabase()));
            }

            int deletedJobs = uploadJobService.createQuery()
                                              .addedBefore(expirationTime)
                                              .delete();
            LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_FILE_UPLOAD_JOBS_0, deletedJobs));
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_FILES_MODIFIED_BEFORE_0, expirationTime);
        }
    }

}
