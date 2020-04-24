package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.HandlerFactory;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ConfigurationEntriesCloudModelBuilderTest {

    private final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Test that provided dependencies without properties from version 2 that are public by default do not result in configuration entries:
            {
                "mtad-07-v2.yaml", 2, "some-org", "some-space", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json"),
            },
            // (1) Test that provided dependencies without properties from version 3 that are not public by default do not result in configuration entries:
            {
                "mtad-07-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json"),
            },
            // (2) Test that explicitly public provided dependencies without properties do not result in configuration entries:
            {
                "mtad-08-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json"),
            },
            // (3) Test that provided dependencies with properties from version 2 that are public by default are converted in configuration entries:
            {
                "mtad-09-v2.yaml", 2, "some-org", "some-space", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-01.json"),
            },
            // (4) Test that public provided dependencies with properties from all modules are converted to configuration entries:
            {
                "mtad-10-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-02.json"),
            },
// @formatter:on
        });
    }

    private final String deploymentDescriptorLocation;
    private final int majorSchemaVersion;
    private final String orgName;
    private final String spaceName;
    private final Expectation expectation;

    private DeploymentDescriptor deploymentDescriptor;

    public ConfigurationEntriesCloudModelBuilderTest(String deploymentDescriptorLocation, int majorSchemaVersion, String orgName,
                                                     String spaceName, Expectation expectation) {
        this.deploymentDescriptorLocation = deploymentDescriptorLocation;
        this.majorSchemaVersion = majorSchemaVersion;
        this.orgName = orgName;
        this.spaceName = spaceName;
        this.expectation = expectation;
    }

    @Before
    public void parseDeploymentDescriptor() {
        DescriptorParser parser = new HandlerFactory(majorSchemaVersion).getDescriptorParser();
        InputStream deploymentDescriptorStream = getClass().getResourceAsStream(deploymentDescriptorLocation);
        this.deploymentDescriptor = parser.parseDeploymentDescriptorYaml(deploymentDescriptorStream);
    }

    @Test
    public void testBuild() {
        ConfigurationEntriesCloudModelBuilder builder = new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, "");
        tester.test(() -> builder.build(deploymentDescriptor), expectation);
    }

}
