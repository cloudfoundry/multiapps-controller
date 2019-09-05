package com.sap.cloud.lm.sl.cf.process.util;

import java.sql.Date;
import java.time.LocalDate;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;

public class HookProcessGetterTest {

    @Mock
    private ProgressMessageService progressMessageService;

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private DelegateExecution context;

    private Date now = Date.valueOf(LocalDate.now());

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWithHookWhichIsOfTypeTask() {
        String result = getProcessDefinitionForHookWithType("task");
        Assertions.assertEquals("executeHookTasksSubProcess", result);

        Mockito.verifyZeroInteractions(progressMessageService, flowableFacade, context);
    }

    @Test
    public void testWithHookWhichIsOfUnsupportedType() {
        Mockito.when(flowableFacade.getProcessInstanceId(Mockito.any()))
               .thenReturn("foo-process-id");
        Mockito.when(context.getCurrentActivityId())
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

        return hookProcessGetter.get(JsonUtil.toJson(testHook), context);
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
