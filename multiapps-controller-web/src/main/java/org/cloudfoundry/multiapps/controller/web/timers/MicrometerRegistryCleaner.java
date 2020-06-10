package org.cloudfoundry.multiapps.controller.web.timers;

import java.util.TimerTask;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MicrometerRegistryCleaner implements RegularTimer {

    @Inject
    protected ApplicationConfiguration configuration;

    @Inject
    private MicrometerNotifier micrometerNotifier;

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerRegistryCleaner.class);

    @Override
    public TimerTask getTimerTask() {
        LOGGER.warn("getTimerTask entering");
        return new TimerTask() {

            @Override
            public void run() {
                LOGGER.info("Micrometer cleaner started");
                micrometerNotifier.clearRegistry();
                LOGGER.info("Micrometer cleaner ended");

            }
        };
    }

    @Override
    public long getDelay() {
        return configuration.getPeriodForMicrometerRegistryCleanup();
    }

    @Override
    public long getPeriod() {
        return configuration.getPeriodForMicrometerRegistryCleanup();
    }

}
