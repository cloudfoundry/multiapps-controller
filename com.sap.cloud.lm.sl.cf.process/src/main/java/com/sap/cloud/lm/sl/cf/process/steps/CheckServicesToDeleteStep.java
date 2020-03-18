package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("checkServicesToDeleteStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServicesToDeleteStep extends CheckForOperationsInProgressStep {

    @Override
    protected List<CloudServiceExtended> getServicesToProcess(ExecutionWrapper execution) {
        List<String> servicesToDelete = execution.getVariable(Variables.SERVICES_TO_DELETE);
        return servicesToDelete.stream()
                               .map(this::buildCloudServiceExtended)
                               .collect(Collectors.toList());
    }

    private CloudServiceExtended buildCloudServiceExtended(String serviceName) {
        return ImmutableCloudServiceExtended.builder()
                                            .name(serviceName)
                                            .build();
    }

}
