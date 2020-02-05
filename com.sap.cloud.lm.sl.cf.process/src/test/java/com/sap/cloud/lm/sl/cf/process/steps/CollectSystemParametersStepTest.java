package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParameters;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

public class CollectSystemParametersStepTest extends CollectSystemParametersStepBaseTest {

    private static final String DEFAULT_PROTOCOL = "https";

    @Test
    public void testGeneralParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(context);
        Map<String, Object> generalParameters = descriptor.getParameters();
        assertEquals(USER, generalParameters.get(SupportedParameters.USER));
        assertEquals(ORG, generalParameters.get(SupportedParameters.ORG));
        assertEquals(SPACE, generalParameters.get(SupportedParameters.SPACE));
        assertEquals(DEFAULT_DOMAIN, generalParameters.get(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals(AUTHORIZATION_URL, generalParameters.get(SupportedParameters.XS_AUTHORIZATION_ENDPOINT));
        assertEquals(AUTHORIZATION_URL, generalParameters.get(SupportedParameters.AUTHORIZATION_URL));
        assertEquals(CONTROLLER_URL, generalParameters.get(SupportedParameters.XS_TARGET_API_URL));
        assertEquals(CONTROLLER_URL, generalParameters.get(SupportedParameters.CONTROLLER_URL));
        assertEquals(MULTIAPPS_CONTROLLER_URL, generalParameters.get(SupportedParameters.DEPLOY_SERVICE_URL));
    }

    private DeployedMta createDeployedMta(String version, List<DeployedMtaApplication> deployedApplications) {
        MtaMetadata metadata = createMtaMetadata(version);
        return ImmutableDeployedMta.builder()
                                   .metadata(metadata)
                                   .applications(deployedApplications)
                                   .services(Collections.emptyList())
                                   .build();
    }

    private MtaMetadata createMtaMetadata(String version) {
        return ImmutableMtaMetadata.builder()
                                   .id("system-parameters-test")
                                   .version(Version.parseVersion(version))
                                   .build();
    }

    @Test
    public void testWithHostBasedRouting() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(context);
        Mockito.verify(readOnlyParametersChecker)
               .check(any());
        List<Module> modules = descriptor.getModules();
        assertEquals(2, modules.size());
        for (Module module : modules) {
            validateHostBasedModuleParameters(module);
        }
    }

    @Test
    public void testWithRoutePath() {
        prepareDescriptor("system-parameters/mtad-with-route-path.yaml");
        prepareClient();

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(context);
        List<Module> modules = descriptor.getModules();
        assertEquals(1, modules.size());
        validateHostBasedModuleParameters(modules.get(0), "/foo");
    }

    private void validateHostBasedModuleParameters(Module module) {
        validateHostBasedModuleParameters(module, null);
    }

    private void validateHostBasedModuleParameters(Module module, String path) {
        Map<String, Object> parameters = module.getParameters();

        String expectedDefaultHost = computeExpectedDefaultHost(module);
        String expectedDefaultUri = computeExpectedDefaultUri(path);
        assertEquals(expectedDefaultHost, parameters.get(SupportedParameters.DEFAULT_HOST));
        assertEquals("${default-host}", parameters.get(SupportedParameters.HOST));
        assertEquals(expectedDefaultUri, parameters.get(SupportedParameters.DEFAULT_URI));
        assertEquals(SystemParameters.DEFAULT_URL, parameters.get(SupportedParameters.DEFAULT_URL));
        assertFalse(parameters.containsKey(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals("${default-domain}", parameters.get(SupportedParameters.DOMAIN));
        assertEquals(DEFAULT_PROTOCOL, parameters.get(SupportedParameters.PROTOCOL));
    }

    private String computeExpectedDefaultHost(Module module) {
        return String.format("%s-%s-%s", ORG, SPACE, module.getName());
    }

    private String computeExpectedDefaultUri(String path) {
        return SystemParameters.DEFAULT_HOST_BASED_URI + (path == null ? "" : SystemParameters.ROUTE_PATH_PLACEHOLDER);
    }

    @Test
    public void testGeneralModuleAndResourceParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        when(credentialsGenerator.next(anyInt())).thenReturn("abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "vwx");

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(context);
        List<Module> modules = descriptor.getModules();
        assertEquals(2, modules.size());
        Module foo = modules.get(0);
        validateGeneralModuleParameters(foo, "abc", "def");
        Module bar = modules.get(1);
        validateGeneralModuleParameters(bar, "ghi", "jkl");

        List<Resource> resources = descriptor.getResources();
        assertEquals(2, resources.size());
        Resource baz = resources.get(0);
        validateGeneralResourceParameters(baz, "mno", "pqr");
        Resource qux = resources.get(1);
        validateGeneralResourceParameters(qux, "stu", "vwx");
    }

    private void validateGeneralModuleParameters(Module module, String expectedGeneratedUsername, String expectedGeneratedPassword) {
        Map<String, Object> parameters = module.getParameters();
        assertEquals(DEFAULT_TIMESTAMP, parameters.get(SupportedParameters.TIMESTAMP));
        assertEquals(expectedGeneratedUsername, parameters.get(SupportedParameters.GENERATED_USER));
        assertEquals(expectedGeneratedPassword, parameters.get(SupportedParameters.GENERATED_PASSWORD));
        assertEquals(module.getName(), parameters.get(SupportedParameters.DEFAULT_APP_NAME));
        assertEquals("${default-app-name}", parameters.get(SupportedParameters.APP_NAME));
    }

    private void validateGeneralResourceParameters(Resource resource, String expectedGeneratedUsername, String expectedGeneratedPassword) {
        Map<String, Object> parameters = resource.getParameters();
        assertEquals(expectedGeneratedUsername, parameters.get(SupportedParameters.GENERATED_USER));
        assertEquals(expectedGeneratedPassword, parameters.get(SupportedParameters.GENERATED_PASSWORD));
        assertEquals(resource.getName(), parameters.get(SupportedParameters.DEFAULT_SERVICE_NAME));
        assertEquals("${default-service-name}", parameters.get(SupportedParameters.SERVICE_NAME));
    }

    @Test(expected = ContentException.class)
    public void testVersionRuleWithDowngrade() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        StepsUtil.setDeployedMta(context, createDeployedMta("2.0.0", Collections.emptyList()));

        step.execute(context);
    }

    @Test(expected = ContentException.class)
    public void testVersionRuleWithReinstallation() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        context.setVariable(Constants.PARAM_VERSION_RULE, VersionRule.HIGHER.toString());
        StepsUtil.setDeployedMta(context, createDeployedMta("1.0.0", Collections.emptyList()));

        step.execute(context);
    }

    @Test
    public void testExistingParametersAreNotOverridden() {
        prepareDescriptor("system-parameters/mtad-with-existing-parameters.yaml");
        prepareClient();

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptorWithSystemParameters(context);
        List<Module> modules = descriptor.getModules();
        assertEquals(1, modules.size());
        Module foo = modules.get(0);
        assertEquals("bar", foo.getParameters()
                               .get(SupportedParameters.APP_NAME));

        List<Resource> resources = descriptor.getResources();
        assertEquals(1, resources.size());
        Resource baz = resources.get(0);
        assertEquals("qux", baz.getParameters()
                               .get(SupportedParameters.SERVICE_NAME));
    }

}
