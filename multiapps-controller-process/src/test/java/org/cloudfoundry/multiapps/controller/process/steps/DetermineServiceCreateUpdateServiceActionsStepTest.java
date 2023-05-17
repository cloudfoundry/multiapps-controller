package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
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
            Arguments.of("determine-actions-create-or-update-services-step-input-13-last-operation-failed-allow-deletion-of-services.json", null),
            Arguments.of("determine-actions-create-or-update-services-step-input-14-update-syslog-url.json", null)
         // @formatter:on
        );
    }

    private static final String SERVICE_NAME = "service";

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputFilename, String expectedExceptionMessage) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputFilename,
                                                                         DetermineServiceCreateUpdateServiceActionsStepTest.class),
                                            StepInput.class);
        initializeParameters(input, null);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }

        step.execute(execution);

        assertStepFinishedSuccessfully();

        validateActions(input);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testServiceParametersFetchingErrorHandling(boolean isOptionalService) {
        var service = createMockServiceInstance(isOptionalService);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of());

        Mockito.when(client.getServiceInstanceParameters(Mockito.any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
        Mockito.when(client.getServiceInstance(SERVICE_NAME, false))
               .thenReturn(service);

        if (isOptionalService) {
            step.execute(execution);
            assertStepFinishedSuccessfully();
            List<ServiceAction> serviceActionsToExecute = context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE);
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_CREDENTIALS),
                       "Actions should contain " + ServiceAction.UPDATE_CREDENTIALS);
            return;
        }
        SLException exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertTrue(exception.getCause() instanceof CloudOperationException);
        assertSame(((CloudOperationException) exception.getCause()).getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testUpdateServiceKeysWhenDeleteServiceKeysIsSet(boolean serviceExists) {
        var service = createMockServiceInstance(false);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of());
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        if (serviceExists) {
            Mockito.when(client.getServiceInstance(SERVICE_NAME, false))
                   .thenReturn(service);
        }

        step.execute(execution);

        assertStepFinishedSuccessfully();
        List<ServiceAction> serviceActionsToExecute = context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE);
        if (serviceExists) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_KEYS), "Actions should contain " + ServiceAction.UPDATE_KEYS);
            return;
        }
        assertFalse(serviceActionsToExecute.contains(ServiceAction.UPDATE_KEYS), "Actions should not contain " + ServiceAction.UPDATE_KEYS);
    }

    @Test
    void testUpdateServiceTagsForUserProvidedService() {
        CloudMetadata serviceMetadata = ImmutableCloudMetadata.of(UUID.randomUUID());
        var existingService = createMockUserProvidedServiceInstance(serviceMetadata, List.of("custom-tag-A", "custom-tag-B"));
        var serviceWithUpdatedTags = createMockUserProvidedServiceInstance(serviceMetadata,
                                                                           List.of("updated-custom-tag-A", "updated-custom-tag-B"));

        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceWithUpdatedTags);
        Mockito.when(client.getServiceInstance(SERVICE_NAME, false))
               .thenReturn(existingService);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        List<ServiceAction> serviceActionsToExecute = context.getVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE);
        assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_TAGS), "Actions should contain " + ServiceAction.UPDATE_TAGS);
    }

    static Stream<Arguments> testSetServiceGuidIfPresent() {
        return Stream.of(Arguments.of(
                                      // (1) Test resolve service guid
                                      "determine-actions-create-or-update-services-step-input-15-dynamic-parameter-relationship-match.json",
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-1")
                                                                                .build(),
                                             ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-2")
                                                                                .build()),
                                      ImmutableDynamicResolvableParameter.builder()
                                                                         .parameterName("service-guid")
                                                                         .relationshipEntityName("service-1")
                                                                         .value("beeb5e8d-4ab9-46ee-9205-455a278743f0")
                                                                         .build()),
                         // (2) Test skip resolve of unrelated parameter
                         Arguments.of("determine-actions-create-or-update-services-step-input-15-dynamic-parameter-relationship-match.json",
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-2")
                                                                                .build()),
                                      null),
                         // (3) Test skip resolve of service marked for recreation
                         Arguments.of("determine-actions-create-or-update-services-step-input-3-recreate-service.json",
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-1")
                                                                                .build()),
                                      null),
                         // (4) Test skip resolve of unrelated parameter due to different parameter type
                         Arguments.of("determine-actions-create-or-update-services-step-input-15-dynamic-parameter-relationship-match.json",
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("metadata-key")
                                                                                .relationshipEntityName("service-1")
                                                                                .build()),
                                      null));
    }

    @ParameterizedTest
    @MethodSource
    void testSetServiceGuidIfPresent(String inputFilename, Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                     DynamicResolvableParameter expectedResolvedParameter) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputFilename,
                                                                         DetermineServiceCreateUpdateServiceActionsStepTest.class),
                                            StepInput.class);
        initializeParameters(input, dynamicResolvableParameters);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedResolvedParameter, context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETER));
    }

    private void initializeParameters(StepInput input, Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        prepareContext(input, dynamicResolvableParameters);
        prepareClient(input);
    }

    private void prepareContext(StepInput input, Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, input.getServiceKeysToCreate());
        context.setVariable(Variables.SERVICE_TO_PROCESS, input.service);
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        context.setVariable(Variables.DELETE_SERVICES, input.shouldDeleteServices);
        context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicResolvableParameters);
    }

    private void prepareClient(StepInput input) {
        if (input.existingService != null) {
            Mockito.when(client.getServiceInstanceParameters(UUID.fromString("beeb5e8d-4ab9-46ee-9205-455a278743f0")))
                   .thenThrow(new CloudOperationException(HttpStatus.BAD_REQUEST));
            Mockito.when(client.getServiceInstanceParameters(UUID.fromString("400bfc4d-5fce-4a41-bae7-765345e1ce27")))
                   .thenReturn(input.existingService.getCredentials());

            if (input.lastOperationForExistingService != null) {
                ServiceOperation lastOp = new ServiceOperation(input.lastOperationForExistingService.getType(),
                                                               null,
                                                               input.lastOperationForExistingService.getState());
                input.existingService = ImmutableCloudServiceInstanceExtended.copyOf(input.existingService)
                                                                             .withLastOperation(lastOp);
            }
            Mockito.when(client.getServiceInstance(input.existingService.getName(), false))
                   .thenReturn(input.existingService);
        }
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
        if (input.shouldUpdateSyslogDrainUrl) {
            assertTrue(serviceActionsToExecute.contains(ServiceAction.UPDATE_SYSLOG_URL),
                       "Actions should contain " + ServiceAction.UPDATE_SYSLOG_URL);
        }
    }

    private CloudServiceInstanceExtended createMockServiceInstance(boolean optional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .resourceName(SERVICE_NAME)
                                                    .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                    .isOptional(optional)
                                                    .tags(List.of())
                                                    .putAllCredentials(Map.of("key", "val"))
                                                    .build();
    }

    private CloudServiceInstanceExtended createMockUserProvidedServiceInstance(CloudMetadata metadata, List<String> tags) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .resourceName(SERVICE_NAME)
                                                    .type(ServiceInstanceType.USER_PROVIDED)
                                                    .metadata(metadata)
                                                    .tags(tags)
                                                    .build();
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
        boolean shouldUpdateSyslogDrainUrl;

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
