package org.cloudfoundry.multiapps.controller.process.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ManageAppServiceBindingEndListenerTest {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";
    private static final String PARENT_EXECUTION_ID = "1234";

    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private DelegateExecution execution;
    @Mock
    private ProcessEngine processEngine;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private Execution parentExecution;

    @InjectMocks
    private ManageAppServiceBindingEndListener manageAppServiceBindingEndListener;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    static Stream<Arguments> testBindUnbindServiceEndListener() {
        return Stream.of(Arguments.of(false, false, false), Arguments.of(false, true, true), Arguments.of(true, false, true),
                         Arguments.of(true, true, true));
    }

    @ParameterizedTest
    @MethodSource
    void testBindUnbindServiceEndListener(boolean shouldUnbind, boolean shouldBind, boolean expectedBooleanValue) {
        prepareExecution(shouldUnbind, shouldBind);

        manageAppServiceBindingEndListener.notifyInternal(execution);

        verify(runtimeService).setVariable(PARENT_EXECUTION_ID,
                                           ManageAppServiceBindingEndListener.buildExportedVariableName(APPLICATION_NAME, SERVICE_NAME),
                                           expectedBooleanValue);
    }

    private void prepareExecution(boolean shouldUnbind, boolean shouldBind) {
        CloudApplication application = ImmutableCloudApplication.builder()
                                                                .name(APPLICATION_NAME)
                                                                .build();
        when(execution.getVariable(Variables.APP_TO_PROCESS.getName())).thenReturn(JsonUtil.toJson(application));
        when(execution.getVariable(Variables.SERVICE_TO_UNBIND_BIND.getName())).thenReturn(SERVICE_NAME);
        when(execution.getVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP.getName())).thenReturn(shouldUnbind);
        when(execution.getVariable(Variables.SHOULD_BIND_SERVICE_TO_APP.getName())).thenReturn(shouldBind);
        when(execution.getParentId()).thenReturn(PARENT_EXECUTION_ID);
        when(parentExecution.getSuperExecutionId()).thenReturn(PARENT_EXECUTION_ID);
        when(flowableFacade.getParentExecution(PARENT_EXECUTION_ID)).thenReturn(parentExecution);
        when(flowableFacade.getProcessEngine()).thenReturn(processEngine);
        when(processEngine.getRuntimeService()).thenReturn(runtimeService);
    }
}
