package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UrisClassifierTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0) The application was deployed with an old version of the deploy-service and there's no
            // 'descriptor-defined-uris' deploy attribute in its environment. There are, however, URIs assigned to it:
            {
                "deployed-module-0.json", Arrays.asList("localhost:50100", "localhost:50200"), Collections.emptyList(),
            },
            // (1) The deploy attributes of the application are missing:
            {
                "deployed-module-1.json", Arrays.asList("localhost:50100", "localhost:50200"), Collections.emptyList(),
            },
            // (2) There is a 'descriptor-defined-uris' attribute and they are the same as the URIs assigned to the
            // application:
            {
                "deployed-module-2.json", Arrays.asList("localhost:50100", "localhost:50200"), Collections.emptyList(),
            },
            // (3) There is a 'descriptor-defined-uris' attribute and there are additional URIs assigned to the
            // application:
            {
                "deployed-module-3.json", Arrays.asList("localhost:50100", "localhost:50200"), Arrays.asList("localhost:50300", "localhost:50400"),
            },
            // (4) There is a 'descriptor-defined-uris' attribute, but some of them are no longer assigned to the
            // application:
            {
                "deployed-module-4.json", Arrays.asList("localhost:50100"), Collections.emptyList(),
            },
            // (5) There is a 'descriptor-defined-uris' attribute, but some of them are no longer assigned to the
            // application and there are new ones:
            {
                "deployed-module-5.json", Arrays.asList("localhost:50100"), Arrays.asList("localhost:50300"),
            },
            // (6) There are no descriptor defined URIs and there are no URIs assigned to the application:
            {
                "deployed-module-6.json", Collections.emptyList(), Collections.emptyList(),
            },
            // (7) There are no descriptor defined URIs and there are URIs assigned to the application:
            {
                "deployed-module-7.json", Collections.emptyList(), Arrays.asList("localhost:50100", "localhost:50200"),
            },
            // (7) There is a 'descriptor-defined-uris' attribute, it contains XSA placeholders and there are new URIs
            // assigned to the application:
            {
                "deployed-module-8.json", Arrays.asList("localhost:50100", "localhost:50200"), Arrays.asList("localhost:50300", "localhost:50400"),
            },
            // (7) The URIs are host-based:
            {
                "deployed-module-9.json", Arrays.asList("host1.localhost", "host2.localhost"), Arrays.asList("host3.localhost", "host4.localhost"),
            },
            // @formatter:on
        });
    }

    private final String deployedModuleJsonLocation;
    private final List<String> expectedDescriptorDefinedUris;
    private final List<String> expectedCustomUris;

    private DeployedMtaModule deployedModule;

    public UrisClassifierTest(String deployedModuleJsonLocation, List<String> expectedDescriptorDefinedUris,
        List<String> expectedCustomUris) {
        this.deployedModuleJsonLocation = deployedModuleJsonLocation;
        this.expectedDescriptorDefinedUris = expectedDescriptorDefinedUris;
        this.expectedCustomUris = expectedCustomUris;
    }

    @Before
    public void setUp() throws Exception {
        String deployedModuleJson = TestUtil.getResourceAsString(deployedModuleJsonLocation, getClass());
        this.deployedModule = JsonUtil.fromJson(deployedModuleJson, DeployedMtaModule.class);
    }

    @Test
    public void testGetDescriptorDefinedUris() throws Exception {
        UrisClassifier urisClassifier = getUrisClassifier();
        List<String> result = urisClassifier.getDescriptorDefinedUris(deployedModule);
        assertEquals(expectedDescriptorDefinedUris, result);
    }

    @Test
    public void testGetCustomUris() throws Exception {
        UrisClassifier urisClassifier = getUrisClassifier();
        List<String> result = urisClassifier.getCustomUris(deployedModule);
        assertEquals(expectedCustomUris, result);
    }

    private UrisClassifier getUrisClassifier() {
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDefaultDomain("localhost");
        return new UrisClassifier(xsPlaceholderResolver);
    }

}
