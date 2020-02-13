package com.sap.cloud.lm.sl.cf.process.jobs;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.core.security.data.termination.DataTerminationService;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named
@Order(10)
public class UserDataCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataCleaner.class);

    @Inject
    private DataTerminationService dataTerminationService;

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, Messages.DELETING_DATA_FOR_NON_EXISTING_USERS);
        dataTerminationService.deleteOrphanUserData();
        LOGGER.info(CleanUpJob.LOG_MARKER, Messages.DELETED_DATA_FOR_NON_EXISTING_USERS);
    }

}
