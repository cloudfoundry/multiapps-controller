package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

@Configuration
@ImportAutoConfiguration({
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        ServletManagementContextAutoConfiguration.class,
        ManagementContextAutoConfiguration.class,

        HealthContributorAutoConfiguration.class,

        InfoEndpointAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,

        HeapDumpWebEndpointAutoConfiguration.class,
        ThreadDumpEndpointAutoConfiguration.class,
        LoggersEndpointAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class,
})@EnableConfigurationProperties(CorsEndpointProperties.class)
class ActuatorConfiguration {

    @Bean
    public WebMvcEndpointHandlerMapping
           webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
                                            org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
                                            org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
                                            EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
                                            WebEndpointProperties webEndpointProperties, Environment environment) {
        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
        return new WebMvcEndpointHandlerMapping(endpointMapping,
                                                webEndpoints,
                                                endpointMediaTypes,
                                                corsProperties.toCorsConfiguration(),
                                                new EndpointLinksResolver(allEndpoints, basePath),
                                                shouldRegisterLinksMapping);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment, String basePath) {
        return webEndpointProperties.getDiscovery()
                                    .isEnabled()
            && (StringUtils.hasText(basePath) || ManagementPortType.get(environment)
                                                                   .equals(ManagementPortType.DIFFERENT));
    }

    @Bean(name = "springEnvironment")
    public Environment environment() {
        return new StandardEnvironment();
    }

    @Bean
    public DispatcherServletPath dispatcherServletPath() {
        return () -> "/";
    }

    @Bean
    public HealthIndicator sampleHealthIndicator() {
        return Health.up()
                     .withDetail("info", "Sample Health")::build;
    }

}