package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.process.steps.ProcessMtaArchiveStep;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDeployProcessGetter;
import org.cloudfoundry.multiapps.controller.web.monitoring.Metrics;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jmx.export.MBeanExporter;

@Configuration
public class ProcessStepsConfiguration {

    private static final String METRICS_BEAN = "org.cloudfoundry.multiapps.controller.web.monitoring:type=Metrics,name=MetricsMBean";
    private static final String DATASOURCE_BEAN = "org.cloudfoundry.multiapps.controller.web.monitoring:type=DataSource,name=DataSourceMBean";

    @Bean("processMtaArchiveStep")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ProcessMtaArchiveStep buildProcessMtaArchiveStep() {
        return new ProcessMtaArchiveStep();
    }

    @Inject
    @Bean
    public MBeanExporter jmxExporter(Metrics metrics, DataSource dataSource) {
        MBeanExporter mBeanExporter = new MBeanExporter();
        Map<String, Object> beans = new HashMap<>();
        beans.put(METRICS_BEAN, metrics);
        if (dataSource instanceof DelegatingDataSource) {
            dataSource = ((DelegatingDataSource) dataSource).getTargetDataSource();
        }
        beans.put(DATASOURCE_BEAN, dataSource);
        mBeanExporter.setBeans(beans);
        return mBeanExporter;
    }

    @Bean
    public ModuleDeployProcessGetter moduleDeployProcessGetter() {
        return new ModuleDeployProcessGetter();
    }
}
