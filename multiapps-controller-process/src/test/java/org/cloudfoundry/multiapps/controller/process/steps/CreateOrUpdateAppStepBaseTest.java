package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;

public abstract class CreateOrUpdateAppStepBaseTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    protected StepInput stepInput;
    protected CloudApplicationExtended application;

    protected static class StepInput {
        List<CloudApplicationExtended> applications = Collections.emptyList();
        final List<SimpleService> services = Collections.emptyList();
        int applicationIndex;
        final Map<String, String> bindingErrors = new HashMap<>();
        final Map<String, List<CloudServiceKey>> existingServiceKeys = new HashMap<>();
    }

    protected static class SimpleService {
        String name;
        boolean isOptional;

        CloudServiceInstanceExtended toCloudServiceExtended() {
            return ImmutableCloudServiceInstanceExtended.builder()
                                                        .name(name)
                                                        .isOptional(isOptional)
                                                        .build();
        }
    }
}
