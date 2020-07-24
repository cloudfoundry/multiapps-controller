package org.cloudfoundry.multiapps.controller.web.configuration;

import java.text.MessageFormat;
import java.time.Duration;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.web.configuration.service.DynatraceServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.DynatraceServiceInfoCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.dynatrace.DynatraceConfig;
import io.micrometer.dynatrace.DynatraceMeterRegistry;

@Configuration
public class MicrometerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerConfiguration.class);
    private static final String CLIENT_CONNECTIONS_METRICS_PREFIX = "reactor.netty.connection.provider.cloudfoundry-client.";

    @Inject
    @Bean
    public DynatraceMeterRegistry dynatraceMeterRegistry(ApplicationConfiguration appConfig) {
        DynatraceServiceInfo serviceInfo = getDynatraceServiceInfo();
        String dynatraceUrl = getDynatraceUrl(serviceInfo);
        String dynatraceToken = getDynatraceToken(serviceInfo);
        if (dynatraceUrl == null || dynatraceToken == null) {
            return null;
        }
        Integer stepInSeconds = appConfig.getMicrometerStepInSeconds();
        Integer batchSize = appConfig.getMicrometerBatchSize();

        DynatraceConfig dynatraceConfig = new DynatraceConfig() {
            @Override
            public String apiToken() {
                return dynatraceToken;
            }

            @Override
            public String uri() {
                return dynatraceUrl;
            }

            @Override
            public String deviceId() {
                return "micrometer";
            }

            @Override
            public String technologyType() {
                return "micrometer";
            }

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Duration step() {
                if (stepInSeconds == null) {
                    return Duration.ofSeconds(15);
                }
                return Duration.ofSeconds(stepInSeconds);
            }

            @Override
            public int batchSize() {
                if (batchSize == null) {
                    return DynatraceConfig.super.batchSize();
                }
                return batchSize;
            }

            @Override
            @Nullable
            public String get(String k) {
                return null;
            }
        };
        DynatraceMeterRegistry registry = new DynatraceMeterRegistry(dynatraceConfig, Clock.SYSTEM);
        registry.config()
                .meterFilter(MeterFilter.acceptNameStartsWith(CLIENT_CONNECTIONS_METRICS_PREFIX))
                .meterFilter(MeterFilter.deny());
        Metrics.globalRegistry.add(registry);
        return registry;
    }

    private String getDynatraceToken(DynatraceServiceInfo serviceInfo) {
        if(serviceInfo == null) {
            return null;
        }
        return serviceInfo.getMicrometerToken();
    }

    private String getDynatraceUrl(DynatraceServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return null;
        }
        return serviceInfo.getMicrometerUrl();
    }

    private DynatraceServiceInfo getDynatraceServiceInfo() {
        String serviceName = DynatraceServiceInfoCreator.DEFAULT_DYNATRACE_SERVICE_ID;
        try {
            CloudFactory cloudFactory = new CloudFactory();
            Cloud cloud = cloudFactory.getCloud();
            ServiceInfo serviceInfo = cloud.getServiceInfo(serviceName);
            if (serviceInfo instanceof DynatraceServiceInfo) {
                return (DynatraceServiceInfo) serviceInfo;
            }
            LOGGER.warn("Service instance did not match allowed name and type.");
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format("Failed to detect service info for service \"{0}\"!", serviceName), e);
        }
        return null;
    }

}
