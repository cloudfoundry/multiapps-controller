package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigurationEntriesCloudModelBuilderTest {

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testBuild() {
        return Stream.of(
        // @formatter:off
            // (0) Test that provided dependencies from version 2 are public by default:
            Arguments.of("mtad-07-v2.yaml", 2, "some-org", "some-space", null, new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json")),
            // (1) Test that provided dependencies from version 3 are not public by default:
            Arguments.of("mtad-07-v2.yaml", 3, "some-org", "some-space", null, new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json")),
            // (2) Test that explicitly public provided dependencies are converted to configuration entries:
            Arguments.of("mtad-08-v2.yaml", 3, "some-org", "some-space", null, new Expectation(Expectation.Type.JSON, "expected-configuration-entries-00.json")),
            // (3) Test that public provided dependencies from all modules are converted to configuration entries:
            Arguments.of("mtad-09-v2.yaml", 2, "some-org", "some-space", null, new Expectation(Expectation.Type.JSON, "expected-configuration-entries-01.json")),
            // (4) Test that the provided dependencies' properties are added in the created configuration entries:
            Arguments.of("mtad-10-v2.yaml", 3, "some-org", "some-space", null, new Expectation(Expectation.Type.JSON, "expected-configuration-entries-02.json")),
            // (5) Test with explicitly public provided dependencies and namespace:
            Arguments.of("mtad-10-v2.yaml", 3, "some-org", "some-space", "namespace", new Expectation(Expectation.Type.JSON, "expected-configuration-entries-03.json"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBuild(String deploymentDescriptorLocation, int majorSchemaVersion, String orgName, String spaceName, String namespace,
                   Expectation expectation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(deploymentDescriptorLocation, majorSchemaVersion);

        ConfigurationEntriesCloudModelBuilder builder = new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, "", namespace);
        tester.test(() -> builder.build(deploymentDescriptor), expectation);
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String deploymentDescriptorLocation, int majorSchemaVersion) {
        DescriptorParser parser = CloudHandlerFactory.forSchemaVersion(majorSchemaVersion)
                                                     .getDescriptorParser();
        InputStream deploymentDescriptorStream = getClass().getResourceAsStream(deploymentDescriptorLocation);
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(deploymentDescriptorStream);
        return parser.parseDeploymentDescriptor(deploymentDescriptorMap);
    }

}
