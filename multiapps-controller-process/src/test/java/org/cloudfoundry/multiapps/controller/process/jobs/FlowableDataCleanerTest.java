package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FlowableDataCleanerTest {

    @Mock
    private FlowableFacade flowableFacade;
    @InjectMocks
    private FlowableDataCleaner flowableDataCleaner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    static Stream<Arguments> testDeleteInvocation() {
        // @formatter:off
             return Stream.of(
                              Arguments.of(Arrays.asList("process-id-1")),
                              Arguments.of(Arrays.asList("process-id-1", "process-id-2")),
                              Arguments.of(Collections.emptyList())
             );
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    void testDeleteInvocation(List<String> processIds) {
        Date date = new Date();
        prepareFlowableFacade(processIds, date);

        flowableDataCleaner.execute(date);

        verifyDeletionOfProcessIds(processIds);
        verify(flowableFacade).findAllRunningProcessInstanceStartedBefore(date);
    }

    private void prepareFlowableFacade(List<String> processIds, Date date) {
        List<ProcessInstance> processInstances = processIds.stream()
                                                           .map(this::buildProcessInstance)
                                                           .collect(Collectors.toList());
        when(flowableFacade.findAllRunningProcessInstanceStartedBefore(date)).thenReturn(processInstances);
    }

    private ProcessInstance buildProcessInstance(String processId) {
        ExecutionEntityImpl executionEntity = new ExecutionEntityImpl();
        executionEntity.setProcessInstanceId(processId);
        return executionEntity;
    }

    private void verifyDeletionOfProcessIds(List<String> processIds) {
        for (String processId : processIds) {
            verify(flowableFacade).deleteProcessInstance(processId, Operation.State.ABORTED.name());
        }
    }
}
