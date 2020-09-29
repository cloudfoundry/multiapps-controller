package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Arrays;

import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class OperationInFinalStateHandlerTest {

    private static final String SPACE_ID = "space-id";

    private final String archiveIds;
    private final String extensionDescriptorIds;
    private final boolean keepFiles;
    private final String[] expectedFileIdsToSweep;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;

    @InjectMocks
    private OperationInFinalStateHandler eventHandler = new OperationInFinalStateHandler();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
//@formatter:off
          {
              "10", "20", true, new String[] { },
          },
          {
              "10", null, true, new String[] { },
          },
          {
              null, "20", true, new String[] { },
          },
          {
              null, null, true, new String[] { },
          },
          {
              "10", "20", false, new String[] { "10", "20", },
          },
          {
              null, "20", false, new String[] { "20", },
          },
          {
              "10", null, false, new String[] { "10", },
          },
          {
              null, null, false, new String[] { },
          },
          {
              "10,20,30", null, false, new String[] { "10", "20", "30", },
          },
          {
              null, "10,20,30", false, new String[] { "10", "20", "30", },
          },
//@formatter:on
        });
    }

    public OperationInFinalStateHandlerTest(String archiveIds, String extensionDescriptorIds, boolean keepFiles,
                                            String[] expectedFileIdsToSweep) {
        this.archiveIds = archiveIds;
        this.extensionDescriptorIds = extensionDescriptorIds;
        this.keepFiles = keepFiles;
        this.expectedFileIdsToSweep = expectedFileIdsToSweep;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareContext();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(stepLogger);
    }

    private void prepareContext() {
        VariableHandling.set(execution, Variables.SPACE_GUID, SPACE_ID);
        VariableHandling.set(execution, Variables.APP_ARCHIVE_ID, archiveIds);
        VariableHandling.set(execution, Variables.EXT_DESCRIPTOR_FILE_ID, extensionDescriptorIds);
        VariableHandling.set(execution, Variables.KEEP_FILES, keepFiles);
    }

    @Test
    public void testDeleteDeploymentFiles() throws Exception {
        eventHandler.deleteDeploymentFiles(execution);
        for (String fileId : expectedFileIdsToSweep) {
            Mockito.verify(fileService)
                   .deleteFile(SPACE_ID, fileId);
        }
    }

}
