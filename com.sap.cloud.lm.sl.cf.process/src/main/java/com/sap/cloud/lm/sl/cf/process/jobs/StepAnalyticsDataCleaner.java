package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.core.persistence.service.StepAnalyticsDataService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named
@Order(20)
public class StepAnalyticsDataCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepAnalyticsDataCleaner.class);

    private StepAnalyticsDataService stepAnalyticsDataService;

    @Inject
    public StepAnalyticsDataCleaner(StepAnalyticsDataService stepAnalyticsDataService) {
        this.stepAnalyticsDataService = stepAnalyticsDataService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_STEP_ANALYTICS_DATA_BEFORE_0, expirationTime));
        int removedStepAnalyticsDataEntries = stepAnalyticsDataService.createQuery()
                                                                      .olderThan(expirationTime)
                                                                      .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_STEP_ANALYTICS_DATA_ENTRIES_0, removedStepAnalyticsDataEntries));
    }

}
