package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.Messages;
import org.cloudfoundry.multiapps.mta.handlers.SchemaVersionDetector;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

class DetectMtaSchemaVersionStepTest extends SyncFlowableStepTest<DetectMtaSchemaVersionStep> {

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("build-cloud-model.yaml",
                                                                                                                  DetectMtaSchemaVersionStepTest.class);

    @Mock
    public SchemaVersionDetector versionDetector;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // Unsupported version:
            Arguments.of("1.0.0", 1,  MessageFormat.format(Messages.UNSUPPORTED_VERSION, "1.0.0")),
            // Supported version:
            Arguments.of("2.0.0", 2, null),
            // Supported version:
            Arguments.of("2.1.0", 2, null),
            // Supported version:
            Arguments.of("2", 2, null),
            // Supported version:
            Arguments.of("2.1", 2, null),
            // Supported version:
            Arguments.of("3", 3, null),
            // Unsupported version:
            Arguments.of("0.1.0", 0, MessageFormat.format(Messages.UNSUPPORTED_VERSION, "0.1.0"))
// @formatter:on
        );
    }

    @BeforeEach
    public void initialize() {
        step.detectorSupplier = () -> versionDetector;
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
        context.setVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, Collections.emptyList());
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String schemaVersion, int expectedMajor, String expectedExceptionMessage) {
        when(versionDetector.detect(any(), any())).thenReturn(Version.parseVersion(schemaVersion));
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals((Integer) expectedMajor, context.getVariable(Variables.MTA_MAJOR_SCHEMA_VERSION));
    }

    @Test
    void testExecuteWhenVersionDetectorThrowsError() {
        when(versionDetector.detect(any(), any())).thenThrow(new SLException("Error"));
        Exception exception = assertThrows(Exception.class, () -> step.execute(execution));
        assertTrue(exception.getMessage()
                            .contains("Error"));
    }

    @Override
    protected DetectMtaSchemaVersionStep createStep() {
        return new DetectMtaSchemaVersionStep();
    }

}
