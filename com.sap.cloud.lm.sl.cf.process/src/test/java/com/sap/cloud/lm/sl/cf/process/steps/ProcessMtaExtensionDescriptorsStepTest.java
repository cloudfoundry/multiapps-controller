package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.persistence.processors.FileDownloadProcessor;

@RunWith(Parameterized.class)
public class ProcessMtaExtensionDescriptorsStepTest extends AbstractStepTest<ProcessMtaExtensionDescriptorsStep> {

    private static final String SPACE_ID = "0";

    private final List<String> descriptorFileLocations;

    public ProcessMtaExtensionDescriptorsStepTest(List<String> descriptorFileLocations) {
        this.descriptorFileLocations = descriptorFileLocations;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Two extension descriptors to process:
            {
                Arrays.asList("config-01.mtaext", "config-02.mtaext"),
            },
            // (1) No  extension descriptors to process:
            {
                Collections.emptyList(),
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareFileService();
    }

    private void prepareContext() {
        List<String> fileIds = new ArrayList<>();
        for (int i = 0; i < descriptorFileLocations.size(); i++) {
            fileIds.add(Integer.toString(i));
        }
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_EXT_DESCRIPTOR_FILE_ID, String.join(",", fileIds));
        context.setVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
    }

    private void prepareFileService() throws Exception {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                FileDownloadProcessor contentProcessor = (FileDownloadProcessor) invocation.getArguments()[0];
                int fileId = Integer.parseInt(contentProcessor.getFileEntry().getId());

                contentProcessor.processContent(getClass().getResourceAsStream(descriptorFileLocations.get(fileId)));
                return null;
            }

        }).when(fileService).processFileContent(any());
    }

    private void validateOutput() throws Exception {
        List<String> expected = new ArrayList<>();
        for (String extensionDescriptorFileLocation : descriptorFileLocations) {
            expected.add(IOUtils.toString(getClass().getResourceAsStream(extensionDescriptorFileLocation)));
        }
        List<String> actual = ContextUtil.getArrayVariableAsList(context,
            com.sap.cloud.lm.sl.cf.process.Constants.VAR_MTA_EXTENSION_DESCRIPTOR_STRINGS);
        assertEquals(expected, actual);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        validateOutput();
    }

    @Override
    protected ProcessMtaExtensionDescriptorsStep createStep() {
        return new ProcessMtaExtensionDescriptorsStep();
    }

}
