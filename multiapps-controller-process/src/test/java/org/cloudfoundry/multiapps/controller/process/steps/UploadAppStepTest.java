package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UploadAppStepTest {

    @Nested
    class UploadAppStepTimeoutTest extends SyncFlowableStepTest<UploadAppStep> {

        private static final String APP_NAME = "sample-app-backend";

        @BeforeEach
        public void prepareContext() {
            context.setVariable(Variables.MODULES_INDEX, 0);
            step.initializeStepLogger(execution);
        }

        @Test
        void testGetTimeoutWithoutAppParameter() {
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .build();

            testGetTimeout(app, UploadAppStep.DEFAULT_APP_UPLOAD_TIMEOUT);
        }

        @Test
        void testGetTimeoutWithAppParameter() {
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .env(Map.of(Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                                        "{\"upload-timeout\":1800}"))
                                                                            .build();

            testGetTimeout(app, 1800);
        }

        private void testGetTimeout(CloudApplicationExtended app, int expectedUploadTimeout) {
            context.setVariable(Variables.APP_TO_PROCESS, app);

            var expectedTimeout = Duration.ofSeconds(expectedUploadTimeout);
            var actualTimeout = step.getTimeout(context);
            assertEquals(expectedTimeout, actualTimeout);
        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStep();
        }

    }

    @Nested
    class UploadAppStepWithoutFileNameTest extends SyncFlowableStepTest<UploadAppStep> {
        private static final String SPACE = "space";
        private static final String APP_NAME = "simple-app";
        private static final String APP_ARCHIVE = "sample-app.mtar";

        @BeforeEach
        public void setUp() {
            prepareContext();
        }

        private void prepareContext() {
            // module name must be null
            CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                            .name(APP_NAME)
                                                                            .build();
            context.setVariable(Variables.APP_TO_PROCESS, app);
            context.setVariable(Variables.MODULES_INDEX, 0);
            context.setVariable(Variables.APP_ARCHIVE_ID, APP_ARCHIVE);
            context.setVariable(Variables.SPACE_GUID, SPACE);
            MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
            mtaArchiveElements.addModuleFileName(APP_NAME, APP_NAME);
            context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
        }

        @Test
        void testWithMissingFileNameMustReturnDone() {
            step.execute(execution);
            assertStepFinishedSuccessfully();
        }

        @Override
        protected UploadAppStep createStep() {
            return new UploadAppStep();
        }

    }

}
