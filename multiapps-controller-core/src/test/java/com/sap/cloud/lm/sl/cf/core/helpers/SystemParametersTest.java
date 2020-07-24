package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;

public class SystemParametersTest {

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

    @BeforeEach
    public void initialize() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(timestampSupplier.get())
               .thenReturn(TIMESTAMP);
    }

    @Test
    public void testGeneralParametersWithoutReserveTemporaryRoutes() throws Exception {
        testGeneralParameters(false);
    }

    @Test
    public void testGeneralParametersWithReserveTemporaryRoutes() throws Exception {
        testGeneralParameters(true);
    }

    private void testGeneralParameters(Boolean reserveTemporaryRoutes) throws Exception {
        SystemParameters testedClass = createSystemParameters(reserveTemporaryRoutes);
        DeploymentDescriptor v2Descriptor = DeploymentDescriptor.createV3();
        testedClass.injectInto(v2Descriptor);
        verifyGeneralParameters(v2Descriptor.getParameters(), reserveTemporaryRoutes);
    }

    @Test
    public void testDescriptorOverridesDefaults() throws Exception {
        SystemParameters testedClass = createSystemParameters(false);

        List<String> descriptorParameterFields = Arrays.asList(SupportedParameters.ORGANIZATION_NAME, SupportedParameters.SPACE_NAME,
                                                               SupportedParameters.USER, SupportedParameters.DEFAULT_DOMAIN,
                                                               SupportedParameters.CONTROLLER_URL, SupportedParameters.AUTHORIZATION_URL,
                                                               SupportedParameters.DEPLOY_SERVICE_URL);

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setParameters(createParametersMap(descriptorParameterFields));
        testedClass.injectInto(descriptor);

        assertCustomValueMap(descriptorParameterFields, descriptor.getParameters());
    }

    @Test
    public void testModuleParametersWithoutReserveTemporaryRoutes() throws Exception {
        testModuleParameters(false);
    }

    @Test
    public void testModuleParametersWithReserveTemporaryRoutes() throws Exception {
        testModuleParameters(true);
    }

    private void testModuleParameters(boolean reserveTemporaryRoutes) throws Exception {
        SystemParameters testedClass = createSystemParameters(reserveTemporaryRoutes);
        Module moduleOne = Module.createV3()
                                 .setName("first");
        Module moduleTwo = Module.createV3()
                                 .setName("second");

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setModules(Arrays.asList(moduleOne, moduleTwo));
        testedClass.injectInto(descriptor);

        for (Module module : descriptor.getModules()) {
            verifyModuleParameters(module.getName(), module.getParameters(), reserveTemporaryRoutes);
        }
    }

    @Test
    public void testModuleParametersOverrideSystemParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false);
        List<String> fields = Arrays.asList(SupportedParameters.PROTOCOL, SupportedParameters.TIMESTAMP, SupportedParameters.INSTANCES,
                                            SupportedParameters.APP_NAME, SupportedParameters.IDLE_DOMAIN, SupportedParameters.DOMAIN);
        Module moduleWithParameters = Module.createV3()
                                            .setName("first")
                                            .setParameters(createParametersMap(fields));
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setModules(Collections.singletonList(moduleWithParameters));

        testedClass.injectInto(descriptor);
        assertCustomValueMap(fields, descriptor.getModules()
                                               .get(0)
                                               .getParameters());
    }

    @Test
    public void testResourceParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false);
        Resource resourceOne = Resource.createV3()
                                       .setName("first");
        Resource resourceTwo = Resource.createV3()
                                       .setName("second");

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setResources(Arrays.asList(resourceOne, resourceTwo));
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
    public void testResourceParametersOverrideSystemParameters() throws Exception {
        SystemParameters testedClass = createSystemParameters(false);
        List<String> fields = Arrays.asList(SupportedParameters.SERVICE_NAME, SupportedParameters.DEFAULT_CONTAINER_NAME,
                                            SupportedParameters.DEFAULT_XS_APP_NAME);
        Resource resourceWithParameters = Resource.createV3()
                                                  .setName("first")
                                                  .setParameters(createParametersMap(fields));
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setResources(Collections.singletonList(resourceWithParameters));

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

    private void verifyModuleParameters(String moduleName, Map<String, Object> moduleParameters, boolean reserveTemporaryRoutes) {
        assertFalse(moduleParameters.containsKey(SupportedParameters.DEFAULT_DOMAIN));
        assertEquals("${default-domain}", moduleParameters.get(SupportedParameters.DOMAIN));

        if (reserveTemporaryRoutes) {
            assertFalse(moduleParameters.containsKey(SupportedParameters.DEFAULT_IDLE_DOMAIN));
            assertEquals("${default-idle-domain}", moduleParameters.get(SupportedParameters.IDLE_DOMAIN));
        } else {
            assertFalse(moduleParameters.containsKey(SupportedParameters.IDLE_DOMAIN));
        }

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

    public SystemParameters createSystemParameters(boolean reserveTemporaryRoutes) throws MalformedURLException {
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
                                             .build();
    }

}
