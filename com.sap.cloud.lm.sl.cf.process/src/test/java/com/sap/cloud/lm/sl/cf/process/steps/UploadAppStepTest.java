package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.util.InputStreamProducer;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.processors.FileDownloadProcessor;

@RunWith(Enclosed.class)
public class UploadAppStepTest {

    private static final IOException IO_EXCEPTION = new IOException();
    private static final CloudFoundryException CF_EXCEPTION = new CloudFoundryException(HttpStatus.BAD_REQUEST);

    private static final String APP_NAME = "sample-app-backend";
    private static final String APP_FILE = "web.zip";
    private static final String TOKEN = "token";
    private static final String SPACE = "space";
    private static final String APP_ARCHIVE = "sample-app.mtar";

    public static class ProcessStepTest extends AbstractStepTest<UploadAppStep> {

        @Mock
        protected ScheduledExecutorService asyncTaskExecutor;

        @Before
        public void setUp() throws Exception {
            StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(new SimpleApplication(APP_NAME, 2).toCloudApplication()), context);
            context.setVariable(Constants.VAR_APPS_INDEX, 0);
        }

        @Test
        public void testPollStatus1() throws Exception {
            step.createStepLogger(context);
            ExecutionStatus status = step.executeStepInternal(context);

            assertEquals(ExecutionStatus.RUNNING.toString(), status.toString());
        }

        @Test(expected = SLException.class)
        public void testPollStatus2() throws Exception {
            when(clientProvider.getCloudFoundryClient(eq(USER_NAME), eq(ORG_NAME), eq(SPACE_NAME), anyString())).thenThrow(
                new SLException(new CloudFoundryException(HttpStatus.BAD_REQUEST)));
            step.execute(context);
        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStep();
        }

    }

    @RunWith(Parameterized.class)
    public static class AppUploaderTest extends AbstractStepTest<UploadAppStep> {

        @Rule
        public TemporaryFolder tempDir = new TemporaryFolder();
        @Rule
        public ExpectedException expectedException = ExpectedException.none();
        @Mock
        private ClientExtensions cfExtensions;
        @Mock
        private CloudFoundryOperations client;

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00)
                {
                    Boolean.TRUE , null, null,
                },
                // (01)
                {
                    Boolean.FALSE, null, null,
                },
                // (02)
                {
                    Boolean.TRUE , format(Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, APP_FILE), null,
                },
                // (03)
                {
                    Boolean.FALSE, format(Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, APP_FILE), null,
                },
                // (04)
                {
                    Boolean.TRUE , null, StepsUtil.createException(CF_EXCEPTION).getMessage(),
                },
                // (05)
                {
                    Boolean.FALSE, null, StepsUtil.createException(CF_EXCEPTION).getMessage(),
                },
// @formatter:on
            });
        }

        private boolean clientSupportsExtensions;
        private String expectedIOExceptionMessage;
        private String expectedCFExceptionMessage;

        private File appFile;

        public AppUploaderTest(boolean clientSupportsExtensions, String expectedIOExceptionMessage, String expectedCFExceptionMessage) {
            this.clientSupportsExtensions = clientSupportsExtensions;
            this.expectedIOExceptionMessage = expectedIOExceptionMessage;
            this.expectedCFExceptionMessage = expectedCFExceptionMessage;
        }

        @Before
        public void setUp() throws Exception {
            MockitoAnnotations.initMocks(this);
            loadParameters();
            prepareFileService();
            prepareContext();
            prepareClients();
        }

        @Test
        public void test() {
            if (!clientSupportsExtensions) {
                cfExtensions = null;
            }
            Runnable uploader = step.getUploadAppStepRunnable(context, new SimpleApplication(APP_NAME, 2).toCloudApplication(), client,
                cfExtensions);
            try {
                uploader.run();
            } catch (Throwable e) {
                assertFalse(appFile.exists());
                throw e;
            }

            Map<String, Object> outputVariables = ((UploadAppStepMock) step).getOutputVariables();
            assertNotNull(outputVariables);
            if (clientSupportsExtensions) {
                assertEquals(TOKEN, outputVariables.get(Constants.VAR_UPLOAD_TOKEN));
                assertEquals(ExecutionStatus.RUNNING.toString(),
                    outputVariables.get(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));
            } else {
                assertEquals(ExecutionStatus.SUCCESS.toString(),
                    outputVariables.get(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));
            }
        }

        public void loadParameters() throws Exception {
            if (expectedIOExceptionMessage != null) {
                expectedException.expectMessage(expectedIOExceptionMessage);
                expectedException.expect(SLException.class);
            }
            if (expectedCFExceptionMessage != null) {
                expectedException.expectMessage(expectedCFExceptionMessage);
                expectedException.expect(SLException.class);
            }
        }

        public void prepareContext() throws Exception {
            context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.VARIABLE_NAME_SPACE_ID, SPACE);
            StepsUtil.setModuleFileName(context, APP_NAME, APP_FILE);
            StepsUtil.setAppPropertiesChanged(context, false);
        }

        public void prepareClients() throws Exception {
            if (clientSupportsExtensions) {
                if (expectedIOExceptionMessage == null && expectedCFExceptionMessage == null) {
                    when(cfExtensions.asynchUploadApplication(eq(APP_NAME), eq(appFile), any())).thenReturn(TOKEN);
                } else if (expectedIOExceptionMessage != null) {
                    when(cfExtensions.asynchUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(IO_EXCEPTION);
                } else if (expectedCFExceptionMessage != null) {
                    when(cfExtensions.asynchUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(CF_EXCEPTION);
                }
            } else {
                if (expectedIOExceptionMessage != null) {
                    doThrow(IO_EXCEPTION).when(client).uploadApplication(eq(APP_NAME), eq(appFile), any());
                }
                if (expectedCFExceptionMessage != null) {
                    doThrow(CF_EXCEPTION).when(client).uploadApplication(eq(APP_NAME), eq(appFile), any());
                }
            }
            when(client.getApplication(APP_NAME)).thenReturn(new SimpleApplication(APP_NAME, 2).toCloudApplication());
        }

        public void prepareFileService() throws Exception {
            tempDir.create();
            appFile = tempDir.newFile(APP_FILE);
            doAnswer(new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) throws Exception {
                    ((FileDownloadProcessor) invocation.getArguments()[0]).processContent(null);
                    return null;
                }
            }).when(fileService).processFileContent(any());
        }

        private class UploadAppStepMock extends UploadAppStep {

            private Map<String, Object> outputVariables;

            @Override
            public File saveToFile(String fileName, InputStreamProducer streamProducer) throws IOException {
                InputStream stream = streamProducer.getNextInputStream();
                assertNotNull(stream);
                Files.copy(stream, appFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return appFile;
            }

            @Override
            InputStreamProducer getInputStreamProducer(InputStream appArchiveStream, String fileName) throws SLException {
                if (!fileName.equals(APP_FILE)) {
                    return super.getInputStreamProducer(appArchiveStream, fileName);
                }
                return new InputStreamProducer(getClass().getResourceAsStream(APP_FILE), fileName) {
                    @Override
                    public InputStream getNextInputStream() {
                        return getClass().getResourceAsStream(APP_FILE);
                    }
                };
            }

            @Override
            protected void signalWaitTask(String processId, Map<String, Object> outputVariables, int timeout) {
                this.outputVariables = outputVariables;
            }

            public Map<String, Object> getOutputVariables() {
                return outputVariables;
            }

        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStepMock();
        }

    }

}
