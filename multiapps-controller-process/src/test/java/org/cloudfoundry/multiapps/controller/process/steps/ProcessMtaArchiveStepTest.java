package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryExtractor;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryStreamWithStreamPositionsDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class ProcessMtaArchiveStepTest extends SyncFlowableStepTest<ProcessMtaArchiveStep> {

    private static final String SPACE_ID = "0";

    private static final String FILE_ID = "0";

    private final StepInput input;

    private final ArchiveEntryExtractor archiveEntryExtractor = Mockito.mock(ArchiveEntryExtractor.class);

    ProcessMtaArchiveStepTest() throws ParsingException {
        String json = TestUtil.getResourceAsString("process-mta-archive-step-1.json", getClass());
        this.input = JsonUtil.fromJson(json, StepInput.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        prepareContext();
        prepareFileService();
        when(configuration.getMaxMtaDescriptorSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        when(configuration.getMaxManifestSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE);
    }

    private void prepareContext() {
        context.setVariable(Variables.APP_ARCHIVE_ID, FILE_ID);
        context.setVariable(Variables.SPACE_GUID, SPACE_ID);
        step.conflictPreventerSupplier = service -> mock(ProcessConflictPreventer.class);
    }

    private <T> void prepareFileService() throws Exception {
        doAnswer(new Answer<T>() {

            @Override
            public T answer(InvocationOnMock invocation) throws Exception {
                String fileId = invocation.getArgument(1);
                int fileIndex = Integer.parseInt(fileId);
                FileContentProcessor<T> fileContentProcessor = invocation.getArgument(2);

                return fileContentProcessor.process(getClass().getResourceAsStream(input.archiveFileLocations.get(fileIndex)));
            }

        }).when(fileService)
          .processFileContent(any(), any(), any());
        step.archiveEntryStreamWithStreamPositionsDeterminer = spy(new ArchiveEntryStreamWithStreamPositionsDeterminer(fileService));
    }

    @Test
    void testExecute() {
        DescriptorParserFacadeFactory descriptorParserFactory = Mockito.mock(DescriptorParserFacadeFactory.class);
        Mockito.when(descriptorParserFactory.getInstance())
               .thenReturn(new DescriptorParserFacade());
        Mockito.when(archiveEntryExtractor.readFullEntry(any(), any()))
               .thenReturn(loadFile("test-manifest-1.MF"))
               .thenReturn(loadFile("test-mtad-1.yaml"));
        step.descriptorParserFactory = descriptorParserFactory;
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

    private byte[] loadFile(String fileName) {
        try (InputStream stream = getClass().getResourceAsStream(fileName)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
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
