package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.HostValidator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class SystemParametersTest {

    @Mock
    private CredentialsGenerator credentialsGenerator;
    @Mock
    private Supplier<String> timestampSupplier;

    private static final String TIMESTAMP = "1568384751";
    private static final String PROTOCOL = "http";

    private static final String ORGANIZATION_NAME = "testOrg123";
    private static final String ORGANIZATION_GUID = "1247566c-7bfd-48f3-a74e-d82711dc1180";
    private static final String SPACE_NAME = "testSpace456";
    private static final String SPACE_GUID = "98f099c0-41d4-455e-affc-b072f5b2b06f";
    private static final String USER_NAME = "someUser123";
    private static final String DEFAULT_DOMAIN = "cfapps.domain.com";
    private static final String CONTROLLER_URL = "http://api.cf.domain.com";
    private static final String AUTHORIZATION_ENDPOINT = "uaa.cf.domain.com";
    private static final String DEPLOY_SERVICE_URL = "http://deploy-service.cfapps.domain.com";

    private static final String DESCRIPTOR_DEFINED_VALUE = "descriptorDefinedValue";

    private static final String XS_TYPE = "CF";
    private static final String NAMESPACE = "productive";

    @BeforeEach
    void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        Mockito.when(timestampSupplier.get())
               .thenReturn(TIMESTAMP);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testGeneralParameters(boolean reserveTemporaryRoutes) throws Exception {
        SystemParameters testedClass = createSystemParameters(reserveTemporaryRoutes, false);
        DeploymentDescriptor v2Descriptor = DeploymentDescriptor.createV3();
        testedClass.injectInto(v2Descriptor);
        verifyGeneralParameters(v2Descriptor.getParameters(), reserveTemporaryRoutes);
    }

    @Test
    void testDescriptorOverridesDefaults() throws Exception {
        SystemParameters testedClass = createSystemParameters(false, false);

        List<String> descriptorParameterFields = List.of(SupportedParameters.ORGANIZATION_NAME, SupportedParameters.SPACE_NAME,
                                                         SupportedParameters.USER, SupportedParameters.DEFAULT_DOMAIN,
                                                         SupportedParameters.CONTROLLER_URL, SupportedParameters.AUTHORIZATION_URL,
                                                         SupportedParameters.DEPLOY_SERVICE_URL);

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setParameters(createParametersMap(descriptorParameterFields));
        testedClass.injectInto(descriptor);

        assertCustomValueMap(descriptorParameterFields, descriptor.getParameters());
    }

    static Stream<Arguments> testModuleParameters() {
        return Stream.of(
                         // [1] Do not reserve temporary routes and does not apply namespace
                         Arguments.of(false, false),
                         // [2] Reserve temporary routes and does not apply namespace
                         Arguments.of(true, false),
                         // [3] Do not reserve temporary routes but use namespace
                         Arguments.of(false, true),
                         // [4] Reserve temporary routes and apply namespace
                         Arguments.of(true, true));
    }

    @ParameterizedTest
    @MethodSource
    void testModuleParameters(boolean reserveTemporaryRoutes, boolean applyNamespace) throws Exception {
        SystemParameters testedClass = createSystemParameters(reserveTemporaryRoutes, applyNamespace);
        Module moduleOne = Module.createV3()
                                 .setName("first");
        Module moduleTwo = Module.createV3()
                                 .setName("second");

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setModules(List.of(moduleOne, moduleTwo));
        testedClass.injectInto(descriptor);

        for (Module module : descriptor.getModules()) {
            verifyModuleParameters(module.getName(), module.getParameters(), reserveTemporaryRoutes, applyNamespace);
        }
    }

    @Test
    void testModuleParametersOverrideSystemParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false, false);
        List<String> fields = List.of(SupportedParameters.PROTOCOL, SupportedParameters.TIMESTAMP, SupportedParameters.INSTANCES,
                                      SupportedParameters.APP_NAME, SupportedParameters.IDLE_DOMAIN, SupportedParameters.DOMAIN);
        Module moduleWithParameters = Module.createV3()
                                            .setName("first")
                                            .setParameters(createParametersMap(fields));
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setModules(List.of(moduleWithParameters));

        testedClass.injectInto(descriptor);
        assertCustomValueMap(fields, descriptor.getModules()
                                               .get(0)
                                               .getParameters());
    }

    @Test
    void testResourceParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false, false);
        Resource resourceOne = Resource.createV3()
                                       .setName("first");
        Resource resourceTwo = Resource.createV3()
                                       .setName("second");

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setResources(List.of(resourceOne, resourceTwo));
        testedClass.injectInto(descriptor);

        for (Resource resource : descriptor.getResources()) {
            verifyResourceParameters(resource.getName(), resource.getParameters());
        }
    }

    private void verifyResourceParameters(String name, Map<String, Object> parameters) {
        assertEquals("${default-service-name}", parameters.get(SupportedParameters.SERVICE_NAME));
        assertEquals(name, parameters.get(SupportedParameters.DEFAULT_SERVICE_NAME));
        assertEquals(NameUtil.computeValidContainerName(ORGANIZATION_NAME, SPACE_NAME, name),
                     parameters.get(SupportedParameters.DEFAULT_CONTAINER_NAME));
        assertEquals(NameUtil.computeValidXsAppName(name), parameters.get(SupportedParameters.DEFAULT_XS_APP_NAME));
    }

    @Test
    void testResourceParametersOverrideSystemParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false, false);
        List<String> fields = List.of(SupportedParameters.SERVICE_NAME, SupportedParameters.DEFAULT_CONTAINER_NAME,
                                      SupportedParameters.DEFAULT_XS_APP_NAME);
        Resource resourceWithParameters = Resource.createV3()
                                                  .setName("first")
                                                  .setParameters(createParametersMap(fields));
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setResources(List.of(resourceWithParameters));

        testedClass.injectInto(descriptor);
        assertCustomValueMap(fields, descriptor.getResources()
                                               .get(0)
                                               .getParameters());
    }

    private Map<String, Object> createParametersMap(List<String> fields) {
        return fields.stream()
                     .collect(Collectors.toMap(field -> field, f -> DESCRIPTOR_DEFINED_VALUE));
    }

    private void assertCustomValueMap(List<String> fields, Map<String, Object> parametersMap) {
        for (String field : fields) {
            assertEquals(DESCRIPTOR_DEFINED_VALUE, parametersMap.get(field));
        }
    }

    private void verifyModuleParameters(String moduleName, Map<String, Object> moduleParameters, boolean reserveTemporaryRoutes,
                                        boolean applyNamespace) {
        assertFalse(moduleParameters.containsKey(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals("${default-domain}", moduleParameters.get(SupportedParameters.DOMAIN));

        if (reserveTemporaryRoutes) {
            assertFalse(moduleParameters.containsKey(SupportedParameters.DEFAULT_IDLE_DOMAIN));
            assertEquals("${default-idle-domain}", moduleParameters.get(SupportedParameters.IDLE_DOMAIN));
        } else {
            assertFalse(moduleParameters.containsKey(SupportedParameters.IDLE_DOMAIN));
        }

        assertEquals(applyNamespace, ((String) moduleParameters.get(SupportedParameters.DEFAULT_HOST)).startsWith(NAMESPACE));
        assertEquals(moduleName, moduleParameters.get(SupportedParameters.DEFAULT_APP_NAME));
        assertEquals("${default-app-name}", moduleParameters.get(SupportedParameters.APP_NAME));
        assertEquals(1, moduleParameters.get(SupportedParameters.DEFAULT_INSTANCES));
        assertEquals("${default-instances}", moduleParameters.get(SupportedParameters.INSTANCES));
        assertEquals(TIMESTAMP, moduleParameters.get(SupportedParameters.TIMESTAMP));

        assertEquals(PROTOCOL, moduleParameters.get(SupportedParameters.PROTOCOL));
    }

    private void verifyGeneralParameters(Map<String, Object> descriptorParameters, Boolean reserveTemporaryRoutes) {
        assertEquals(ORGANIZATION_NAME, descriptorParameters.get(SupportedParameters.ORGANIZATION_NAME));
        assertEquals(ORGANIZATION_GUID, descriptorParameters.get(SupportedParameters.ORGANIZATION_GUID));
        assertEquals(SPACE_NAME, descriptorParameters.get(SupportedParameters.SPACE_NAME));
        assertEquals(SPACE_GUID, descriptorParameters.get(SupportedParameters.SPACE_GUID));
        assertEquals(USER_NAME, descriptorParameters.get(SupportedParameters.USER));
        assertEquals(DEFAULT_DOMAIN, descriptorParameters.get(SupportedParameters.DEFAULT_DOMAIN));

        if (reserveTemporaryRoutes) {
            assertEquals(DEFAULT_DOMAIN, descriptorParameters.get(SupportedParameters.DEFAULT_IDLE_DOMAIN));
        } else {
            assertFalse(descriptorParameters.containsKey(SupportedParameters.DEFAULT_IDLE_DOMAIN));
        }

        assertEquals(CONTROLLER_URL, descriptorParameters.get(SupportedParameters.CONTROLLER_URL));
        assertEquals(XS_TYPE, descriptorParameters.get(SupportedParameters.XS_TYPE));
        assertEquals(AUTHORIZATION_ENDPOINT, descriptorParameters.get(SupportedParameters.AUTHORIZATION_URL));
        assertEquals(DEPLOY_SERVICE_URL, descriptorParameters.get(SupportedParameters.DEPLOY_SERVICE_URL));
    }

    private SystemParameters createSystemParameters(boolean reserveTemporaryRoutes, boolean applyNamespace) throws MalformedURLException {
        return new SystemParameters.Builder().authorizationEndpoint(AUTHORIZATION_ENDPOINT)
                                             .controllerUrl(new URL(CONTROLLER_URL))
                                             .credentialsGenerator(credentialsGenerator)
                                             .defaultDomain(DEFAULT_DOMAIN)
                                             .deployServiceUrl(DEPLOY_SERVICE_URL)
                                             .organizationName(ORGANIZATION_NAME)
                                             .organizationGuid(ORGANIZATION_GUID)
                                             .spaceName(SPACE_NAME)
                                             .spaceGuid(SPACE_GUID)
                                             .timestampSupplier(timestampSupplier)
                                             .reserveTemporaryRoutes(reserveTemporaryRoutes)
                                             .user(USER_NAME)
                                             .hostValidator(new HostValidator(applyNamespace ? NAMESPACE : null, applyNamespace))
                                             .build();
    }

}
