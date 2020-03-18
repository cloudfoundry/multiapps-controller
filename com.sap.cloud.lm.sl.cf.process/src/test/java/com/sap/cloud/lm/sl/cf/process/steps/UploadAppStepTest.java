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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudBuild.State;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild.ImmutableDropletInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
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
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveContext;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveReader;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationZipBuilder;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
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
        private static final String SPACE = "space";
        private static final String APP_ARCHIVE = "sample-app.mtar";
        private static final UploadToken UPLOAD_TOKEN = ImmutableUploadToken.builder()
                                                                            .packageGuid(UUID.randomUUID())
                                                                            .build();
        private static final String MODULE_DIGEST = "439B99DFFD0583200D5D21F4CD1BF035";
        private static final String DATE_PATTERN = "dd-MM-yyyy";

        public final TemporaryFolder tempDir = new TemporaryFolder();
        @Rule
        public final ExpectedException expectedException = ExpectedException.none();

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00)
                {
                    null, null, true, null
                },
                // (01)
                {
                    format(Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, APP_FILE), null, true, null
                },
                // (02)
                {
                    null, createException(CO_EXCEPTION).getMessage(), true, null
                },
                // (03)
                {
                    null, null, true, Collections.emptyList()
                },
                // (04)
                {
                    null, null, true, Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), 
                                                    createCloudBuild(State.FAILED, parseDate("21-03-2018"), null))
                },
                // (05)
                {
                    null, null, false, Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), 
                                                     createCloudBuild(State.STAGED, parseDate("21-03-2018"), null))
                },
// @formatter:on
            });
        }

        private final String expectedIOExceptionMessage;
        private final String expectedCFExceptionMessage;
        private final MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
        private final List<CloudBuild> cloudBuilds;
        private final boolean shouldUpload;

        private File appFile;

        public UploadAppStepParameterizedTest(String expectedIOExceptionMessage, String expectedCFExceptionMessage, boolean shouldUpload,
                                              List<CloudBuild> cloudBuilds) {
            this.expectedIOExceptionMessage = expectedIOExceptionMessage;
            this.expectedCFExceptionMessage = expectedCFExceptionMessage;
            this.shouldUpload = shouldUpload;
            this.cloudBuilds = cloudBuilds;
        }

        @Before
        public void setUp() throws Exception {
            loadParameters();
            prepareFileService();
            prepareContext();
            prepareClients();
        }

        @After
        public void tearDown() {
            FileUtils.deleteQuietly(appFile.getParentFile());
        }

        @Test
        public void test() {
            try {
                step.execute(context);
            } catch (Throwable e) {
                e.printStackTrace();
                assertFalse(appFile.exists());
                throw e;
            }

            assertCall(Constants.VAR_UPLOAD_TOKEN, JsonUtil.toJson(UPLOAD_TOKEN));
        }

        private void assertCall(String variableName, String variableValue) {
            int numberOfCalls = shouldUpload ? 1 : 0;
            Mockito.verify(context, Mockito.times(numberOfCalls))
                   .setVariable(variableName, variableValue);
        }

        public void loadParameters() {
            if (expectedIOExceptionMessage != null) {
                expectedException.expectMessage(expectedIOExceptionMessage);
                expectedException.expect(SLException.class);
            }
            if (expectedCFExceptionMessage != null) {
                expectedException.expectMessage(expectedCFExceptionMessage);
                expectedException.expect(SLException.class);
            }
        }

        public void prepareContext() {
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .moduleName(APP_NAME)
                                                                            .build();
            execution.setVariable(Variables.APP_TO_PROCESS, app);
            context.setVariable(Constants.VAR_MODULES_INDEX, 0);
            context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE);
            mtaArchiveElements.addModuleFileName(APP_NAME, APP_FILE);
            execution.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
            StepsUtil.setVcapAppPropertiesChanged(context, false);
            when(configuration.getMaxResourceFileSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE);
        }

        public void prepareClients() throws Exception {
            if (expectedIOExceptionMessage == null && expectedCFExceptionMessage == null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenReturn(UPLOAD_TOKEN);
            } else if (expectedIOExceptionMessage != null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(IO_EXCEPTION);
            } else if (expectedCFExceptionMessage != null) {
                when(client.asyncUploadApplication(eq(APP_NAME), eq(appFile), any())).thenThrow(CO_EXCEPTION);
            }

            ImmutableCloudApplicationExtended application = createApplication(APP_NAME, 2, cloudBuilds == null ? null : MODULE_DIGEST);
            when(client.getApplication(APP_NAME)).thenReturn(application);
            when(client.getBuildsForApplication(application.getMetadata()
                                                           .getGuid())).thenReturn(cloudBuilds);
        }

        private ImmutableCloudApplicationExtended createApplication(String appName, int instances, String digest) {
            Map<String, Object> deployAttributes = new HashMap<>();
            deployAttributes.put(com.sap.cloud.lm.sl.cf.core.Constants.ATTR_APP_CONTENT_DIGEST, digest);
            return ImmutableCloudApplicationExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .name(appName)
                                                    .moduleName(appName)
                                                    .putEnv(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                                                            JsonUtil.toJson(deployAttributes))
                                                    .instances(instances)
                                                    .build();
        }

        private static CloudBuild createCloudBuild(State state, Date createdAt, String error) {
            return ImmutableCloudBuild.builder()
                                      .metadata(ImmutableCloudMetadata.builder()
                                                                      .guid(UUID.randomUUID())
                                                                      .createdAt(createdAt)
                                                                      .build())
                                      .dropletInfo(ImmutableDropletInfo.builder()
                                                                       .guid(UUID.randomUUID())
                                                                       .build())
                                      .state(state)
                                      .error(error)
                                      .build();
        }

        private static Date parseDate(String date) {
            try {
                return new SimpleDateFormat(DATE_PATTERN).parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
                throw new RuntimeException("Invalid Date!");
            }
        }

        public void prepareFileService() throws Exception {
            tempDir.create();
            appFile = tempDir.newFile(APP_FILE);
            doAnswer((Answer<Void>) invocation -> {
                FileContentProcessor contentProcessor = invocation.getArgument(2);
                contentProcessor.processFileContent(null);
                return null;
            }).when(fileService)
              .processFileContent(Mockito.anyString(), Mockito.anyString(), Mockito.any());
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
                    protected Path createTempFile() {
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
            execution.setVariable(Variables.APP_TO_PROCESS, app);

            int uploadTimeout = step.getTimeout(execution);
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
            execution.setVariable(Variables.APP_TO_PROCESS, app);
            context.setVariable(Constants.VAR_MODULES_INDEX, 0);
            context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE);
            MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
            mtaArchiveElements.addModuleFileName(APP_NAME, APP_NAME);
            execution.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
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
