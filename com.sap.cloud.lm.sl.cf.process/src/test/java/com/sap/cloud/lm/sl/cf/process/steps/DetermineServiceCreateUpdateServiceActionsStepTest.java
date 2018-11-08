package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DetermineServiceCreateUpdateServiceActionsStepTest
    extends SyncFlowableStepTest<DetermineServiceCreateUpdateServiceActionsStep> {

    @Mock
    private ServiceGetter serviceInstanceGetter;

    private StepInput stepInput;

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Parameters(name="{0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "determine-actions-create-or-update-services-step-input-1-create-key.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-2-no-action.json", null,
            },
//            {
//                "determine-actions-create-or-update-services-step-input-3-recreate-service.json", null,
//            },
            {
                "determine-actions-create-or-update-services-step-input-4-update-plan.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-5-update-key.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-6-update-tags.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-7-update-credentials.json", null,
            },
//            {
//                "determine-actions-create-or-update-services-step-input-8-recreate-service-failure.json", null,
//            },
         // @formatter:on
        });
    }

    public DetermineServiceCreateUpdateServiceActionsStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil
            .fromJson(TestUtil.getResourceAsString(stepInput, DetermineServiceCreateUpdateServiceActionsStepTest.class), StepInput.class);
    }

    @Before
    public void setUp() {
        prepareContext();
        prepareClient();
        prepareServiceInstanceGetter();
    }

    private void prepareServiceInstanceGetter() {
        Mockito.reset(serviceInstanceGetter);
        Mockito.when(serviceInstanceGetter.getServiceInstanceEntity(Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn(stepInput.getServiceInstanceEntity(stepInput.existingService));
    }

    private void prepareContext() {
        StepsUtil.setServiceKeysToCreate(context, stepInput.getServiceKeysToCreate());
        context.setVariable(Constants.VAR_SERVICE_TO_PROCESS, JsonUtil.toJson(stepInput.service));
        context.setVariable(Constants.PARAM_DELETE_SERVICE_KEYS, true);
        context.setVariable(Constants.PARAM_DELETE_SERVICES, true);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepIsRunning();

        validateActions();
    }

    private void validateActions() {
        List<ServiceAction> serviceActionsToExecute = StepsUtil.getServiceActionsToExecute(context);
        if (stepInput.shouldCreateService) {
            collector.checkThat("Actions should contain " + ServiceAction.CREATE, serviceActionsToExecute.contains(ServiceAction.CREATE),
                Is.is(true));
        }
        if (stepInput.shouldRecreateService) {
            collector.checkThat("Actions should contain " + ServiceAction.RECREATE,
                serviceActionsToExecute.contains(ServiceAction.RECREATE), Is.is(true));
        }
        if (stepInput.shouldUpdateServicePlan) {
            collector.checkThat("Actions should contain " + ServiceAction.UPDATE_PLAN,
                serviceActionsToExecute.contains(ServiceAction.UPDATE_PLAN), Is.is(true));
        }
        if (stepInput.shouldUpdateServiceTags) {
            collector.checkThat("Actions should contain " + ServiceAction.UPDATE_TAGS,
                serviceActionsToExecute.contains(ServiceAction.UPDATE_TAGS), Is.is(true));
        }
        if (stepInput.shouldUpdateServiceCredentials) {
            collector.checkThat("Actions should contain " + ServiceAction.UPDATE_CREDENTIALS,
                serviceActionsToExecute.contains(ServiceAction.UPDATE_CREDENTIALS), Is.is(true));
        }
        if (stepInput.shouldUpdateServiceKeys) {
            collector.checkThat("Actions should contain " + ServiceAction.UPDATE_KEYS,
                serviceActionsToExecute.contains(ServiceAction.UPDATE_KEYS), Is.is(true));
        }
    }

    private void assertStepIsRunning() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    private void prepareClient() {
        if (stepInput.existingService != null) {
            Mockito.when(client.getService(stepInput.existingService.getName(), false))
                .thenReturn(stepInput.getCloudService(stepInput.existingService));
            Mockito.when(client.getServiceInstance(stepInput.existingService.getName(), false))
                .thenReturn(stepInput.getCloudServiceInsntance(stepInput.existingService));
        }
    }

    private static class StepInput {

        // ServiceData - Input
        CloudServiceExtended service;
        CloudServiceExtendedForTest existingService;

        // ServiceData - Expectation
        boolean shouldCreateService;
        boolean shouldRecreateService;
        boolean shouldUpdateServicePlan;
        boolean shouldUpdateServiceKeys;
        boolean shouldUpdateServiceTags;
        boolean shouldUpdateServiceCredentials;

        // ServiceKeys - Input
        List<ServiceKey> serviceKeysToCreate = Collections.emptyList();
        // ServiceKeys - Expectation

        public Map<String, List<ServiceKey>> getServiceKeysToCreate() {
            Map<String, List<ServiceKey>> result = new HashMap<>();
            result.put(service.getName(), serviceKeysToCreate);
            return result;
        }

        public CloudService getCloudService(CloudServiceExtended service) {
            CloudService cloudService = new CloudService();
            cloudService.setMeta(service.getMeta());
            cloudService.setLabel(service.getLabel());
            cloudService.setName(service.getName());
            cloudService.setProvider(service.getProvider());
            cloudService.setVersion(service.getVersion());
            cloudService.setPlan(service.getPlan());
            return cloudService;
        }

        public CloudServiceInstance getCloudServiceInsntance(CloudServiceExtended service) {
            CloudServiceInstance cloudServiceInstance = new CloudServiceInstance();
            cloudServiceInstance.setMeta(service.getMeta());
            cloudServiceInstance.setName(service.getName());
            cloudServiceInstance.setCredentials(service.getCredentials());
            return cloudServiceInstance;
        }

        public Map<String, Object> getServiceInstanceEntity(CloudServiceExtendedForTest service) {
            if(service == null) {
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            if (service.lastOperation != null) {
                Map<String, String> operation = new HashMap<>();
                operation.put("type", service.lastOperation.getType()
                    .toString());
                operation.put("state", service.lastOperation.getState()
                    .toString());
                result.put("last_operation", operation);
            }
            if(service.getTags() != null) {
                result.put("tags", service.getTags());
            }
            return result;
        }
    }

    private static class CloudServiceExtendedForTest extends CloudServiceExtended {
        ServiceOperation lastOperation;
    }

    @Override
    protected DetermineServiceCreateUpdateServiceActionsStep createStep() {
        return new DetermineServiceCreateUpdateServiceActionsStep();
    }

}
