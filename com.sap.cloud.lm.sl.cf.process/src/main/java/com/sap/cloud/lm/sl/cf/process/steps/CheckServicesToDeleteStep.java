package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;

@Component("checkServicesToDeleteStep")
public class CheckServicesToDeleteStep extends CheckForOperationsInProgressStep {

    @Override
    protected List<CloudServiceExtended> getServicesToProcess(ExecutionWrapper execution) {
        List<CloudServiceExtended> services = new ArrayList<>();
        StepsUtil.getServicesToDelete(execution.getContext())
            .forEach(serviceName -> services.add(ImmutableCloudServiceExtended.builder()
                .name(serviceName)
                .build()));
        return services;
    }

}
