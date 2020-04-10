package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ProcessMtaArchiveStepTest extends SyncFlowableStepTest<ProcessMtaArchiveStep> {

    private static final String SPACE_ID = "0";

    private static final String FILE_ID = "0";

    private final StepInput input;

    public ProcessMtaArchiveStepTest() throws ParsingException {
        String json = TestUtil.getResourceAsString("process-mta-archive-step-1.json", getClass());
        this.input = JsonUtil.fromJson(json, StepInput.class);
    }

    @BeforeEach
    public void setUp() throws Exception {
        prepareContext();
        prepareFileService();
        when(configuration.getMaxMtaDescriptorSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        when(configuration.getMaxManifestSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE);
    }

    private void prepareContext() {
        context.setVariable(Variables.APP_ARCHIVE_ID, FILE_ID);
        context.setVariable(Variables.SPACE_ID, SPACE_ID);
        step.conflictPreventerSupplier = service -> mock(ProcessConflictPreventer.class);
    }

    private void prepareFileService() throws Exception {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                String fileId = (String) invocation.getArguments()[1];
                FileContentProcessor fileContentProcessor = invocation.getArgument(2);

                fileContentProcessor.processFileContent(getClass().getResourceAsStream(input.archiveFileLocations.get(Integer.parseInt(fileId))));
                return null;
            }

        }).when(fileService)
          .processFileContent(any(), any(), any());
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        testModules();
        testResources();
        testDependencies();
    }

    private void testModules() {
        Set<String> actualModules = context.getVariable(Variables.MTA_ARCHIVE_MODULES);

        assertEquals(input.expectedModules, actualModules);
    }

    private void testResources() {
        for (String expectedResource : input.expectedResources) {
            assertNotNull(context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS)
                                 .getResourceFileName(expectedResource));
        }
    }

    private void testDependencies() {
        for (String expectedDependency : input.expectedRequiredDependencies) {
            assertNotNull(context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS)
                                 .getRequiredDependencyFileName(expectedDependency));
        }
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
                                                                                .collect(Collectors.toMap(m -> m, m -> m)));
            when(helper.getMtaArchiveResources()).thenReturn(input.expectedResources.stream()
                                                                                    .collect(Collectors.toMap(r -> r, r -> r)));
            when(helper.getMtaRequiresDependencies()).thenReturn(input.expectedRequiredDependencies.stream()
                                                                                                   .collect(Collectors.toMap(d -> d,
                                                                                                                             d -> d)));
            return helper;
        }

    }

    @Override
    protected ProcessMtaArchiveStep createStep() {
        return new ProcessMtaArchiveStepMock();
    }

}
