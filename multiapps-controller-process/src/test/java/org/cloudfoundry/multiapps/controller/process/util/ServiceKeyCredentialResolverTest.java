package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ServiceKeyCredentialResolverTest {

    private static final String REFERENCED_RESOURCE = "ztis-config";
    private static final String REFERENCED_SERVICE_INSTANCE = "ztis-config-instance";
    private static final String REFERENCED_KEY = "ztis-key";

    @Mock
    private CloudControllerClient client;

    private ServiceKeyCredentialResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        resolver = new ServiceKeyCredentialResolver(client, Map.of(REFERENCED_RESOURCE, REFERENCED_SERVICE_INSTANCE));
    }

    @Test
    void testResolvesServiceKeyCredentialReference() {
        Mockito.when(client.getServiceKey(REFERENCED_SERVICE_INSTANCE, REFERENCED_KEY))
               .thenReturn(buildReferencedKey(Map.of("issuer", "CN=the-issuer")));
        CloudServiceKey keyToResolve = buildKeyWithCredentials(Map.of("issuer",
                                                                      "{ds/" + REFERENCED_RESOURCE + "/service-keys." + REFERENCED_KEY
                                                                          + ".credentials.issuer}"));

        CloudServiceKey resolved = resolver.resolve(keyToResolve);

        Assertions.assertEquals("CN=the-issuer", resolved.getCredentials()
                                                         .get("issuer"));
    }

    @Test
    void testLeavesPlainCredentialsUntouched() {
        CloudServiceKey keyToResolve = buildKeyWithCredentials(Map.of("app-identifier", "caas-coll-client"));

        CloudServiceKey resolved = resolver.resolve(keyToResolve);

        Assertions.assertEquals("caas-coll-client", resolved.getCredentials()
                                                            .get("app-identifier"));
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testFailsLoudlyWhenReferencedFieldMissing() {
        Mockito.when(client.getServiceKey(REFERENCED_SERVICE_INSTANCE, REFERENCED_KEY))
               .thenReturn(buildReferencedKey(Map.of("subject", "CN=only-subject")));
        CloudServiceKey keyToResolve = buildKeyWithCredentials(Map.of("issuer",
                                                                      "{ds/" + REFERENCED_RESOURCE + "/service-keys." + REFERENCED_KEY
                                                                          + ".credentials.issuer}"));

        Assertions.assertThrows(ContentException.class, () -> resolver.resolve(keyToResolve));
    }

    private CloudServiceKey buildKeyWithCredentials(Map<String, Object> credentials) {
        return ImmutableCloudServiceKey.builder()
                                       .name("caas-v2-key")
                                       .credentials(credentials)
                                       .serviceInstance(ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name("caas-v2")
                                                                                             .build())
                                       .build();
    }

    private CloudServiceKey buildReferencedKey(Map<String, Object> credentials) {
        return ImmutableCloudServiceKey.builder()
                                       .name(REFERENCED_KEY)
                                       .credentials(credentials)
                                       .serviceInstance(ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(REFERENCED_SERVICE_INSTANCE)
                                                                                             .build())
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .build())
                                       .build();
    }

}