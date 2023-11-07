package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.helpers.SystemParameters;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.cloudfoundry.multiapps.mta.model.VersionRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

class CollectSystemParametersStepTest extends CollectSystemParametersStepBaseTest {

    private static final String DEFAULT_PROTOCOL = "https";
    private static final String DEFAULT_DOMAIN_PLACEHOLDER = "apps.internal";
    private static final String MTA_VERSION = "1.0.0";
    private static final String MTA_ID = "system-parameters-test";

    @Test
    void testGeneralParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();

        step.execute(execution);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        Map<String, Object> generalParameters = descriptor.getParameters();
        assertEquals(USER, generalParameters.get(SupportedParameters.USER));
        assertEquals(ORGANIZATION_NAME, generalParameters.get(SupportedParameters.ORGANIZATION_NAME));
        assertEquals(ORGANIZATION_GUID, generalParameters.get(SupportedParameters.ORGANIZATION_GUID));
        assertEquals(SPACE_NAME, generalParameters.get(SupportedParameters.SPACE_NAME));
        assertEquals(SPACE_GUID, generalParameters.get(SupportedParameters.SPACE_GUID));
        assertEquals(DEFAULT_DOMAIN, generalParameters.get(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals(AUTHORIZATION_URL, generalParameters.get(SupportedParameters.XS_AUTHORIZATION_ENDPOINT));
        assertEquals(AUTHORIZATION_URL, generalParameters.get(SupportedParameters.AUTHORIZATION_URL));
        assertEquals(CONTROLLER_URL, generalParameters.get(SupportedParameters.XS_TARGET_API_URL));
        assertEquals(CONTROLLER_URL, generalParameters.get(SupportedParameters.CONTROLLER_URL));
        assertEquals(MULTIAPPS_CONTROLLER_URL, generalParameters.get(SupportedParameters.DEPLOY_SERVICE_URL));
        assertEquals(DEFAULT_TIMESTAMP, generalParameters.get(SupportedParameters.TIMESTAMP));
        assertEquals(MTA_VERSION, generalParameters.get(SupportedParameters.MTA_VERSION));
        assertEquals(MTA_ID, generalParameters.get(SupportedParameters.MTA_ID));
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
    void testWithHostBasedRouting() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();

        step.execute(execution);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        Mockito.verify(readOnlyParametersChecker)
               .check(any());
        List<Module> modules = descriptor.getModules();
        assertEquals(2, modules.size());
        for (Module module : modules) {
            validateHostBasedModuleParameters(module);
        }
    }

    @Test
    void testWithRoutePath() {
        prepareDescriptor("system-parameters/mtad-with-route-path.yaml");
        prepareClient();

        step.execute(execution);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
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
        return String.format("%s-%s-%s", ORGANIZATION_NAME, SPACE_NAME, module.getName());
    }

    private String computeExpectedDefaultUri(String path) {
        return SystemParameters.DEFAULT_HOST_BASED_URI + (path == null ? "" : SystemParameters.ROUTE_PATH_PLACEHOLDER);
    }

    @Test
    void testGeneralModuleAndResourceParameters() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        when(credentialsGenerator.next(anyInt())).thenReturn("abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "vwx");

        step.execute(execution);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
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

    @Test
    void testVersionRuleWithDowngrade() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        context.setVariable(Variables.DEPLOYED_MTA, createDeployedMta("2.0.0", Collections.emptyList()));
        assertThrows(ContentException.class, () -> step.execute(execution));
    }

    @Test
    void testVersionRuleWithReinstallation() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        context.setVariable(Variables.VERSION_RULE, VersionRule.HIGHER);
        context.setVariable(Variables.DEPLOYED_MTA, createDeployedMta("1.0.0", Collections.emptyList()));
        assertThrows(ContentException.class, () -> step.execute(execution));
    }

    @Test
    void testExistingParametersAreNotOverridden() {
        prepareDescriptor("system-parameters/mtad-with-existing-parameters.yaml");
        prepareClient();

        step.execute(execution);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        List<Module> modules = descriptor.getModules();
        assertEquals(1, modules.size());
        Module foo = modules.get(0);
        assertEquals("bar", NameUtil.getApplicationName(foo));

        List<Resource> resources = descriptor.getResources();
        assertEquals(1, resources.size());
        Resource baz = resources.get(0);
        assertEquals("qux", NameUtil.getServiceName(baz));
    }

    @Test
    void testDefaultDomainNotFoundException() {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        when(client.getDefaultDomain()).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));

        step.execute(execution);

        assertTrue(context.getVariable(Variables.MISSING_DEFAULT_DOMAIN));
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        assertEquals(DEFAULT_DOMAIN_PLACEHOLDER, descriptor.getParameters()
                                                           .get(SupportedParameters.DEFAULT_DOMAIN));
    }

    @Test
    void testDefaultDomainException() {
        when(client.getDefaultDomain()).thenThrow(new CloudOperationException(HttpStatus.GATEWAY_TIMEOUT));
        assertThrows(SLException.class, () -> step.execute(execution));
    }

    private static Stream<Arguments> testVersionRuleWithBuildSuffix() {
        //the build suffix does not affect the version (2 versions are considered identical when performing semver
        // comparison, even if one has a build suffix)
        return Stream.of(
                Arguments.of("1.0.0-pre.rel+build123", "1.0.0-pre.rel+build123", VersionRule.HIGHER, true), //reject
                Arguments.of("1.0.0-pre.rel+build123", "1.0.0", VersionRule.HIGHER, false), //allow
                Arguments.of("1.0.0", "1.0.0-pre.rel", VersionRule.SAME_HIGHER, true), //reject
                Arguments.of("1.0.0-pre.rel", "1.0.0-pre.rel+build123", VersionRule.SAME_HIGHER, false), //allow
                Arguments.of("1.0.0", "1.0.0-pre.rel+build123", VersionRule.ALL, false) //allow
        );
    }

    @ParameterizedTest
    @MethodSource
    void testVersionRuleWithBuildSuffix(String oldMtaVersion, String newMtaVersion, VersionRule versionRule,
                                        boolean shouldThrow) {
        prepareDescriptor("system-parameters/mtad.yaml");
        prepareClient();
        context.setVariable(Variables.VERSION_RULE, versionRule);
        context.setVariable(Variables.DEPLOYED_MTA, createDeployedMta(oldMtaVersion, Collections.emptyList()));
        var descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor.setVersion(newMtaVersion));

        if (shouldThrow) {
            assertThrows(ContentException.class, () -> step.execute(execution));
        } else {
            assertDoesNotThrow(() -> step.execute(execution));
        }
    }

}
