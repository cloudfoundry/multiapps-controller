package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jmx.export.MBeanExporter;

import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.BuildApplicationDeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.BuildCloudDeployModelStep;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessDescriptorStep;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDeployProcessGetter;
import com.sap.cloud.lm.sl.cf.web.monitoring.Metrics;

@Configuration
@ComponentScan(basePackages = "com.sap.cloud.lm.sl.cf")
public class ProcessStepsConfiguration {

	protected static final String PROTOTYPE = "prototype";

	@Bean
	@Scope(PROTOTYPE)
	public BuildCloudDeployModelStep buildCloudDeployModelStep() {
		return new BuildCloudDeployModelStep();
	}

	@Bean
	@Scope(PROTOTYPE)
	public ProcessDescriptorStep processDescriptorStep() {
		return new ProcessDescriptorStep();
	}

	@Bean
	@Scope(PROTOTYPE)
	public BuildApplicationDeployModelStep buildApplicationDeployModelStep() {
		return new BuildApplicationDeployModelStep();
	}

	@Bean
	public ProcessTypeToOperationMetadataMapper operationMetadataMapper() {
		return new ProcessTypeToOperationMetadataMapper();
	}

	@Inject
	@Bean
	public MBeanExporter jmxExporter(Metrics metrics) {
		MBeanExporter mBeanExporter = new MBeanExporter();
		Map<String, Object> beans = new HashMap<>();
		beans.put("com.sap.cloud.lm.sl.cf.web.monitoring:type=Metrics,name=MetricsMBean", metrics);
		mBeanExporter.setBeans(beans);
		return mBeanExporter;
	}

	@Bean
	public ModuleDeployProcessGetter moduleDeployProcessGetter() {
		return new ModuleDeployProcessGetter();
	}

	@Bean
	public ModuleToDeployHelper moduleToDeployHelper() {
		return new ModuleToDeployHelper();
	}
}
