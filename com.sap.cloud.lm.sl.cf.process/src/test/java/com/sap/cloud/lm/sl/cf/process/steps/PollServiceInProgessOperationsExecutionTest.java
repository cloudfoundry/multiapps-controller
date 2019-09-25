package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.persistence.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;

@RunWith(Parameterized.class)
public class PollServiceInProgessOperationsExecutionTest extends AsyncStepOperationTest<CheckForOperationsInProgressStep> {

    private static final String TEST_SPACE_ID = "test";
    private static final String TEST_PROVIDER = "testProvider";
    private static final String TEST_PLAN = "testPlan";
    private static final String TEST_VERSION = "0.0.1-beta";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) With 2 services in progress:
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(ServiceOperationType.DELETE, ServiceOperationType.CREATE, ServiceOperationType.DELETE),
                Arrays.asList(ServiceOperationState.IN_PROGRESS, ServiceOperationState.SUCCEEDED, ServiceOperationState.IN_PROGRESS), 
                false, AsyncExecutionState.RUNNING, null
            },
            // (1) With 1 service in progress state and 1 successfully deleted
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(ServiceOperationType.DELETE, ServiceOperationType.CREATE, ServiceOperationType.DELETE),
                Arrays.asList(null, ServiceOperationState.SUCCEEDED, ServiceOperationState.IN_PROGRESS), 
                true, AsyncExecutionState.RUNNING, null
            },
            // (2) With 3 services finished operations successfully 
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(ServiceOperationType.UPDATE, ServiceOperationType.CREATE, ServiceOperationType.DELETE),
                Arrays.asList(ServiceOperationState.SUCCEEDED, ServiceOperationState.SUCCEEDED, ServiceOperationState.SUCCEEDED), 
                false, AsyncExecutionState.FINISHED, null
            },
            // (3) Handle missing response for last service operation
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(null, ServiceOperationType.CREATE, null),
                Arrays.asList(null, null, null), 
                false, AsyncExecutionState.FINISHED, null
            },
            // (4) Throw exception on create failed service state
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(null, ServiceOperationType.CREATE, null),
                Arrays.asList(null, ServiceOperationState.FAILED, null), 
                true, null, "Error creating service \"service2\" from offering \"null\" and plan \"testPlan\""
            },
            // (5) Throw exception on update failed service state
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(null, ServiceOperationType.UPDATE, null),
                Arrays.asList(null, ServiceOperationState.FAILED, null), 
                true, null, "Error updating service \"service2\" from offering \"null\" and plan \"testPlan\""
            },
            // (5) Throw exception on delete failed service state
            {
                Arrays.asList("service1","service2", "service3"), 
                Arrays.asList(null, ServiceOperationType.DELETE, null),
                Arrays.asList(null, ServiceOperationState.FAILED, null), 
                true, null, "Error deleting service \"service2\" from offering \"null\" and plan \"testPlan\""
            },            
// @formatter:on
        });
    }

    public PollServiceInProgessOperationsExecutionTest(List<String> serviceNames, List<ServiceOperationType> servicesOperationTypes,
                                                       List<ServiceOperationState> servicesOperationStates, boolean shouldVerifyStepLogger,
                                                       AsyncExecutionState expectedExecutionState, String expectedExceptionMessage) {
        this.serviceNames = serviceNames;
        this.servicesOperationTypes = servicesOperationTypes;
        this.servicesOperationStates = servicesOperationStates;
        this.shouldVerifyStepLogger = shouldVerifyStepLogger;
        this.expectedExecutionState = expectedExecutionState;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Mock
    private ServiceOperationGetter serviceOperationGetter;
    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    private EventsGetter eventsGetter;
    @Mock
    private CloudControllerClient client;
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private List<String> serviceNames;
    private List<ServiceOperationType> servicesOperationTypes;
    private List<ServiceOperationState> servicesOperationStates;
    private boolean shouldVerifyStepLogger;
    private AsyncExecutionState expectedExecutionState;
    private String expectedExceptionMessage;

    @Before
    public void setUp() {
        context.setVariable(Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        List<CloudServiceExtended> services = generateCloudServicesExtended();
        prepareServiceOperationGetter(services);
        prepareServicesData(services);
        prepareTriggeredServiceOperations();
        when(clientProvider.getControllerClient(anyString(), anyString())).thenReturn(client);
        if (expectedExceptionMessage != null) {
            exception.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareServiceOperationGetter(List<CloudServiceExtended> services) {
        for (int i = 0; i < services.size(); i++) {
            CloudServiceExtended service = services.get(i);
            ServiceOperationType serviceOperationType = servicesOperationTypes.get(i);
            ServiceOperationState serviceOperationState = servicesOperationStates.get(i);
            if (serviceOperationType != null && serviceOperationState != null) {
                when(serviceOperationGetter.getLastServiceOperation(any(),
                                                                    eq(service))).thenReturn(new ServiceOperation(serviceOperationType,
                                                                                                                  "",
                                                                                                                  serviceOperationState));
            }
        }
    }

    private void prepareTriggeredServiceOperations() {
        Map<String, ServiceOperationType> trigerredServiceOperations = new HashMap<>();
        for (int index = 0; index < serviceNames.size(); index++) {
            String serviceName = serviceNames.get(index);
            ServiceOperationType serviceOperationType = servicesOperationTypes.get(index);
            if (serviceOperationType != null) {
                trigerredServiceOperations.put(serviceName, serviceOperationType);
            }
        }
        StepsUtil.setTriggeredServiceOperations(context, trigerredServiceOperations);
    }

    private List<CloudServiceExtended> generateCloudServicesExtended() {
        return serviceNames.stream()
                           .map(this::buildCloudServiceExtended)
                           .collect(Collectors.toList());
    }

    private ImmutableCloudServiceExtended buildCloudServiceExtended(String serviceName) {
        return ImmutableCloudServiceExtended.builder()
                                            .name(serviceName)
                                            .provider(TEST_PROVIDER)
                                            .plan(TEST_PLAN)
                                            .version(TEST_VERSION)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .build();
    }

    private void prepareServicesData(List<CloudServiceExtended> services) {
        StepsUtil.setServicesData(context, services);
    }

    @Override
    protected CheckForOperationsInProgressStep createStep() {
        return new CheckForOperationsInProgressStep();
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        if (shouldVerifyStepLogger) {
            verify(stepLogger).warnWithoutProgressMessage(anyString(), any());
        }
        assertEquals(expectedExecutionState, result);
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }
}
