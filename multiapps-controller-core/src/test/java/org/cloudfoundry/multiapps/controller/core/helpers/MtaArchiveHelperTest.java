package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MtaArchiveHelperTest {

    private MtaArchiveHelper helper;
    private DeploymentDescriptor descriptor;

    public static Stream<Arguments> getParameters() {
        return Stream.of(
        // @formatter:off
            // (0) Without modules and resources
            Arguments.of("mta-archive-helper-1.mtar", "mta-archive-helper-1.yaml"),
            // (1) With modules and resources
            Arguments.of("mta-archive-helper-2.mtar", "mta-archive-helper-2.yaml"));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testResources(String mtarLocation, String deploymentDescriptorLocation) {
        initializeParameters(mtarLocation, deploymentDescriptorLocation);
        Set<String> descriptorResources = getResourcesNamesFromDescriptor();
        Set<String> mtaResources = helper.getMtaArchiveResources()
                                         .keySet();
        assertEquals(descriptorResources, mtaResources);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testDependencies(String mtarLocation, String deploymentDescriptorLocation) {
        initializeParameters(mtarLocation, deploymentDescriptorLocation);
        Set<String> descriptorDependencies = getRequiredDependenciesNamesFromDescriptor();
        Set<String> mtaDependencies = helper.getMtaRequiresDependencies()
                                            .keySet();
        assertEquals(descriptorDependencies, mtaDependencies);
    }

    private void initializeParameters(String mtarLocation, String deploymentDescriptorLocation) {
        InputStream stream = getClass().getResourceAsStream(mtarLocation);
        helper = new MtaArchiveHelper(ArchiveHandler.getManifest(stream, ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE));

        DescriptorParser parser = new DescriptorParser();
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream(deploymentDescriptorLocation));
        descriptor = parser.parseDeploymentDescriptor(deploymentDescriptorMap);
        helper.init();
    }

    private Set<String> getResourcesNamesFromDescriptor() {
        return descriptor.getResources()
                         .stream()
                         .map(Resource::getName)
                         .collect(Collectors.toSet());
    }

    private Set<String> getRequiredDependenciesNamesFromDescriptor() {
        return descriptor.getModules()
                         .stream()
                         .map(Module::getRequiredDependencies)
                         .flatMap(List::stream)
                         .map(RequiredDependency::getName)
                         .collect(Collectors.toSet());
    }

}
