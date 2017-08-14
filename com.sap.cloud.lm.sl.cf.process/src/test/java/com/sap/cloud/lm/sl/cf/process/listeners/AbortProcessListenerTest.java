package com.sap.cloud.lm.sl.cf.process.listeners;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricVariableInstance;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
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

import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.helpers.BeanProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;

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
        private AbstractFileService fileService;

        @InjectMocks
        BeanProvider beanProvider = BeanProvider.getInstance();

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
                Mockito.verify(fileService).deleteFile(SPACE_ID, fileId);
            }
        }

        private class AbortProcessListenerMock extends AbortProcessListener {

            private static final long serialVersionUID = 1L;

            @Override
            protected HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
                String parameter) {
                switch (parameter) {
                    case com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID:
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
                Mockito.when(instanceMock.getValue()).thenReturn(parameterValue);
                return instanceMock;
            }

        }

    }

    public static class AbortProcessListenerPortCleanupTest {

        private static final String SPACE = "initial";
        private static final String ORG = "initial";
        private static final String DEFAULT_DOMAIN = "localhost";
        private static final String USER = "XSMASTER";

        private Set<Integer> allocatedPorts;

        @Mock
        private CloudFoundryClientProvider clientProvider;
        @Mock
        private CloudFoundryOperations client;

        @InjectMocks
        BeanProvider beanProvider = BeanProvider.getInstance();

        @InjectMocks
        AbortProcessListener abortListener = new AbortProcessListenerMock();

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testDeleteAllocatedRoutes1() throws Exception {
            Mockito.when(clientProvider.getCloudFoundryClient(USER, ORG, SPACE, null)).thenReturn(client);
            Mockito.when(client.getDefaultDomain()).thenReturn(new CloudDomain(null, DEFAULT_DOMAIN, null));
            Mockito.doThrow(CloudFoundryException.class).when(client).deleteRoute(Integer.toString(1), DEFAULT_DOMAIN);
            Mockito.doThrow(CloudFoundryException.class).when(client).deleteRoute(Integer.toString(3), DEFAULT_DOMAIN);

            allocatedPorts = new TreeSet<>(Arrays.asList(1, 2, 3, 4));

            abortListener.deleteAllocatedRoutes(mock(HistoryService.class), PROCESS_INSTANCE_ID);

            Mockito.verify(client).deleteRoute(Integer.toString(2), DEFAULT_DOMAIN);
            Mockito.verify(client).deleteRoute(Integer.toString(4), DEFAULT_DOMAIN);
        }

        @Test
        public void testDeleteAllocatedRoutes2() throws Exception {
            Mockito.when(clientProvider.getCloudFoundryClient(USER, ORG, SPACE, null)).thenReturn(client);
            abortListener.deleteAllocatedRoutes(mock(HistoryService.class), PROCESS_INSTANCE_ID);
            Mockito.verify(client, Mockito.never()).deleteRoute(Mockito.anyString(), Mockito.any());
        }

        private class AbortProcessListenerMock extends AbortProcessListener {

            private static final long serialVersionUID = 1L;

            @Override
            protected HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
                String parameter) {
                switch (parameter) {
                    case Constants.VAR_ALLOCATED_PORTS:
                        return createInstanceMock(GsonHelper.getAsBinaryJson(allocatedPorts));
                    case Constants.VAR_USER:
                        return createInstanceMock(USER);
                    case Constants.VAR_SPACE:
                        return createInstanceMock(SPACE);
                    case Constants.VAR_ORG:
                        return createInstanceMock(ORG);
                    default:
                        return null;
                }
            }

            private HistoricVariableInstance createInstanceMock(Object parameterValue) {
                HistoricVariableInstance instanceMock = Mockito.mock(HistoricVariableInstance.class);
                Mockito.when(instanceMock.getValue()).thenReturn(parameterValue);
                return instanceMock;
            }

        }

    }

}
