package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.ServiceOperation;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ServiceProgressReporterTest {

    @Mock
    private ProcessContext context;
    @Mock
    private StepLogger stepLogger;

    private ServiceProgressReporter serviceProgressReporter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        serviceProgressReporter = new ServiceProgressReporter();
    }

    static Stream<Arguments> testServiceProgressReporter() {
        return Stream.of(Arguments.of(Arrays.asList(new ServiceOperation(ServiceOperation.Type.CREATE,
                                                                         "",
                                                                         ServiceOperation.State.IN_PROGRESS),
                                                    new ServiceOperation(ServiceOperation.Type.UPDATE,
                                                                         "",
                                                                         ServiceOperation.State.IN_PROGRESS)),
                                      2),
                         Arguments.of(Collections.singletonList(new ServiceOperation(ServiceOperation.Type.UPDATE,
                                                                                     "",
                                                                                     ServiceOperation.State.SUCCEEDED)),
                                      0));
    }

    @ParameterizedTest
    @MethodSource
    void testServiceProgressReporter(List<ServiceOperation> servicesOperations, int countTriggeredServiceOperations) {
        prepareExecution();
        Map<String, ServiceOperation.Type> triggeredServiceOperations = getServicesOperationsInProgress(servicesOperations);

        serviceProgressReporter.reportOverallProgress(context, servicesOperations, triggeredServiceOperations);

        if (countTriggeredServiceOperations > 0) {
            verify(stepLogger).info(anyString(), any(), eq(countTriggeredServiceOperations), any());
        } else {
            verify(stepLogger).info(anyString(), eq(countTriggeredServiceOperations));
        }
    }

    private void prepareExecution() {
        when(context.getStepLogger()).thenReturn(stepLogger);
    }

    private Map<String, ServiceOperation.Type> getServicesOperationsInProgress(List<ServiceOperation> servicesOperations) {
        Map<String, ServiceOperation.Type> servicesOperationsInProgress = new HashMap<>();
        for (int index = 0; index < servicesOperations.size(); index++) {
            ServiceOperation serviceOperation = servicesOperations.get(index);
            if (serviceOperation.getState() == ServiceOperation.State.IN_PROGRESS) {
                servicesOperationsInProgress.put("service" + index, serviceOperation.getType());
            }
        }
        return servicesOperationsInProgress;
    }

}
