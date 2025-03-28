package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Order(20)
public class BackupDescriptorsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupDescriptorsCleaner.class);

    private final DescriptorBackupService descriptorBackupService;

    @Inject
    public BackupDescriptorsCleaner(DescriptorBackupService descriptorBackupService) {
        this.descriptorBackupService = descriptorBackupService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETING_BACKUP_DESCRIPTORS_STORED_BEFORE_0, expirationTime));

        int removedBackupDescriptors = descriptorBackupService.createQuery()
                                                              .olderThan(expirationTime)
                                                              .delete();

        LOGGER.debug(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETED_BACKUP_DESCRIPTORS_0, removedBackupDescriptors));
    }

}
