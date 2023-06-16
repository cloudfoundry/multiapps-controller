package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.CredentialsGenerator;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.util.ReadOnlyParametersChecker;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.VersionRule;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.util.AuthorizationEndpointGetter;

public abstract class CollectSystemParametersStepBaseTest extends SyncFlowableStepTest<CollectSystemParametersStep> {

    protected static final String DEFAULT_TIMESTAMP = "19700101";
    protected static final String USER = "admin";
    protected static final String ORGANIZATION_NAME = "my-org";
    protected static final String ORGANIZATION_GUID = "1247566c-7bfd-48f3-a74e-d82711dc1180";
    protected static final String SPACE_NAME = "my-space";
    protected static final String SPACE_GUID = "98f099c0-41d4-455e-affc-b072f5b2b06f";
    protected static final String AUTHORIZATION_URL = "https://localhost:30032/uaa-security";
    protected static final String CONTROLLER_URL = "https://localhost:30030";
    protected static final String MULTIAPPS_CONTROLLER_URL = "https://localhost:50010";
    protected static final String DEFAULT_DOMAIN = "localhost";
    protected static final UUID DEFAULT_DOMAIN_GUID = UUID.fromString("7b5987e9-4325-4bb6-93e2-a0b1c562e60c");
    protected static final VersionRule VERSION_RULE = VersionRule.SAME_HIGHER;

    protected static final String DEFAULT_NAMESPACE = null;
    protected static final boolean DEFAULT_APPLY_NAMESPACE = Variables.APPLY_NAMESPACE.getDefaultValue();

    @Mock
    protected CredentialsGenerator credentialsGenerator;
    @Mock
    protected ReadOnlyParametersChecker readOnlyParametersChecker;
    @Mock
    protected AuthorizationEndpointGetter authorizationEndpointGetter;
    @Mock
    protected TokenService tokenService;
    @Mock
    protected WebClientFactory webClientFactory;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        when(configuration.getControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        when(configuration.getDeployServiceUrl()).thenReturn(MULTIAPPS_CONTROLLER_URL);
        when(authorizationEndpointGetter.getAuthorizationEndpoint()).thenReturn(AUTHORIZATION_URL);

        context.setVariable(Variables.USER, USER);
        context.setVariable(Variables.ORGANIZATION_NAME, ORGANIZATION_NAME);
        context.setVariable(Variables.ORGANIZATION_GUID, ORGANIZATION_GUID);
        context.setVariable(Variables.SPACE_NAME, SPACE_NAME);
        context.setVariable(Variables.SPACE_GUID, SPACE_GUID);
        context.setVariable(Variables.TIMESTAMP, DEFAULT_TIMESTAMP);

        context.setVariable(Variables.MTA_NAMESPACE, DEFAULT_NAMESPACE);
        context.setVariable(Variables.APPLY_NAMESPACE, DEFAULT_APPLY_NAMESPACE);
        context.setVariable(Variables.VERSION_RULE, VERSION_RULE);

        step.credentialsGeneratorSupplier = () -> credentialsGenerator;
    }

    protected void prepareDescriptor(String descriptorPath) {
        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor(descriptorPath, getClass());
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    protected void prepareClient() {
        CloudDomain defaultDomain = mockDefaultDomain();

        when(client.getDefaultDomain()).thenReturn(defaultDomain);
    }

    private CloudDomain mockDefaultDomain() {
        CloudDomain domain = mock(CloudDomain.class);
        when(domain.getName()).thenReturn(DEFAULT_DOMAIN);
        when(domain.getMetadata()).thenReturn(ImmutableCloudMetadata.builder()
                                                                    .guid(DEFAULT_DOMAIN_GUID)
                                                                    .build());
        return domain;
    }

    @Override
    protected CollectSystemParametersStep createStep() {
        return new CollectSystemParametersStep() {
            @Override
            protected AuthorizationEndpointGetter getAuthorizationEndpointGetter(ProcessContext context) {
                return authorizationEndpointGetter;
            }
        };
    }

}
