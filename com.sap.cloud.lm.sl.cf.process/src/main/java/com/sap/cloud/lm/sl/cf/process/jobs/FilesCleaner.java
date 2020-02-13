package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named
@Order(20)
public class FilesCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesCleaner.class);

    private final FileService fileService;

    @Inject
    public FilesCleaner(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_FILES_MODIFIED_BEFORE_0, expirationTime));
        try {
            int removedOldFilesCount = fileService.deleteModifiedBefore(expirationTime);
            LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_FILES_0, removedOldFilesCount));
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_FILES_MODIFIED_BEFORE_0, expirationTime);
        }
    }

}
