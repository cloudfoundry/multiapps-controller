package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component
@Order(40)
public class FilesCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesCleaner.class);

    private final AbstractFileService fileService;

    @Inject
    public FilesCleaner(AbstractFileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(format("Deleting old MTA files modified before: {0}", expirationTime));
        try {
            int removedOldFilesCount = fileService.deleteByModificationTime(expirationTime);
            LOGGER.info(format("Deleted old MTA files: {0}", removedOldFilesCount));
        } catch (FileStorageException e) {
            throw new SLException(e, "Deletion of old MTA files failed");
        }
    }

}
