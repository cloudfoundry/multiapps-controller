package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jmx.export.MBeanExporter;

import com.sap.cloud.lm.sl.cf.process.steps.BuildApplicationDeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.BuildCloudDeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessDescriptorStep;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDeployProcessGetter;
import com.sap.cloud.lm.sl.cf.web.monitoring.Metrics;

@Configuration
public class ProcessStepsConfiguration {

    private static final String METRICS_BEAN = "com.sap.cloud.lm.sl.cf.web.monitoring:type=Metrics,name=MetricsMBean";

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public BuildCloudDeployModelStep buildCloudDeployModelStep() {
        return new BuildCloudDeployModelStep();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ProcessDescriptorStep processDescriptorStep() {
        return new ProcessDescriptorStep();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public BuildApplicationDeployModelStep buildApplicationDeployModelStep() {
        return new BuildApplicationDeployModelStep();
    }

    @Inject
    @Bean
    public MBeanExporter jmxExporter(Metrics metrics) {
        MBeanExporter mBeanExporter = new MBeanExporter();
        Map<String, Object> beans = new HashMap<>();
        beans.put(METRICS_BEAN, metrics);
        mBeanExporter.setBeans(beans);
        return mBeanExporter;
    }

    @Bean
    public ModuleDeployProcessGetter moduleDeployProcessGetter() {
        return new ModuleDeployProcessGetter();
    }
}
