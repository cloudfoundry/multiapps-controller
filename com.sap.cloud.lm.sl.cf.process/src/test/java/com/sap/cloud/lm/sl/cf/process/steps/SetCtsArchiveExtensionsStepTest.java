package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

import com.sap.cloud.lm.sl.cf.process.util.CtsArchiveExtensionsSetter;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.cts.FileInfo.FileInfoBuilder;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;

@RunWith(Parameterized.class)
public class SetCtsArchiveExtensionsStepTest extends AbstractStepTest<SetCtsArchiveExtensionsStep> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The return code is set to 0:
            {
                "set-cts-file-status-step-input-00.json", "R:cts-extensions-00.json",
            },
            // (1) The return code is set to 4:
            {
                "set-cts-file-status-step-input-01.json", "R:cts-extensions-01.json",
            },
            // (2) The return code is null:
            {
                "set-cts-file-status-step-input-02.json", "R:cts-extensions-00.json",
            },
// @formatter:on
        });
    }

    private StepInput stepInput;
    private String expected;

    public SetCtsArchiveExtensionsStepTest(String stepInputSourceLocation, String expected) throws ParsingException, IOException {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputSourceLocation, getClass()), StepInput.class);
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        if (stepInput.ctsReturnCode != null) {
            StepsUtil.setCtsReturnCode(context, stepInput.getCtsReturnCode());
        }

        step.extensionsSetterSupplier = () -> new CtsArchiveExtensionsSetter(taskExtensionService, () -> null);

        FileInfoBuilder fileInfoBuilder = new FileInfoBuilder();
        fileInfoBuilder.fileId(stepInput.getMtadId());
        fileInfoBuilder.fileName(stepInput.getMtarName());
        fileInfoBuilder.filePath(stepInput.getMtarPath());
        StepsUtil.setCtsCurrentFileInfo(context, fileInfoBuilder.build());

        doReturn(stepInput.getProcessId()).when(context).getProcessInstanceId();
        doReturn("test-subprocess-id").when(context).getVariable("subProcessId");
        doReturn("prepareDeployParametersTask").when(context).getVariable(com.sap.cloud.lm.sl.slp.Constants.INDEXED_STEP_NAME);
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
    protected SetCtsArchiveExtensionsStep createStep() {
        return new SetCtsArchiveExtensionsMockStep();
    }

    private static class StepInput {

        private String processId;
        private CtsReturnCode ctsReturnCode;
        private String mtadId;
        private String mtarName;
        private String mtarPath;

        public String getProcessId() {
            return processId;
        }

        public CtsReturnCode getCtsReturnCode() {
            return ctsReturnCode;
        }

        public String getMtadId() {
            return mtadId;
        }

        public String getMtarName() {
            return mtarName;
        }

        public String getMtarPath() {
            return mtarPath;
        }
    }

    private class SetCtsArchiveExtensionsMockStep extends SetCtsArchiveExtensionsStep {
        @Override
        protected CtsReturnCode getCtsReturnCodeFromSubProcess(DelegateExecution context, String subProcessId) {
            if (stepInput.ctsReturnCode == null) {
                return CtsReturnCode.OK;
            }
            return stepInput.ctsReturnCode;
        }
    }

    @Override
    protected String getCorrelationId() {
        return stepInput.processId;
    }

}
