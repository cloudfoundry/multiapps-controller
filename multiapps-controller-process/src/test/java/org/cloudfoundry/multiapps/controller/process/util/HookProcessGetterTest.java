package org.cloudfoundry.multiapps.controller.process.util;

import java.sql.Date;
import java.time.LocalDate;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class HookProcessGetterTest {

    @Mock
    private ProgressMessageService progressMessageService;

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private DelegateExecution execution;

    private final Date now = Date.valueOf(LocalDate.now());

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWithHookWhichIsOfTypeTask() {
        String result = getProcessDefinitionForHookWithType("task");
        Assertions.assertEquals("executeHookTasksSubProcess", result);

        Mockito.verifyNoInteractions(progressMessageService, flowableFacade, execution);
    }

    @Test
    public void testWithHookWhichIsOfUnsupportedType() {
        Mockito.when(flowableFacade.getProcessInstanceId(Mockito.any()))
               .thenReturn("foo-process-id");
        Mockito.when(execution.getCurrentActivityId())
               .thenReturn("foo-current-activity-id");

        Throwable thrownException = Assertions.assertThrows(IllegalStateException.class,
                                                            () -> getProcessDefinitionForHookWithType("unsupported-hook-type"));

        Assertions.assertEquals("Unsupported hook type \"unsupported-hook-type\"", thrownException.getMessage());

        Mockito.verify(progressMessageService)
               .add(ImmutableProgressMessage.builder()
                                            .processId("foo-process-id")
                                            .taskId("foo-current-activity-id")
                                            .type(ProgressMessageType.ERROR)
                                            .text(thrownException.getMessage())
                                            .timestamp(now)
                                            .build());
    }

    private String getProcessDefinitionForHookWithType(String hookType) {
        HookProcessGetter hookProcessGetter = new HookProcessGetterMock(progressMessageService, flowableFacade);
        Hook testHook = Hook.createV3()
                            .setName("foo")
                            .setType(hookType);

        return hookProcessGetter.get(JsonUtil.toJson(testHook), execution);
    }

    private class HookProcessGetterMock extends HookProcessGetter {

        public HookProcessGetterMock(ProgressMessageService progressMessageService, FlowableFacade flowableFacade) {
            super(progressMessageService, flowableFacade);
        }

        @Override
        protected java.util.Date getCurrentTime() {
            return now;
        }
    }
}
