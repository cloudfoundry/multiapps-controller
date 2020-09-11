package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.Messages;
import org.cloudfoundry.multiapps.mta.handlers.SchemaVersionDetector;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

@RunWith(Parameterized.class)
public class DetectMtaSchemaVersionStepTest extends SyncFlowableStepTest<DetectMtaSchemaVersionStep> {

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("build-cloud-model.yaml",
                                                                                                                  DetectMtaSchemaVersionStepTest.class);

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // Unsupported version:
            {
                "1.0.0", 1,  MessageFormat.format(Messages.UNSUPPORTED_VERSION, "1.0.0"),
            },
            // Supported version:
            {
                "2.0.0", 2, null,
            },
            // Supported version:
            {
                "2.1.0", 2, null,
            },
            // Supported version:
            {
                "2", 2, null,
            },
            // Supported version:
            {
                "2.1", 2, null,
            },
            // Supported version:
            {
                "3", 3, null,
            },
            // Unsupported version:
            {
                "0.1.0", 0, MessageFormat.format(Messages.UNSUPPORTED_VERSION, "0.1.0"),
            },
// @formatter:on
        });
    }

    @Mock
    public SchemaVersionDetector versionDetector;
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final String schemaVersion;
    private final int expectedMajor;
    private final String expectedExceptionMessage;

    public DetectMtaSchemaVersionStepTest(String schemaVersion, int expectedMajor, String expectedExceptionMessage) {
        this.schemaVersion = schemaVersion;
        this.expectedMajor = expectedMajor;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() {
        step.detectorSupplier = () -> versionDetector;

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
        context.setVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, Collections.emptyList());
    }

    @Test
    public void testExecute1() {
        when(versionDetector.detect(any(), any())).thenReturn(Version.parseVersion(schemaVersion));
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }

        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals((Integer) expectedMajor, context.getVariable(Variables.MTA_MAJOR_SCHEMA_VERSION));
    }

    @Test
    public void testExecute2() {
        expectedException.expectMessage("Error");

        when(versionDetector.detect(any(), any())).thenThrow(new SLException("Error"));
        step.execute(execution);
    }

    @Override
    protected DetectMtaSchemaVersionStep createStep() {
        return new DetectMtaSchemaVersionStep();
    }

}
