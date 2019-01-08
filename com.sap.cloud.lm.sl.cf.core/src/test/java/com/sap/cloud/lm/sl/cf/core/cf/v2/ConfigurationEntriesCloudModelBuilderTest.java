package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.HandlerFactory;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class ConfigurationEntriesCloudModelBuilderTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Test that provided dependencies from version 2 are public by default:
            {
                "mtad-07-v2.yaml", 2, "some-org", "some-space", new Expectation(Expectation.Type.RESOURCE, "expected-configuration-entries-01.json"),
            },
            // (1) Test that provided dependencies from version 3 are not public by default:
            {
                "mtad-07-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.RESOURCE, "expected-configuration-entries-02.json"),
            },
            // (2) Test that explicitly public provided dependencies are converted to configuration entries:
            {
                "mtad-08-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.RESOURCE, "expected-configuration-entries-03.json"),
            },
            // (3) Test that public provided dependencies from all modules are converted to configuration entries:
            {
                "mtad-09-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.RESOURCE, "expected-configuration-entries-04.json"),
            },
            // (4) Test that the provided dependencies' properties are added in the created configuration entries:
            {
                "mtad-10-v2.yaml", 3, "some-org", "some-space", new Expectation(Expectation.Type.RESOURCE, "expected-configuration-entries-05.json"),
            },
// @formatter:on
        });
    }

    private String deploymentDescriptorLocation;
    private int majorSchemaVersion;
    private String orgName;
    private String spaceName;
    private Expectation expectation;

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
        TestUtil.test(() -> builder.build(deploymentDescriptor), expectation, getClass());
    }

}
