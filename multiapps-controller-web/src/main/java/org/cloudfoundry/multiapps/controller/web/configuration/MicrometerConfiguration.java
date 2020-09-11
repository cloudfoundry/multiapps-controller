package org.cloudfoundry.multiapps.controller.web.configuration;

import java.text.MessageFormat;
import java.time.Duration;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

@Configuration
public class MicrometerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerConfiguration.class);
    public static final String DYNATRACE_SERVICE_NAME = "deploy-service-dynatrace";
    private static final String CLIENT_CONNECTIONS_METRICS_PREFIX = "reactor.netty.connection.provider.cloudfoundry-client.";

    @Inject
    @Bean
    public JmxMeterRegistry jmxMeterRegistry(ApplicationConfiguration appConfig) {
        // Skip creation of registry when Dynatrace is not bound
        if (!dynatraceExist()) {
            LOGGER.info("Skipping registering JmxMeterRegistry");
            return null;
        }
        Integer stepInSeconds = appConfig.getMicrometerStepInSeconds();

        JmxConfig jmxConfig = new JmxConfig() {

            @Override
            public Duration step() {
                if (stepInSeconds == null) {
                    return Duration.ofSeconds(15);
                }
                return Duration.ofSeconds(stepInSeconds);
            }

            @Override
            public String get(String k) {
                return null;
            }
        };
        JmxMeterRegistry registry = new JmxMeterRegistry(jmxConfig, Clock.SYSTEM);
        registry.config()
                .meterFilter(MeterFilter.acceptNameStartsWith(CLIENT_CONNECTIONS_METRICS_PREFIX))
                .meterFilter(MeterFilter.deny());
        Metrics.globalRegistry.add(registry);
        return registry;
    }

    private boolean dynatraceExist() {
        try {
            CloudFactory cloudFactory = new CloudFactory();
            Cloud cloud = cloudFactory.getCloud();
            cloud.getServiceInfo(DYNATRACE_SERVICE_NAME);
            LOGGER.info("Dynatrace service instance found");
            return true;
        } catch (Exception e) {
            LOGGER.warn(MessageFormat.format("Failed to detect service info for service \"{0}\"!", DYNATRACE_SERVICE_NAME), e);
        }
        return false;
    }

}
