package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;

public abstract class CreateOrUpdateAppStepBaseTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    protected StepInput stepInput;
    protected CloudApplicationExtended application;

    protected static class StepInput {
        List<CloudApplicationExtended> applications = Collections.emptyList();
        List<SimpleService> services = Collections.emptyList();
        int applicationIndex;
        Map<String, String> bindingErrors = new HashMap<>();
        Map<String, List<CloudServiceKey>> existingServiceKeys = new HashMap<>();
    }

    protected static class SimpleService {
        String name;
        boolean isOptional;

        CloudServiceExtended toCloudServiceExtended() {
            return ImmutableCloudServiceExtended.builder()
                .name(name)
                .isOptional(isOptional)
                .build();
        }
    }
}
