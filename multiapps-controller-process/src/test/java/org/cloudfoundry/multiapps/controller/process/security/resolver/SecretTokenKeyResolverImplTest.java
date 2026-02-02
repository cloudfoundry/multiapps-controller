package org.cloudfoundry.multiapps.controller.process.security.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecretTokenKeyResolverImplTest {

    private CloudControllerClientProvider cloudControllerClientProvider;

    private CloudControllerClient cloudControllerClient;

    private DelegateExecution execution;

    private CloudServiceInstance cloudServiceInstance;

    private SecretTokenKeyResolver secretTokenKeyResolver;

    @BeforeEach
    void setUp() {
        cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        cloudControllerClient = Mockito.mock(CloudControllerClient.class);
        execution = Mockito.mock(DelegateExecution.class);
        cloudServiceInstance = Mockito.mock(CloudServiceInstance.class);

        when(execution.getVariable(Variables.SPACE_GUID.getName())).thenReturn("space-guid");
        when(execution.getVariable(Variables.CORRELATION_ID.getName())).thenReturn("corr-id");
        when(execution.getVariable(Variables.USER.getName())).thenReturn("user");
        when(execution.getVariable(Variables.USER_GUID.getName())).thenReturn("user-guid");

        when(execution.getVariable(Variables.MTA_ID.getName())).thenReturn("my-mta");
        when(execution.getVariable(Variables.MTA_NAMESPACE.getName())).thenReturn("ns");

        when(cloudControllerClientProvider.getControllerClient(anyString(), anyString(), anyString())).thenReturn(
            cloudControllerClient);

        secretTokenKeyResolver = new SecretTokenKeyResolverImpl(cloudControllerClientProvider);
    }

    @Test
    void testResolveSuccessWithNamespace() {
        String expectedUps = "__mta-secure-my-mtans";

        UUID upsGuid = UUID.randomUUID();
        when(cloudServiceInstance.getGuid()).thenReturn(upsGuid);
        when(cloudControllerClient.getServiceInstance(expectedUps)).thenReturn(cloudServiceInstance);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("encryptionKey", "abcdefghijklmnopqrstuvwxyz123456");
        when(cloudControllerClient.getUserProvidedServiceInstanceParameters(upsGuid)).thenReturn(credentials);

        String encryptionKey = secretTokenKeyResolver.resolve(execution);

        assertEquals("abcdefghijklmnopqrstuvwxyz123456", encryptionKey);
        verify(cloudControllerClient, times(1)).getServiceInstance(expectedUps);
        verify(cloudControllerClient, times(1)).getUserProvidedServiceInstanceParameters(upsGuid);
    }

    @Test
    void testResolveSuccessWithoutNamespace() {
        when(execution.getVariable(Variables.MTA_NAMESPACE.getName())).thenReturn(null);

        String expectedUps = "__mta-secure-my-mta";

        UUID upsGuid = UUID.randomUUID();
        when(cloudServiceInstance.getGuid()).thenReturn(upsGuid);
        when(cloudControllerClient.getServiceInstance(expectedUps)).thenReturn(cloudServiceInstance);

        Map<String, Object> creds = new HashMap<String, Object>();
        creds.put("encryptionKey", "abcdefghijklmnopqrstuvwxyz123456");
        when(cloudControllerClient.getUserProvidedServiceInstanceParameters(upsGuid)).thenReturn(creds);

        String encryptionKey = secretTokenKeyResolver.resolve(execution);

        assertEquals("abcdefghijklmnopqrstuvwxyz123456", encryptionKey);
    }

    @Test
    void testResolveWhenServiceInstanceMissing() {
        when(cloudControllerClient.getServiceInstance("__mta-secure-my-mtans")).thenReturn(null);

        Exception exception = assertThrows(SLException.class, () -> secretTokenKeyResolver.resolve(execution));
        assertTrue(exception.getMessage()
                            .contains("Could not retrieve service instance"));
        verify(cloudControllerClient, times(1)).getServiceInstance("__mta-secure-my-mtans");
        verify(cloudControllerClient, never()).getUserProvidedServiceInstanceParameters(any(UUID.class));
    }

    @Test
    void testResolveWhenCredentialsMissing() {
        String expectedUps = "__mta-secure-my-mtans";

        UUID upsGuid = UUID.randomUUID();
        when(cloudServiceInstance.getGuid()).thenReturn(upsGuid);
        when(cloudControllerClient.getServiceInstance(expectedUps)).thenReturn(cloudServiceInstance);

        when(cloudControllerClient.getUserProvidedServiceInstanceParameters(upsGuid))
            .thenReturn(new HashMap<>());

        Exception exception = assertThrows(SLException.class, () -> secretTokenKeyResolver.resolve(execution));
        assertTrue(exception.getMessage()
                            .contains("Could not retrieve credentials"));
    }

}
