package org.cloudfoundry.multiapps.controller.process.util;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OperationInFinalStateHandlerTest {

    private static final String SPACE_ID = "space-id";

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;

    @InjectMocks
    private final OperationInFinalStateHandler eventHandler = new OperationInFinalStateHandler();

    public static Stream<Arguments> testDeleteDeploymentFiles() {
        return Stream.of(
//@formatter:off
          Arguments.of("10", "20", true, new String[] { }),
          Arguments.of("10", null, true, new String[] { }),
          Arguments.of(null, "20", true, new String[] { }),
          Arguments.of(null, null, true, new String[] { }),
          Arguments.of("10", "20", false, new String[] { "10", "20", }),
          Arguments.of(null, "20", false, new String[] { "20", }),
          Arguments.of("10", null, false, new String[] { "10", }),
          Arguments.of(null, null, false, new String[] { }),
          Arguments.of("10,20,30", null, false, new String[] { "10", "20", "30", }),
          Arguments.of(null, "10,20,30", false, new String[] { "10", "20", "30", })
//@formatter:on
        );
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(stepLogger);
    }

    private void prepareContext(String archiveIds, String extensionDescriptorIds, boolean keepFiles) {
        VariableHandling.set(execution, Variables.SPACE_GUID, SPACE_ID);
        VariableHandling.set(execution, Variables.APP_ARCHIVE_ID, archiveIds);
        VariableHandling.set(execution, Variables.EXT_DESCRIPTOR_FILE_ID, extensionDescriptorIds);
        VariableHandling.set(execution, Variables.KEEP_FILES, keepFiles);
    }

    @ParameterizedTest
    @MethodSource
    void testDeleteDeploymentFiles(String archiveIds, String extensionDescriptorIds, boolean keepFiles, String[] expectedFileIdsToSweep)
        throws Exception {
        prepareContext(archiveIds, extensionDescriptorIds, keepFiles);
        eventHandler.deleteDeploymentFiles(execution);
        for (String fileId : expectedFileIdsToSweep) {
            Mockito.verify(fileService)
                   .deleteFile(SPACE_ID, fileId);
        }
    }

}
