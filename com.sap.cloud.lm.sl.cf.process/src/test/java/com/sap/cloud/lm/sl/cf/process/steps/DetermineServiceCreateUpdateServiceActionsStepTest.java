package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters(name = "{0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "determine-actions-create-or-update-services-step-input-1-create-key.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-2-no-action.json", null,
            },
            {
                "determine-actions-create-or-update-services-step-input-3-recreate-service.json", null,
            },
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
//          {
//          "determine-actions-create-or-update-services-step-input-8-recreate-service-failure.json", null,
//          },
            {
                "determine-actions-create-or-update-services-step-input-9-recreate-service-error.json", MessageFormat.format(Messages.ERROR_SERVICE_NEEDS_TO_BE_RECREATED_BUT_FLAG_NOT_SET, "service-1", "label-1/plan-3", "service-1", "label-1-old/plan-3"),
            },
            {
                "determine-actions-create-or-update-services-step-input-10-update-credentials.json", null
            },
                        {
                "determine-actions-create-or-update-services-step-input-11-no-update-credentials.json", null
            }
         // @formatter:on
        });
    }

    public DetermineServiceCreateUpdateServiceActionsStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput,
                                                                        DetermineServiceCreateUpdateServiceActionsStepTest.class),
                                           StepInput.class);
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    @Before
    public void setUp() {
        prepareContext();
        prepareClient();
        prepareServiceInstanceGetter();
    }

    private void prepareServiceInstanceGetter() {
        Mockito.reset(serviceInstanceGetter);
        Mockito.when(serviceInstanceGetter.getServiceInstanceEntity(any(), any(), any()))
               .thenReturn(stepInput.getExistingServiceInstanceEntity());
    }

    private void prepareContext() {
        StepsUtil.setServiceKeysToCreate(context, stepInput.getServiceKeysToCreate());
        context.setVariable(Constants.VAR_SERVICE_TO_PROCESS, JsonUtil.toJson(stepInput.service));
        context.setVariable(Constants.PARAM_DELETE_SERVICE_KEYS, true);
        context.setVariable(Constants.PARAM_DELETE_SERVICES, stepInput.shouldDeleteServices);
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
            CloudService existingService = stepInput.getCloudService(stepInput.existingService);
            CloudServiceInstance existingServiceInstance = stepInput.getCloudServiceInstance(stepInput.existingService);
            Mockito.when(client.getService(stepInput.existingService.getName(), false))
                   .thenReturn(existingService);
            Mockito.when(client.getServiceInstance(stepInput.existingService.getName(), false))
                   .thenReturn(existingServiceInstance);
            Mockito.when(client.getServiceParameters(UUID.fromString("beeb5e8d-4ab9-46ee-9205-455a278743f0")))
                   .thenThrow(new CloudOperationException(HttpStatus.BAD_REQUEST));
            Mockito.when(client.getServiceParameters(UUID.fromString("400bfc4d-5fce-4a41-bae7-765345e1ce27")))
                   .thenReturn(existingServiceInstance.getCredentials());
        }
    }

    private static class StepInput {

        // ServiceData - Input
        CloudServiceExtended service;
        CloudServiceExtended existingService;
        ServiceOperation lastOperationForExistingService;

        // ServiceData - Expectation
        boolean shouldCreateService;
        boolean shouldDeleteServices;
        boolean shouldRecreateService;
        boolean shouldUpdateServicePlan;
        boolean shouldUpdateServiceKeys;
        boolean shouldUpdateServiceTags;
        boolean shouldUpdateServiceCredentials;

        // ServiceKeys - Input
        List<CloudServiceKey> serviceKeysToCreate = Collections.emptyList();
        // ServiceKeys - Expectation

        public Map<String, List<CloudServiceKey>> getServiceKeysToCreate() {
            Map<String, List<CloudServiceKey>> result = new HashMap<>();
            result.put(service.getName(), serviceKeysToCreate);
            return result;
        }

        public CloudService getCloudService(CloudServiceExtended service) {
            return ImmutableCloudService.builder()
                                        .from(service)
                                        .build();
        }

        public CloudServiceInstance getCloudServiceInstance(CloudServiceExtended service) {
            return ImmutableCloudServiceInstance.builder()
                                                .metadata(service.getMetadata())
                                                .name(service.getName())
                                                .credentials(service.getCredentials())
                                                .build();
        }

        public Map<String, Object> getExistingServiceInstanceEntity() {
            if (existingService == null) {
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            if (lastOperationForExistingService != null) {
                Map<String, String> operation = new HashMap<>();
                operation.put("type", lastOperationForExistingService.getType()
                                                                     .toString());
                operation.put("state", lastOperationForExistingService.getState()
                                                                      .toString());
                result.put("last_operation", operation);
            }
            if (existingService.getTags() != null) {
                result.put("tags", existingService.getTags());
            }
            return result;
        }
    }

    @Override
    protected DetermineServiceCreateUpdateServiceActionsStep createStep() {
        return new DetermineServiceCreateUpdateServiceActionsStep();
    }

}
