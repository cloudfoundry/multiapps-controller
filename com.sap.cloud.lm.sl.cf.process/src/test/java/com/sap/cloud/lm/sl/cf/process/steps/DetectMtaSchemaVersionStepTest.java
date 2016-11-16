package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.MtaSchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.message.Messages;
import com.sap.cloud.lm.sl.mta.model.Version;

@RunWith(Parameterized.class)
public class DetectMtaSchemaVersionStepTest extends AbstractStepTest<DetectMtaSchemaVersionStep> {

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // Supported version:
            {
                "1.0.0", 1, 0, null,
            },
            // Supported version:
            {
                "2.0.0", 2, 0, null,
            },
            // Supported version:
            {
                "2.1.0", 2, 1, null,
            },
            // Supported version:
            {
                "2", 2, 1, null,
            },
            // Supported version:
            {
                "2.1", 2, 1, null,
            },
            // Supported version:
            {
                "3", 3, 1, null,
            },
            // Unsupported version:
            {
                "0.1.0", 0, 1, format(Messages.UNSUPPORTED_VERSION, "0.1.0"),
            },
// @formatter:on
        });
    }

    @Mock
    public MtaSchemaVersionDetector versionDetector;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String schemaVersion;
    private int expectedMajor;
    private int expectedMinor;
    private String expectedExceptionMessage;

    public DetectMtaSchemaVersionStepTest(String schemaVersion, int expectedMajor, int expectedMinor, String expectedExceptionMessage) {
        this.schemaVersion = schemaVersion;
        this.expectedMajor = expectedMajor;
        this.expectedMinor = expectedMinor;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        step.detectorSupplier = () -> versionDetector;

        StepsUtil.setExtensionDescriptorStrings(context, Collections.emptyList());
        StepsUtil.setDeploymentDescriptorString(context, "");
    }

    @Test
    public void testExecute1() throws Exception {
        when(versionDetector.detect(any(), any())).thenReturn(Version.parseVersion(schemaVersion));
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }

        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        assertEquals(expectedMajor, context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION));
        assertEquals(expectedMinor, context.getVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION));
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
