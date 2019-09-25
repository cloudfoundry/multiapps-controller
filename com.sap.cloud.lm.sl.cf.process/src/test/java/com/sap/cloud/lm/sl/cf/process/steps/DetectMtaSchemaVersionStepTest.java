package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.SchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.message.Messages;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Version;

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
                "1.0.0", 1,  format(Messages.UNSUPPORTED_VERSION, "1.0.0"),
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
                "0.1.0", 0, format(Messages.UNSUPPORTED_VERSION, "0.1.0"),
            },
// @formatter:on
        });
    }

    @Mock
    public SchemaVersionDetector versionDetector;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String schemaVersion;
    private int expectedMajor;
    private String expectedExceptionMessage;

    public DetectMtaSchemaVersionStepTest(String schemaVersion, int expectedMajor, String expectedExceptionMessage) {
        this.schemaVersion = schemaVersion;
        this.expectedMajor = expectedMajor;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        step.detectorSupplier = () -> versionDetector;

        StepsUtil.setDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setExtensionDescriptorChain(context, Collections.emptyList());
    }

    @Test
    public void testExecute1() throws Exception {
        when(versionDetector.detect(any(), any())).thenReturn(Version.parseVersion(schemaVersion));
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }

        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(expectedMajor, context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION));
    }

    @Test
    public void testExecute2() throws Exception {
        expectedException.expectMessage("Error");

        when(versionDetector.detect(any(), any())).thenThrow(new SLException("Error"));
        step.execute(context);
    }

    @Override
    protected DetectMtaSchemaVersionStep createStep() {
        return new DetectMtaSchemaVersionStep();
    }

}
