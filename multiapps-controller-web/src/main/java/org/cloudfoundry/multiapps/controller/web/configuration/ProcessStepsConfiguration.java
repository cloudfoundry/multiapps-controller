package org.cloudfoundry.multiapps.controller.web.configuration;

import org.cloudfoundry.multiapps.controller.process.steps.ProcessMtaArchiveStep;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDeployProcessGetter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ProcessStepsConfiguration {

    @Bean("processMtaArchiveStep")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ProcessMtaArchiveStep buildProcessMtaArchiveStep() {
        return new ProcessMtaArchiveStep();
    }

    @Bean("moduleDeployProcessGetter")
    @Qualifier("moduleDeployProcessGetter")
    public ModuleDeployProcessGetter moduleDeployProcessGetter() {
        return new ModuleDeployProcessGetter();
    }
}
