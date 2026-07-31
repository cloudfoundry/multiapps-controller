package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.monitoring.Metrics;
import org.cloudfoundry.multiapps.controller.web.monitoring.UploadDurationMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

@Configuration
public class JmxConfiguration {

    private static final String METRICS_BEAN = "org.cloudfoundry.multiapps.controller.web.monitoring:type=Metrics,name=MetricsMBean";

    private static final String UPLOAD_METRICS_BEAN = "org.cloudfoundry.multiapps.controller.web.monitoring:type=Metrics,name=UploadMetricsMBean";

    @Bean
    public MBeanExporter jmxExporter(Metrics metrics, UploadDurationMetrics uploadDurationMetrics) {
        MBeanExporter mBeanExporter = new MBeanExporter();
        mBeanExporter.setBeans(Map.of(METRICS_BEAN, metrics, UPLOAD_METRICS_BEAN, uploadDurationMetrics));
        return mBeanExporter;
    }
}
