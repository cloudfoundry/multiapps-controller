package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class CollectSystemParametersStepTest extends CollectSystemParametersStepBaseTest {

    private static final int USED_PORT = 50020;
    private static final String DEFAULT_PROTOCOL = "https";

    @Test
    public void testGeneralParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
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

    @Test
    public void testGeneralParametersWithXsaPlaceholders() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);

        when(configuration.areXsPlaceholdersSupported()).thenReturn(true);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        Map<String, Object> generalParameters = descriptor.getParameters();
        assertEquals(USER, generalParameters.get(SupportedParameters.USER));
        assertEquals(ORG, generalParameters.get(SupportedParameters.ORG));
        assertEquals(SPACE, generalParameters.get(SupportedParameters.SPACE));
        assertEquals(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER, generalParameters.get(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER,
            generalParameters.get(SupportedParameters.XS_AUTHORIZATION_ENDPOINT));
        assertEquals(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER,
            generalParameters.get(SupportedParameters.AUTHORIZATION_URL));
        assertEquals(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER, generalParameters.get(SupportedParameters.XS_TARGET_API_URL));
        assertEquals(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER, generalParameters.get(SupportedParameters.CONTROLLER_URL));
        assertEquals(SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER, generalParameters.get(SupportedParameters.DEPLOY_SERVICE_URL));
    }

    @Test
    public void testWithPortBasedRouting() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        for (int index = 0; index < modules.size(); index++) {
            Module module = modules.get(index);
            validatePortBasedModuleParameters(module, PortValidator.MIN_PORT_VALUE + index);
        }
    }

    @Test
    public void testWithTcpRouting() {
        prepareDescriptor("system-parameters/mtad-with-tcp.yaml");
        prepareClient(true);

        step.execute(context);

        verify(portAllocator).allocateTcpPort("foo", false);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        validatePortBasedModuleParameters(modules.get(0), PortValidator.MIN_PORT_VALUE, "tcp");
    }

    @Test
    public void testWithTcpRoutingAndExistingApplicationsWithHostBasedRoutes() {
        prepareDescriptor("system-parameters/mtad-with-tcp.yaml");
        prepareClient(false);
        List<DeployedMtaModule> deployedMtaModules = Arrays.asList(createDeployedMtaModule("foo", Arrays.asList("https://foo.localhost")));
        StepsUtil.setDeployedMta(context, createDeployedMta("1.0.0", deployedMtaModules));

        step.execute(context);

        verify(portAllocator).allocateTcpPort("foo", false);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        validatePortBasedModuleParameters(modules.get(0), PortValidator.MIN_PORT_VALUE, "tcp");
    }

    private DeployedMta createDeployedMta(String version, List<DeployedMtaModule> deployedModules) {
        DeployedMtaMetadata metadata = new DeployedMtaMetadata("system-parameters-test", Version.parseVersion(version));
        return new DeployedMta(metadata, deployedModules, Collections.emptySet());
    }

    private DeployedMtaModule createDeployedMtaModule(String name, List<String> uris) {
        return new DeployedMtaModule("foo", "foo", null, null, Collections.emptyList(), Collections.emptyList(), uris);
    }

    @Test
    public void testWithTcpsRouting() {
        prepareDescriptor("system-parameters/mtad-with-tcps.yaml");
        prepareClient(true);

        step.execute(context);

        verify(portAllocator).allocateTcpPort("foo", true);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        validatePortBasedModuleParameters(modules.get(0), PortValidator.MIN_PORT_VALUE, "tcps");
    }

    @Test(expected = ContentException.class)
    public void testWithTcpAndTcpsRouting() {
        prepareDescriptor("system-parameters/mtad-with-tcp-and-tcps.yaml");
        prepareClient(true);

        step.execute(context);
    }

    private void validatePortBasedModuleParameters(Module module, int expectedPort) {
        validatePortBasedModuleParameters(module, expectedPort, DEFAULT_PROTOCOL);
    }

    private void validatePortBasedModuleParameters(Module module, int expectedPort, String expectedProtocol) {
        Map<String, Object> parameters = module.getParameters();
        assertEquals(expectedPort, parameters.get(SupportedParameters.DEFAULT_PORT));
        assertEquals(expectedPort, parameters.get(SupportedParameters.PORT));
        assertEquals(SystemParametersBuilder.DEFAULT_PORT_BASED_URI, parameters.get(SupportedParameters.DEFAULT_URI));
        assertEquals(SystemParametersBuilder.DEFAULT_URL, parameters.get(SupportedParameters.DEFAULT_URL));
        assertEquals(DEFAULT_DOMAIN, parameters.get(SupportedParameters.DOMAIN));
        assertEquals(expectedProtocol, parameters.get(SupportedParameters.PROTOCOL));
    }

    @Test
    public void testWithHostBasedRouting() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(false);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        for (Module module : modules) {
            validateHostBasedModuleParameters(module);
        }
    }

    @Test
    public void testWithRoutePath() {
        prepareDescriptor("system-parameters/mtad-with-route-path.yaml");
        prepareClient(false);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
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
        assertEquals(expectedDefaultHost, parameters.get(SupportedParameters.HOST));
        assertEquals(expectedDefaultUri, parameters.get(SupportedParameters.DEFAULT_URI));
        assertEquals(SystemParametersBuilder.DEFAULT_URL, parameters.get(SupportedParameters.DEFAULT_URL));
        assertEquals(DEFAULT_DOMAIN, parameters.get(SupportedParameters.DOMAIN));
        assertEquals(DEFAULT_PROTOCOL, parameters.get(SupportedParameters.PROTOCOL));
    }

    private String computeExpectedDefaultHost(Module module) {
        return String.format("%s-%s-%s", ORG, SPACE, module.getName());
    }

    private String computeExpectedDefaultUri(String path) {
        return SystemParametersBuilder.DEFAULT_HOST_BASED_URI + (path == null ? "" : SystemParametersBuilder.ROUTE_PATH_PLACEHOLDER);
    }

    @Test
    public void testGeneralModuleAndResourceParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);
        when(credentialsGenerator.next(anyInt())).thenReturn("abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "vwx");

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        Module foo = modules.get(0);
        validateGeneralModuleParameters(foo, "abc", "def");
        Module bar = modules.get(1);
        validateGeneralModuleParameters(bar, "ghi", "jkl");

        List<Resource> resources = descriptor.getResources2();
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
        assertEquals(module.getName(), parameters.get(SupportedParameters.APP_NAME));
    }

    private void validateGeneralResourceParameters(Resource resource, String expectedGeneratedUsername, String expectedGeneratedPassword) {
        Map<String, Object> parameters = resource.getParameters();
        assertEquals(expectedGeneratedUsername, parameters.get(SupportedParameters.GENERATED_USER));
        assertEquals(expectedGeneratedPassword, parameters.get(SupportedParameters.GENERATED_PASSWORD));
        assertEquals(resource.getName(), parameters.get(SupportedParameters.SERVICE_NAME));
    }

    @Test
    public void testReuseOfPorts() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);
        List<DeployedMtaModule> deployedMtaModules = Arrays
            .asList(createDeployedMtaModule("foo", Arrays.asList("https://localhost:" + USED_PORT)));
        StepsUtil.setDeployedMta(context, createDeployedMta("0.9.0", deployedMtaModules));

        step.execute(context);

        verify(portAllocator, never()).allocatePort("foo");
        verify(portAllocator, never()).allocateTcpPort(eq("foo"), anyBoolean());

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(2, modules.size());
        validatePortBasedModuleParameters(modules.get(0), USED_PORT);
    }

    @Test(expected = ContentException.class)
    public void testVersionRuleWithDowngrade() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);
        StepsUtil.setDeployedMta(context, createDeployedMta("2.0.0", Collections.emptyList()));

        step.execute(context);
    }

    @Test(expected = ContentException.class)
    public void testVersionRuleWithReinstallation() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);
        context.setVariable(Constants.PARAM_VERSION_RULE, VersionRule.HIGHER.toString());
        StepsUtil.setDeployedMta(context, createDeployedMta("1.0.0", Collections.emptyList()));

        step.execute(context);
    }

    @Test
    public void testWithNonApplications() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient(true);
        when(moduleToDeployHelper.isApplication(any())).thenReturn(false);

        step.execute(context);

        verify(portAllocator, never()).allocatePort(anyString());
        verify(portAllocator, never()).allocateTcpPort(anyString(), anyBoolean());
    }

    @Test
    public void testExistingParametersAreNotOverridden() {
        prepareDescriptor("system-parameters/mtad-with-existing-parameters.yaml");
        prepareClient(true);

        step.execute(context);

        DeploymentDescriptor descriptor = StepsUtil.getCompleteDeploymentDescriptor(context);
        List<Module> modules = descriptor.getModules2();
        assertEquals(1, modules.size());
        Module foo = modules.get(0);
        assertEquals("bar", foo.getParameters()
            .get(SupportedParameters.APP_NAME));

        List<Resource> resources = descriptor.getResources2();
        assertEquals(1, resources.size());
        Resource baz = resources.get(0);
        assertEquals("qux", baz.getParameters()
            .get(SupportedParameters.SERVICE_NAME));
    }

}
