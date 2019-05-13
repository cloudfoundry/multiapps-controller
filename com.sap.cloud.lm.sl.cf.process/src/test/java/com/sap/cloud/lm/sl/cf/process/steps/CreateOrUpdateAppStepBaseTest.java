package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;

public abstract class CreateOrUpdateAppStepBaseTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    protected StepInput stepInput;
    protected CloudApplicationExtended application;
    
    protected static class StepInput {
        List<CloudApplicationExtended> applications = Collections.emptyList();
        List<SimpleService> services = Collections.emptyList();
        int applicationIndex;
        PlatformType platform;
        Map<String, String> bindingErrors = new HashMap<>();
        Map<String, List<ServiceKey>> existingServiceKeys = new HashMap<>();
    }

    protected static class SimpleService {
        String name;
        boolean isOptional;

        CloudServiceExtended toCloudServiceExtended() {
            CloudServiceExtended service = new CloudServiceExtended();
            service.setName(name);
            service.setOptional(isOptional);
            return service;
        }
    }
}
