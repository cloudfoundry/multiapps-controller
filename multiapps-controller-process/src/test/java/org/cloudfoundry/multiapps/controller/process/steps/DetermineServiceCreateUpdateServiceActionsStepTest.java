package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class DetermineServiceCreateUpdateServiceActionsStepTest extends SyncFlowableStepTest<DetermineServiceCreateUpdateServiceActionsStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            Arguments.of("determine-actions-create-or-update-services-step-input-1-create-key.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-2-no-action.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-3-recreate-service.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-4-update-plan.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-5-update-key.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-6-update-tags.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-7-update-credentials.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-9-recreate-service-error.json", MessageFormat.format(Messages.ERROR_SERVICE_NEEDS_TO_BE_RECREATED_BUT_FLAG_NOT_SET, "service-1", "label-1/plan-3", "service-1", "label-1-old/plan-3")),
            Arguments.of("determine-actions-create-or-update-services-step-input-10-update-credentials.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-11-no-update-credentials.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-12-last-operation-failed.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-13-last-operation-failed-allow-deletion-of-services.json", null)
         // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputFilename, String expectedExceptionMessage) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputFilename,
                                                                         DetermineServiceCreateUpdateServiceActionsStepTest.class),
                                            StepInput.class);
        initializeParameters(input);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }

        step.execute(execution);

        assertStepIsRunning();

        validateActions(input);
    }

    private void initializeParameters(StepInput input) {
        prepareContext(input);
        prepareClient(input);
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, input.getServiceKeysToCreate());
        context.setVariable(Variables.SERVICE_TO_PROCESS, input.service);
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        context.setVariable(Variables.DELETE_SERVICES, input.shouldDeleteServices);
    }

    private void validateActions(StepInput input) {
        List<ServiceAction> serviceActionsToExecute = context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE);
        if (input.shouldCreateService) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.CREATE), "Actions should contain " + ServiceAction.CREATE);
        }
        if (input.shouldRecreateService) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.RECREATE), "Actions should contain " + ServiceAction.RECREATE);
        }
        if (input.shouldUpdateServicePlan) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_PLAN), "Actions should contain " + ServiceAction.UPDATE_PLAN);
        }
        if (input.shouldUpdateServiceTags) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_TAGS), "Actions should contain " + ServiceAction.UPDATE_TAGS);
        }
        if (input.shouldUpdateServiceParameters) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_CREDENTIALS),
                       "Actions should contain " + ServiceAction.UPDATE_CREDENTIALS);
        }
        if (input.shouldUpdateServiceKeys) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_KEYS), "Actions should contain " + ServiceAction.UPDATE_KEYS);
        }
    }

    private void assertStepIsRunning() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    private void prepareClient(StepInput input) {
        if (input.existingService != null) {
            Mockito.when(client.getServiceInstanceParameters(UUID.fromString("beeb5e8d-4ab9-46ee-9205-455a278743f0")))
                   .thenThrow(new CloudOperationException(HttpStatus.BAD_REQUEST));
            Mockito.when(client.getServiceInstanceParameters(UUID.fromString("400bfc4d-5fce-4a41-bae7-765345e1ce27")))
                   .thenReturn(input.existingService.getCredentials());

            if (input.lastOperationForExistingService != null) {
                ServiceOperation lastOp = new ServiceOperation(input.lastOperationForExistingService.getType(), null,
                                                               input.lastOperationForExistingService.getState());
                input.existingService = ImmutableCloudServiceInstanceExtended.copyOf(input.existingService)
                                                                             .withLastOperation(lastOp);
            }
            Mockito.when(client.getServiceInstance(input.existingService.getName(), false))
                   .thenReturn(input.existingService);
        }
    }

    private static class StepInput {

        // ServiceData - Input
        CloudServiceInstanceExtended service;
        CloudServiceInstanceExtended existingService;
        ServiceOperation lastOperationForExistingService;

        // ServiceData - Expectation
        boolean shouldCreateService;
        boolean shouldDeleteServices;
        boolean shouldRecreateService;
        boolean shouldUpdateServicePlan;
        boolean shouldUpdateServiceKeys;
        boolean shouldUpdateServiceTags;
        boolean shouldUpdateServiceParameters;

        // ServiceKeys - Input
        final List<CloudServiceKey> serviceKeysToCreate = Collections.emptyList();
        
        // ServiceKeys - Expectation
        public Map<String, List<CloudServiceKey>> getServiceKeysToCreate() {
            return Map.of(service.getName(), serviceKeysToCreate);
        }
    }

    @Override
    protected DetermineServiceCreateUpdateServiceActionsStep createStep() {
        return new DetermineServiceCreateUpdateServiceActionsStep();
    }

}
