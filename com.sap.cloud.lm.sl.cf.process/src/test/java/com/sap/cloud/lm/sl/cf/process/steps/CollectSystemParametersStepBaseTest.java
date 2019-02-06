package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Spy;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

public abstract class CollectSystemParametersStepBaseTest extends SyncFlowableStepTest<CollectSystemParametersStep> {

    protected static final String DEFAULT_TIMESTAMP = "19700101";
    protected static final String USER = "admin";
    protected static final String ORG = "my-org";
    protected static final String SPACE = "my-space";
    protected static final String AUTHORIZATION_URL = "https://localhost:30032/uaa-security";
    protected static final String CONTROLLER_URL = "https://localhost:30030";
    protected static final String MULTIAPPS_CONTROLLER_URL = "https://localhost:50010";
    protected static final String DEFAULT_DOMAIN = "localhost";
    protected static final UUID DEFAULT_DOMAIN_GUID = UUID.fromString("7b5987e9-4325-4bb6-93e2-a0b1c562e60c");
    protected static final String VERSION_RULE = VersionRule.SAME_HIGHER.toString();

    protected static final boolean DEFAULT_USE_NAMESPACES = false;
    protected static final boolean DEFAULT_USE_NAMESPACES_FOR_SERVICES = false;

    @Spy
    protected PortAllocator portAllocator = new PortAllocatorMock(PortValidator.MIN_PORT_VALUE);
    @Mock
    protected CredentialsGenerator credentialsGenerator;
    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;

    @Before
    public void setUp() throws MalformedURLException {
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);

        when(configuration.getControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        when(configuration.getRouterPort()).thenReturn(ApplicationConfiguration.DEFAULT_HTTPS_ROUTER_PORT);
        when(configuration.areXsPlaceholdersSupported()).thenReturn(false);
        when(configuration.getPlatformType()).thenReturn(PlatformType.XS2);

        context.setVariable(Constants.VAR_USER, USER);
        context.setVariable(Constants.VAR_ORG, ORG);
        context.setVariable(Constants.VAR_SPACE, SPACE);

        context.setVariable(Constants.PARAM_USE_NAMESPACES, DEFAULT_USE_NAMESPACES);
        context.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, DEFAULT_USE_NAMESPACES_FOR_SERVICES);
        context.setVariable(Constants.PARAM_VERSION_RULE, VERSION_RULE);

        step.credentialsGeneratorSupplier = () -> credentialsGenerator;
        step.timestampSupplier = () -> DEFAULT_TIMESTAMP;
        when(clientProvider.getPortAllocator(any(), anyString())).thenReturn(portAllocator);
    }

    protected void prepareDescriptor(String descriptorPath) {
        DeploymentDescriptor descriptor = StepsTestUtil.loadDeploymentDescriptor(descriptorPath, getClass());
        StepsUtil.setDeploymentDescriptor(context, descriptor);
    }

    protected void prepareClient(boolean portBasedRouting) {
        CloudDomain defaultDomain = mockDefaultDomain();
        CloudInfo info = mockInfo(portBasedRouting);

        when(client.getDefaultDomain()).thenReturn(defaultDomain);
        when(client.getCloudInfo()).thenReturn(info);
    }

    private CloudDomain mockDefaultDomain() {
        CloudDomain domain = mock(CloudDomain.class);
        when(domain.getName()).thenReturn(DEFAULT_DOMAIN);
        when(domain.getMeta()).thenReturn(new Meta(DEFAULT_DOMAIN_GUID, null, null));
        return domain;
    }

    private CloudInfo mockInfo(boolean portBasedRouting) {
        CloudInfo info = portBasedRouting ? mock(CloudInfoExtended.class) : mock(CloudInfo.class);
        if (info instanceof CloudInfoExtended) {
            when(((CloudInfoExtended) info).getDeployServiceUrl()).thenReturn(MULTIAPPS_CONTROLLER_URL);
            when(((CloudInfoExtended) info).isPortBasedRouting()).thenReturn(true);
        }
        when(info.getAuthorizationEndpoint()).thenReturn(AUTHORIZATION_URL);
        return info;
    }

    @Override
    protected CollectSystemParametersStep createStep() {
        return new CollectSystemParametersStep();
    }

}
