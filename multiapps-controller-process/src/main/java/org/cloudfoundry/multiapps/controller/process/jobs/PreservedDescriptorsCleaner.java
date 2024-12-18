package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(20)
public class PreservedDescriptorsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreservedDescriptorsCleaner.class);

    private final DescriptorPreserverService descriptorPreserverService;

    @Inject
    public PreservedDescriptorsCleaner(DescriptorPreserverService descriptorPreserverService) {
        this.descriptorPreserverService = descriptorPreserverService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETING_PRESERVED_DESCRIPTORS_STORED_BEFORE_0, expirationTime));

        int removedPreservedDescriptors = descriptorPreserverService.createQuery()
                                                                    .olderThan(expirationTime)
                                                                    .delete();

        LOGGER.debug(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETED_PRESERVED_DESCRIPTORS_0, removedPreservedDescriptors));
    }

}
