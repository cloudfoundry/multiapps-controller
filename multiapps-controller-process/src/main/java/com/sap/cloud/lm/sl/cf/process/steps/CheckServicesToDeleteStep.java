package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("checkServicesToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServicesToDeleteStep extends CheckForOperationsInProgressStep {

    @Override
    protected List<CloudServiceInstanceExtended> getServicesToProcess(ProcessContext context) {
        List<String> servicesToDelete = context.getVariable(Variables.SERVICES_TO_DELETE);
        return servicesToDelete.stream()
                               .map(this::buildCloudServiceExtended)
                               .collect(Collectors.toList());
    }

    private CloudServiceInstanceExtended buildCloudServiceExtended(String serviceName) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .build();
    }

}
