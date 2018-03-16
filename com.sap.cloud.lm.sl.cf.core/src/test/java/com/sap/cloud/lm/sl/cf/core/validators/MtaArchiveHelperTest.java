package com.sap.cloud.lm.sl.cf.core.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

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
        helper = new MtaArchiveHelper(ArchiveHandler.getManifest(stream, Configuration.getInstance()
            .getMaxManifestSize()));

        DescriptorParser parser = new DescriptorParser();
        descriptor = parser.parseDeploymentDescriptorYaml(getClass().getResourceAsStream(deploymentDescriptorLocation));
    }

    @Before
    public void setUp() throws SLException {
        helper.init();
    }

    @Test
    public void testModules() {
        Set<String> descriptorModules = getModulesNamesFromDescriptor();
        Set<String> mtaModules = CloudModelBuilderUtil.getModuleNames(descriptor);

        assertEquals(descriptorModules.size(), mtaModules.size());
        for (String moduleName : mtaModules) {
            assertTrue(descriptorModules.contains(moduleName));
        }
    }

    @Test
    public void testResources() {
        Set<String> descriptorResources = getResourcesNamesFromDescriptor();
        Set<String> mtaResources = helper.getMtaArchiveResources()
            .keySet();

        assertEquals(descriptorResources.size(), mtaResources.size());

        for (String resourceName : mtaResources) {
            assertTrue(descriptorResources.contains(resourceName));
        }
    }

    @Test
    public void testDependencies() {
        Set<String> descriptorDependencies = getRequiredDependenciesNamesFromDescriptor();
        Set<String> mtaDependencies = helper.getMtaRequiresDependencies()
            .keySet();

        assertEquals(descriptorDependencies.size(), mtaDependencies.size());

        for (String dependencyName : mtaDependencies) {
            assertTrue(descriptorDependencies.contains(dependencyName));
        }
    }

    private Set<String> getModulesNamesFromDescriptor() {
        return descriptor.getModules1_0()
            .stream()
            .map(m -> m.getName())
            .collect(Collectors.toSet());
    }

    private Set<String> getResourcesNamesFromDescriptor() {
        return descriptor.getResources1_0()
            .stream()
            .map(r -> r.getName())
            .collect(Collectors.toSet());
    }

    private Set<String> getRequiredDependenciesNamesFromDescriptor() {
        return descriptor.getModules1_0()
            .stream()
            .map(m -> m.getRequiredDependencies1_0())
            .flatMap(d -> d.stream())
            .collect(Collectors.toSet());
    }

}
