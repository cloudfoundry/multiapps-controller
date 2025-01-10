package org.cloudfoundry.multiapps.controller.core.helpers;

import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void testGetResourceFileAttributes() throws IOException {
        Manifest manifest = getManifest();
        helper = new MtaArchiveHelper(manifest);
        Map<String, List<String>> result = helper.getResourceFileAttributes();
        assertEquals(Map.of("config.json", List.of("parameters-service", "parameters-service-2")), result);
    }

    @NotNull
    private static Manifest getManifest() throws IOException {
        // @formatter:off
        String manifestContent = "Manifest-Version: 1.0\n\n"
            + "Name: config-bind.json\n"
            + "MTA-Requires: anatz/my-required-application, anatz/my-required-application-2\n"
            + "Content-Type: application/json\n\n"
            + "Name: config.json\n"
            + "MTA-Resource: parameters-service, parameters-service-2\n"
            + "Content-Type: application/json\n";
        // @formatter:on

        InputStream mockInputStream = new ByteArrayInputStream(manifestContent.getBytes());

        Manifest manifest = new Manifest(mockInputStream);
        return manifest;
    }

    @Test
    void getRequiresDependenciesFileAttributes() throws IOException {
        Manifest manifest = getManifest();
        helper = new MtaArchiveHelper(manifest);
        Map<String, List<String>> result = helper.getRequiresDependenciesFileAttributes();
        assertEquals(Map.of("config-bind.json", List.of("anatz/my-required-application", "anatz/my-required-application-2")), result);
    }

    private Manifest createFakeManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Main-Class", "com.example.Main");
        return manifest;
    }

    private byte[] manifestToByteArray(Manifest manifest) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            manifest.write(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
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
