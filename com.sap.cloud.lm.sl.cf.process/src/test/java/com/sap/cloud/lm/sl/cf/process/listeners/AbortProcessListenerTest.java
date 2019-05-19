package com.sap.cloud.lm.sl.cf.process.listeners;

import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.flowable.engine.HistoryService;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Enclosed.class)
public class AbortProcessListenerTest {

    private static final String PROCESS_INSTANCE_ID = "process-instance-id";

    @RunWith(Parameterized.class)
    public static class AbortProcessListenerFileCleanupTest {

        private static final String SPACE_ID = "space-id";

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0) Assert that the archive and extension descriptors will not be deleted when using --keep-files:
                {
                    "10", "20", true, new String[] { },
                },
                // (1) Assert that the archive and extension descriptors will be deleted when not using --keep-files:
                {
                    "10", "20", false, new String[] { "10", "20", },
                },
                // (2) Assert that the archive will not be deleted when using --keep-files:
                {
                    "10", null, true, new String[] { },
                },
                // (3) Assert that the archive will be deleted when not using --keep-files:
                {
                    "10", null, false, new String[] { "10", },
                },
                // (4) Assert that nothing will be deleted when using --keep-files and no files are present:
                {
                    null, null, true, new String[] { },
                },
                // (5) Assert that nothing will be deleted when not using --keep-files and no files are present:
                {
                    null, null, false, new String[] { },
                },
                // (6) Assert that the extension descriptors will not be deleted when using --keep-files:
                {
                    null, "20", true, new String[] { },
                },
                // (7) Assert that the extension descriptors will be deleted when not using --keep-files:
                {
                    null, "20", false, new String[] { "20", },
                },
                // (8) Assert that ALL extension descriptors will be deleted when not using --keep-files:
                {
                    null, "10,20,30", false, new String[] { "10", "20", "30", },
                },
                // (8) Assert that ALL MTAR chunks will be deleted when not using --keep-files:
                {
                    "10,20,30", null, false, new String[] { "10", "20", "30", },
                },
// @formatter:on
            });
        }

        private final String archiveIds;
        private final String extensionDescriptorIds;
        private final boolean shouldKeepFiles;
        private final String[] expectedFileIdsToSweep;

        @Mock
        private FileService fileService;

        @InjectMocks
        AbortProcessListener abortListener = new AbortProcessListenerMock();

        public AbortProcessListenerFileCleanupTest(String archiveIds, String extensionDescriptorIds, boolean shouldKeepFiles,
            String[] expectedFileIdsToSweep) {
            this.archiveIds = archiveIds;
            this.extensionDescriptorIds = extensionDescriptorIds;
            this.shouldKeepFiles = shouldKeepFiles;
            this.expectedFileIdsToSweep = expectedFileIdsToSweep;
        }

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testDeleteDeploymentFiles() throws Exception {
            abortListener.deleteDeploymentFiles(mock(HistoryService.class), PROCESS_INSTANCE_ID);
            for (String fileId : expectedFileIdsToSweep) {
                Mockito.verify(fileService)
                    .deleteFile(SPACE_ID, fileId);
            }
        }

        private class AbortProcessListenerMock extends AbortProcessListener {

            private static final long serialVersionUID = 1L;

            @Override
            protected HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
                String parameter) {
                switch (parameter) {
                    case com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID:
                        return createInstanceMock(SPACE_ID);
                    case Constants.PARAM_EXT_DESCRIPTOR_FILE_ID:
                        return createInstanceMock(extensionDescriptorIds);
                    case Constants.PARAM_APP_ARCHIVE_ID:
                        return createInstanceMock(archiveIds);
                    case Constants.PARAM_KEEP_FILES:
                        return createInstanceMock(shouldKeepFiles);
                    default:
                        return null;
                }
            }

            private HistoricVariableInstance createInstanceMock(Object parameterValue) {
                HistoricVariableInstance instanceMock = Mockito.mock(HistoricVariableInstance.class);
                Mockito.when(instanceMock.getValue())
                    .thenReturn(parameterValue);
                return instanceMock;
            }

        }

    }

}
