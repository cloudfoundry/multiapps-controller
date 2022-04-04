package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ProcessMtaArchiveStepTest extends SyncFlowableStepTest<ProcessMtaArchiveStep> {

    private static final String SPACE_ID = "0";

    private static final String FILE_ID = "0";

    private final StepInput input;

    public ProcessMtaArchiveStepTest(String input) throws ParsingException, IOException {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, ProcessMtaArchiveStepTest.class), StepInput.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // (0)
            { "process-mta-archive-step-1.json" } });
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareFileService();
        when(configuration.getMaxMtaDescriptorSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        when(configuration.getMaxManifestSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE);
    }

    private void prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_APP_ARCHIVE_ID, FILE_ID);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        step.conflictPreventerSupplier = (dao) -> mock(ProcessConflictPreventer.class);
    }

    private void prepareFileService() throws Exception {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                FileDownloadProcessor contentProcessor = (FileDownloadProcessor) invocation.getArguments()[0];
                int fileId = Integer.parseInt(contentProcessor.getFileEntry()
                                                              .getId());

                contentProcessor.processContent(getClass().getResourceAsStream(input.archiveFileLocations.get(fileId)));
                return null;
            }

        }).when(fileService)
          .processFileContent(any());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        testModules();
        testResources();
        testDependencies();
    }

    private void testModules() throws Exception {
        List<String> actualModules = StepsUtil.getArrayVariableAsList(context,
                                                                      com.sap.cloud.lm.sl.cf.process.Constants.VAR_MTA_ARCHIVE_MODULES);

        assertEquals(input.expectedModules.size(), actualModules.size());

        for (String actualModuleName : actualModules) {
            assertTrue(input.expectedModules.contains(actualModuleName));
        }
    }

    private void testResources() throws Exception {
        for (String expecedResource : input.expectedResources) {
            assertTrue(StepsUtil.getResourceFileName(context, expecedResource) != null);
        }
    }

    private void testDependencies() throws Exception {
        for (String expecedDependecy : input.expectedRequiredDependencies) {
            assertTrue(StepsUtil.getRequiresFileName(context, expecedDependecy) != null);
        }
    }

    @Override
    protected ProcessMtaArchiveStep createStep() {
        return new ProcessMtaArchiveStepMock();
    }

    private static class StepInput {

        List<String> archiveFileLocations;
        Set<String> expectedModules;
        Set<String> expectedResources;
        Set<String> expectedRequiredDependencies;

    }

    private class ProcessMtaArchiveStepMock extends ProcessMtaArchiveStep {

        @Override
        protected MtaArchiveHelper getHelper(Manifest manifest) {
            MtaArchiveHelper helper = Mockito.mock(MtaArchiveHelper.class);
            when(helper.getMtaArchiveModules()).thenReturn(input.expectedModules.stream()
                                                                                .collect(Collectors.toMap(m -> m, Function.identity())));
            when(helper.getMtaArchiveResources()).thenReturn(input.expectedResources.stream()
                                                                                    .collect(Collectors.toMap(r -> r,
                                                                                                              Function.identity())));
            when(helper.getMtaRequiresDependencies()).thenReturn(input.expectedRequiredDependencies.stream()
                                                                                                   .collect(Collectors.toMap(d -> d,
                                                                                                                             Function.identity())));

            try {
                doAnswer(a -> null).when(helper)
                                   .init();
            } catch (SLException e) {
                // Ignore...
            }

            return helper;
        }

    }

}
