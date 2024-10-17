package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UploadAppStepTest {

    @Nested
    class UploadAppStepTimeoutTest extends SyncFlowableStepTest<UploadAppStep> {

        @BeforeEach
        public void prepareContext() {
            context.setVariable(Variables.MODULES_INDEX, 0);
            step.initializeStepLogger(execution);
        }

        @ParameterizedTest
        @MethodSource("testValidatePriority")
        void testGetTimeout(Integer timeoutProcessVariable, Integer timeoutModuleLevel, Integer timeoutGlobalLevel, int expectedTimeout) {
            setUpContext(timeoutProcessVariable, timeoutModuleLevel, timeoutGlobalLevel, Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE,
                         SupportedParameters.UPLOAD_TIMEOUT, SupportedParameters.APPS_UPLOAD_TIMEOUT);

            Duration actualTimeout = step.getTimeout(context);
            assertEquals(Duration.ofSeconds(expectedTimeout), actualTimeout);
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
            context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
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
