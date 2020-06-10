package org.cloudfoundry.multiapps.controller.web.configuration;

import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.processes.metering.EmptyMircrometerNotifier;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerNotifier;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerNotifierImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.dynatrace.DynatraceMeterRegistry;

@Configuration
public class MicrometerNotifierConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerNotifierConfiguration.class);

    @Bean
    public MicrometerNotifier micrometerNotifier(@Autowired(required = false) DynatraceMeterRegistry dynatraceMeterRegistry,
                                                 ProcessTypeParser processTypeParser) {
        LOGGER.warn("dynatraceMeterRegistry is: " + dynatraceMeterRegistry);
        if (dynatraceMeterRegistry == null) {
            return new EmptyMircrometerNotifier();
        }
        return new MicrometerNotifierImpl(dynatraceMeterRegistry, processTypeParser);
    }
    
}
