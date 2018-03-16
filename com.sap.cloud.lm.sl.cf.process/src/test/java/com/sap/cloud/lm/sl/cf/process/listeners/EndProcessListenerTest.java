package com.sap.cloud.lm.sl.cf.process.listeners;

import java.util.Arrays;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;

@RunWith(Parameterized.class)
public class EndProcessListenerTest {

    private static final String SPACE_ID = "space-id";

    private final String archiveIds;
    private final String extensionDescriptorIds;
    private final boolean keepFiles;
    private final String[] expectedFileIdsToSweep;

    private DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Mock
    private AbstractFileService fileService;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;

    @InjectMocks
    private EndProcessListener listener = new EndProcessListener();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
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
// @formatter:on
        });
    }

    public EndProcessListenerTest(String archiveIds, String extensionDescriptorIds, boolean keepFiles, String[] expectedFileIdsToSweep) {
        this.archiveIds = archiveIds;
        this.extensionDescriptorIds = extensionDescriptorIds;
        this.keepFiles = keepFiles;
        this.expectedFileIdsToSweep = expectedFileIdsToSweep;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        prepareContext();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(stepLogger);
    }

    private void prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, archiveIds);
        context.setVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID, extensionDescriptorIds);
        context.setVariable(Constants.PARAM_KEEP_FILES, keepFiles);
    }

    @Test
    public void testDeleteDeploymentFiles() throws Exception {
        listener.deleteDeploymentFiles(context);
        for (String fileId : expectedFileIdsToSweep) {
            Mockito.verify(fileService)
                .deleteFile(SPACE_ID, fileId);
        }
    }

}
