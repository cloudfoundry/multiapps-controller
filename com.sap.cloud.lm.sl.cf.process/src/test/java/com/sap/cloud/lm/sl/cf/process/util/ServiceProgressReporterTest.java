package com.sap.cloud.lm.sl.cf.process.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;

public class ServiceProgressReporterTest {

    @Mock
    private ExecutionWrapper execution;
    @Mock
    private StepLogger stepLogger;

    private ServiceProgressReporter serviceProgressReporter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        serviceProgressReporter = new ServiceProgressReporter();
    }

    public static Stream<Arguments> testServiceProgressReporter() {
        return Stream.of(
        // @formatter:off
                      Arguments.of(Arrays.asList(new ServiceOperation(ServiceOperationType.CREATE, "", ServiceOperationState.IN_PROGRESS), 
                                                 new ServiceOperation(ServiceOperationType.UPDATE, "", ServiceOperationState.IN_PROGRESS)), 2),
                      Arguments.of(Arrays.asList(new ServiceOperation(ServiceOperationType.UPDATE, "", ServiceOperationState.SUCCEEDED)), 0));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testServiceProgressReporter(List<ServiceOperation> servicesOperations, int countTriggeredServiceOperations) {
        prepareExecution();
        Map<String, ServiceOperationType> triggeredServiceOperations = getServicesOperationsInProgress(servicesOperations);

        serviceProgressReporter.reportOverallProgress(execution, servicesOperations, triggeredServiceOperations);

        if (countTriggeredServiceOperations > 0) {
            verify(stepLogger).info(anyString(), any(), eq(countTriggeredServiceOperations), any());
        } else {
            verify(stepLogger).info(anyString(), eq(countTriggeredServiceOperations));
        }
    }

    private void prepareExecution() {
        when(execution.getStepLogger()).thenReturn(stepLogger);
    }

    private Map<String, ServiceOperationType> getServicesOperationsInProgress(List<ServiceOperation> servicesOperations) {
        Map<String, ServiceOperationType> servicesOperationsInProgress = new HashMap<>();
        for (int index = 0; index < servicesOperations.size(); index++) {
            ServiceOperation serviceOperation = servicesOperations.get(index);
            if (serviceOperation.getState() == ServiceOperationState.IN_PROGRESS) {
                servicesOperationsInProgress.put("service" + index, serviceOperation.getType());
            }
        }
        return servicesOperationsInProgress;
    }
}
