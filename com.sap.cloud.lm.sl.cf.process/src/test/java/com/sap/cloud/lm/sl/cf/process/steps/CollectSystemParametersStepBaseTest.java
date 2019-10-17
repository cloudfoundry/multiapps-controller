package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import com.sap.cloud.lm.sl.cf.process.util.ReadOnlyParametersChecker;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.Before;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

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

    @Mock
    protected CredentialsGenerator credentialsGenerator;

    @Mock
    protected ReadOnlyParametersChecker readOnlyParametersChecker;

    @Before
    public void setUp() throws MalformedURLException {
        when(configuration.getControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        when(configuration.getDeployServiceUrl()).thenReturn(MULTIAPPS_CONTROLLER_URL);

        context.setVariable(Constants.VAR_USER, USER);
        context.setVariable(Constants.VAR_ORG, ORG);
        context.setVariable(Constants.VAR_SPACE, SPACE);

        context.setVariable(Constants.PARAM_USE_NAMESPACES, DEFAULT_USE_NAMESPACES);
        context.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, DEFAULT_USE_NAMESPACES_FOR_SERVICES);
        context.setVariable(Constants.PARAM_VERSION_RULE, VERSION_RULE);

        step.credentialsGeneratorSupplier = () -> credentialsGenerator;
        step.timestampSupplier = () -> DEFAULT_TIMESTAMP;
    }

    protected void prepareDescriptor(String descriptorPath) {
        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor(descriptorPath, getClass());
        StepsUtil.setDeploymentDescriptor(context, descriptor);
    }

    protected void prepareClient() {
        CloudDomain defaultDomain = mockDefaultDomain();
        CloudInfo info = mockInfo();

        when(client.getDefaultDomain()).thenReturn(defaultDomain);
        when(client.getCloudInfo()).thenReturn(info);
    }

    private CloudDomain mockDefaultDomain() {
        CloudDomain domain = mock(CloudDomain.class);
        when(domain.getName()).thenReturn(DEFAULT_DOMAIN);
        when(domain.getMetadata()).thenReturn(ImmutableCloudMetadata.builder()
                                                                    .guid(DEFAULT_DOMAIN_GUID)
                                                                    .build());
        return domain;
    }

    private CloudInfo mockInfo() {
        CloudInfo info = mock(CloudInfo.class);
        when(info.getAuthorizationEndpoint()).thenReturn(AUTHORIZATION_URL);
        return info;
    }

    @Override
    protected CollectSystemParametersStep createStep() {
        return new CollectSystemParametersStep();
    }

}
