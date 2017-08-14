package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.CtsProcessExtensionsSetter;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;

@RunWith(Parameterized.class)
public class SetCtsProcessExtensionsStepTest extends AbstractStepTest<SetCtsProcessExtensionsStep> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The return code is set to 0:
            {
                "set-cts-process-status-step-input-00.json", "R:cts-process-extensions-00.json",
            },
            // (1) The return code is set to 4:
            {
                "set-cts-process-status-step-input-01.json", "R:cts-process-extensions-01.json",
            },
            // (2) The return code is null:
            {
                "set-cts-process-status-step-input-02.json", "R:cts-process-extensions-00.json",
            },
// @formatter:on
        });
    }

    private StepInput stepInput;
    private String expected;

    public SetCtsProcessExtensionsStepTest(String stepInputSourceLocation, String expected) throws ParsingException, IOException {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputSourceLocation, getClass()), StepInput.class);
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        Map<String, Object> extensionElements = new HashMap<>();
        if (stepInput.getCtsReturnCode() != null) {
            extensionElements.put("ctsErrorCode", stepInput.ctsReturnCode.toString());
        } else {
            extensionElements.put("ctsErrorCode", CtsReturnCode.OK);
        }
        doReturn(Arrays.asList(new ProgressMessage(stepInput.getActProcessId(), stepInput.getActProcessId(), ProgressMessageType.EXT,
            JsonUtil.toJson(extensionElements), new Date()))).when(taskExtensionService).findByProcessId(stepInput.getActProcessId());

        step.extensionsSetterSupplier = () -> new CtsProcessExtensionsSetter(taskExtensionService, () -> null);

        doReturn(stepInput.getActProcessId()).when(context).getProcessInstanceId();
        doReturn(stepInput.getCtsProcessId()).when(context).getVariable(Constants.PARAM_CTS_PROCESS_ID);
    }

    @Test
    public void testExecute() throws Exception {
        ArgumentCaptor<ProgressMessage> progressMessageCaptor = ArgumentCaptor.forClass(ProgressMessage.class);
        TestUtil.test(() -> {
            step.execute(context);

            assertStepFinishedSuccessfully();

            verify(taskExtensionService).add(progressMessageCaptor.capture());
            return progressMessageCaptor.getValue();

        } , expected, getClass());
    }

    @Override
    protected SetCtsProcessExtensionsStep createStep() {
        return new SetCtsProcessExtensionsStep();
    }

    private static class StepInput {

        private String ctsProcessId;
        private String actProcessId;
        private CtsReturnCode ctsReturnCode;

        public String getCtsProcessId() {
            return ctsProcessId;
        }

        public String getActProcessId() {
            return actProcessId;
        }

        public CtsReturnCode getCtsReturnCode() {
            return ctsReturnCode;
        }

    }

    @Override
    protected String getCorrelationId() {
        return stepInput.actProcessId;
    }

}
