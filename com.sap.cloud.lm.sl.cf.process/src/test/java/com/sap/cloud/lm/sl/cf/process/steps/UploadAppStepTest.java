package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveContext;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveReader;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationZipBuilder;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

@RunWith(Enclosed.class)
public class UploadAppStepTest {

    @RunWith(Parameterized.class)
    public static class UploadAppStepParameterizedTest extends SyncFlowableStepTest<UploadAppStep> {

        private static final IOException IO_EXCEPTION = new IOException();
        private static final CloudOperationException CO_EXCEPTION = new CloudOperationException(HttpStatus.BAD_REQUEST);

        private static final String APP_NAME = "sample-app-backend";
        private static final String APP_FILE = "web.zip";
        private static final String TOKEN = "token";
        private static final String SPACE = "space";
        private static final String APP_ARCHIVE = "sample-app.mtar";

        public TemporaryFolder tempDir = new TemporaryFolder();
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00)
                {
                    null, null,
                },
                // (01)
                {
                    null, null,
                },
                // (02)
                {
                    format(Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, APP_FILE), null,
                },
                // (03)
                {
                    format(Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, APP_FILE), null,
                },
                // (04)
                {
                    null, createException(CO_EXCEPTION).getMessage(),
                },
                // (05)
                {
                    null, createException(CO_EXCEPTION).getMessage(),
                },
// @formatter:on
            });
        }

        private String expectedIOExceptionMessage;
        private String expectedCFExceptionMessage;
        private MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();

        private File appFile;

        public UploadAppStepParameterizedTest(String expectedIOExceptionMessage, String expectedCFExceptionMessage) {
            this.expectedIOExceptionMessage = expectedIOExceptionMessage;
            this.expectedCFExceptionMessage = expectedCFExceptionMessage;
        }

        @Before
        public void setUp() throws Exception {
            loadParameters();
            prepareFileService();
            prepareContext();
            prepareClients();
        }

        @After
        public void tearDown() throws IOException {
            FileUtils.deleteQuietly(appFile.getParentFile());
        }

        @Test
        public void test() throws Throwable {
            try {
                step.execute(context);
            } catch (Throwable e) {
                e.printStackTrace();
                assertFalse(appFile.exists());
                throw e;
            }

            String uploadTokenJson = JsonUtil.toJson(new UploadToken(TOKEN, null));
            assertCall(Constants.VAR_UPLOAD_TOKEN, uploadTokenJson);
        }

        private void assertCall(String variableName, String variableValue) {
            Mockito.verify(context)
                   .setVariable(variableName, variableValue);
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
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .moduleName(APP_NAME)
                                                                            .build();
            StepsUtil.setApp(context, app);
            context.setVariable(Constants.VAR_MODULES_INDEX, 0);
            context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE);
            mtaArchiveElements.addModuleFileName(APP_NAME, APP_FILE);
            StepsUtil.setMtaArchiveElements(context, mtaArchiveElements);
            StepsUtil.setVcapAppPropertiesChanged(context, false);
            when(configuration.getMaxResourceFileSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE);
        }

        public void prepareClients() throws Exception {
            if (expectedIOExceptionMessage == null && expectedCFExceptionMessage == null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenReturn(new UploadToken(TOKEN, null));
            } else if (expectedIOExceptionMessage != null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(IO_EXCEPTION);
            } else if (expectedCFExceptionMessage != null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(CO_EXCEPTION);
            }
            when(client.getApplication(APP_NAME)).thenReturn(new SimpleApplication(APP_NAME, 2).toCloudApplication());
        }

        public void prepareFileService() throws Exception {
            tempDir.create();
            appFile = tempDir.newFile(APP_FILE);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Exception {
                    ((FileDownloadProcessor) invocation.getArguments()[0]).processContent(null);
                    return null;
                }
            }).when(fileService)
              .processFileContent(any());
        }

        private class UploadAppStepMock extends UploadAppStep {

            public UploadAppStepMock() {
                applicationArchiveReader = getApplicationArchiveReader();
                applicationZipBuilder = getApplicationZipBuilder(applicationArchiveReader);
            }

            @Override
            protected ApplicationArchiveContext createApplicationArchiveContext(InputStream appArchiveStream, String fileName,
                                                                                long maxSize) {
                return super.createApplicationArchiveContext(getClass().getResourceAsStream(APP_ARCHIVE), fileName, maxSize);
            }

            private ApplicationArchiveReader getApplicationArchiveReader() {
                return new ApplicationArchiveReader();
            }

            private ApplicationZipBuilder getApplicationZipBuilder(ApplicationArchiveReader applicationArchiveReader) {
                return new ApplicationZipBuilder(applicationArchiveReader) {
                    @Override
                    protected Path createTempFile() throws IOException {
                        return appFile.toPath();
                    }
                };
            }

        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStepMock();
        }

        private static SLException createException(CloudOperationException e) {
            return new SLException(e, Messages.CF_ERROR, e.getMessage());
        }

    }

    public static class UploadAppStepTimeoutTest extends SyncFlowableStepTest<UploadAppStep> {

        private static final String APP_NAME = "sample-app-backend";

        @Before
        public void prepareContext() {
            context.setVariable(Constants.VAR_MODULES_INDEX, 0);
            step.initializeStepLogger(context);
        }

        @Test
        public void testGetTimeoutWithoutAppParameter() {
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .build();

            testGetTimeout(app, UploadAppStep.DEFAULT_APP_UPLOAD_TIMEOUT);
        }

        @Test
        public void testGetTimeoutWithAppParameter() {
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .env(MapUtil.asMap(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                                               "{\"upload-timeout\":1800}"))
                                                                            .build();

            testGetTimeout(app, 1800);
        }

        private void testGetTimeout(CloudApplicationExtended app, int expectedUploadTimeout) {
            StepsUtil.setApp(context, app);

            int uploadTimeout = step.getTimeout(context);
            assertEquals(expectedUploadTimeout, uploadTimeout);
        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStep();
        }

    }

    public static class UploadAppStepWithoutFileNameTest extends SyncFlowableStepTest<UploadAppStep> {
        private static final String SPACE = "space";
        private static final String APP_NAME = "simple-app";
        private static final String APP_ARCHIVE = "sample-app.mtar";

        @Before
        public void setUp() {
            prepareContext();
        }

        private void prepareContext() {
            // module name must be null
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .build();
            StepsUtil.setApp(context, app);
            context.setVariable(Constants.VAR_MODULES_INDEX, 0);
            context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE);
            MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
            mtaArchiveElements.addModuleFileName(APP_NAME, APP_NAME);
            StepsUtil.setMtaArchiveElements(context, mtaArchiveElements);
        }

        @Test
        public void testWithMissingFileNameMustReturnDone() {
            step.execute(context);
            assertStepFinishedSuccessfully();
        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStep();
        }

    }

}
