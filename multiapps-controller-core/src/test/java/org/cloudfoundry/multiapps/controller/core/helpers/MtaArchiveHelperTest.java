package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MtaArchiveHelperTest {

    private static MtaArchiveHelper helper;
    private static DeploymentDescriptor descriptor;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // (0) Without modules and resources
            { "mta-archive-helper-1.mtar", "mta-archive-helper-1.yaml" },
            // (1) With modules and resources
            { "mta-archive-helper-2.mtar", "mta-archive-helper-2.yaml" } });
    }

    public MtaArchiveHelperTest(String mtarLocation, String deploymentDescriptorLocation) throws SLException {
        InputStream stream = getClass().getResourceAsStream(mtarLocation);
        helper = new MtaArchiveHelper(ArchiveHandler.getManifest(stream, ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE));

        DescriptorParser parser = new DescriptorParser();
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream(deploymentDescriptorLocation));
        descriptor = parser.parseDeploymentDescriptor(deploymentDescriptorMap);
    }

    @Before
    public void setUp() throws SLException {
        helper.init();
    }

    @Test
    public void testResources() {
        Set<String> descriptorResources = getResourcesNamesFromDescriptor();
        Set<String> mtaResources = helper.getMtaArchiveResources()
                                         .keySet();

        assertEquals(descriptorResources, mtaResources);
    }

    @Test
    public void testDependencies() {
        Set<String> descriptorDependencies = getRequiredDependenciesNamesFromDescriptor();
        Set<String> mtaDependencies = helper.getMtaRequiresDependencies()
                                            .keySet();

        assertEquals(descriptorDependencies, mtaDependencies);
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
