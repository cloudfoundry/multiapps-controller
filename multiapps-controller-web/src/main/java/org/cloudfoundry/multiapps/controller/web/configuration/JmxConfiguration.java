package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.web.monitoring.Metrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

@Configuration
public class JmxConfiguration {

    private static final String METRICS_BEAN = "org.cloudfoundry.multiapps.controller.web.monitoring:type=Metrics,name=MetricsMBean";

    @Inject
    @Bean
    public MBeanExporter jmxExporter(Metrics metrics) {
        MBeanExporter mBeanExporter = new MBeanExporter();
        mBeanExporter.setBeans(Map.of(METRICS_BEAN, metrics));
        return mBeanExporter;
    }
}
