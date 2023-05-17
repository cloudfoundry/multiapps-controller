package org.cloudfoundry.multiapps.controller.process.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;

class ManageAppServiceBindingEndListenerTest {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";

    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private DelegateExecution execution;
    @Mock
    private ProcessTypeParser processTypeParser;
    @InjectMocks
    private ManageAppServiceBindingEndListener manageAppServiceBindingEndListener;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    // @formatter:off
    static Stream<Arguments> testBindUnbindServiceEndListener() {
        return Stream.of(Arguments.of(false, false, false),
                         Arguments.of(false, true, true),
                         Arguments.of(true, false, true),
                         Arguments.of(true, true, true));
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    void testBindUnbindServiceEndListener(boolean shouldUnbind, boolean shouldBind, boolean expectedBooleanValue) {
        prepareExecution(shouldUnbind, shouldBind);

        manageAppServiceBindingEndListener.notifyInternal(execution);

        verify(flowableFacade).setVariableInParentProcess(execution,
                                                          ManageAppServiceBindingEndListener.buildExportedVariableName(APPLICATION_NAME,
                                                                                                                       SERVICE_NAME),
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
    }

    @Test
    void testBindUnbindServiceEndListenerWhenAppOrServiceIsNotSet() {
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.BLUE_GREEN_DEPLOY);

        manageAppServiceBindingEndListener.notifyInternal(execution);
        verify(flowableFacade, never()).setVariableInParentProcess(any(), anyString(), any());
    }
}
