package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.AwsS3ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.AzureObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.GcpObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.JCloudsObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.AwsTransientErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.AzureTransientErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.GcpTransientErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.JCloudsTransientErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.factory.ObjectStoreSelectorFactory;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ImmutableObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfoCreator;
import org.jclouds.blobstore.BlobStoreContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStoreSelectorFactoryTest {

    private static final String SERVICE_NAME = "deploy-service-os";
    private static final String ACCESS_KEY_ID_VALUE = "access_key_id_value";
    private static final String SECRET_ACCESS_KEY_VALUE = "secret_access_key_value";
    private static final String BUCKET_VALUE = "bucket_value";

    private static final ThreadLocal<List<ObjectStoreServiceInfo>> providersOverride = new ThreadLocal<>();

    @Mock
    private EnvironmentServicesFinder environmentServicesFinder;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private JCloudsObjectStoreFileStorage jCloudsObjectStoreFileStorage;
    @Mock
    private GcpObjectStoreFileStorage gcpObjectStoreFileStorage;
    @Mock
    private AwsS3ObjectStoreFileStorage awsS3ObjectStoreFileStorage;
    @Mock
    private AzureObjectStoreFileStorage azureObjectStoreFileStorage;

    private ObjectStoreServiceInfo capturedGcpServiceInfo;
    private ObjectStoreServiceInfo capturedAzureServiceInfo;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(applicationConfiguration.getObjectStoreRegions()).thenReturn(Set.of());
    }

    @Test
    void testObjectStoreCreationWithoutServiceInstance() {
        ObjectStoreSelectorFactory selector = createSelector();

        assertNull(selector.fileStorage());
        assertNull(selector.classifier());
    }

    @Test
    void testObjectStoreCreationWithValidServiceInstance() {
        mockCfService();

        ObjectStoreSelectorFactory selector = createSelector();

        assertNotNull(selector.fileStorage());
        assertNotNull(selector.classifier());
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAws() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);

        ObjectStoreSelectorFactory selector = createSelector();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, selector.fileStorage());
        assertInstanceOf(AwsTransientErrorClassifier.class, selector.classifier());
        verify(awsS3ObjectStoreFileStorage)
               .testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAzure() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AZURE);

        ObjectStoreSelectorFactory selector = createSelector();

        assertInstanceOf(AzureObjectStoreFileStorage.class, selector.fileStorage());
        assertInstanceOf(AzureTransientErrorClassifier.class, selector.classifier());
        verify(azureObjectStoreFileStorage)
               .testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForGcp() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);

        ObjectStoreSelectorFactory selector = createSelector();

        assertInstanceOf(GcpObjectStoreFileStorage.class, selector.fileStorage());
        assertInstanceOf(GcpTransientErrorClassifier.class, selector.classifier());
        verify(gcpObjectStoreFileStorage)
               .testConnection();
    }

    static Stream<Arguments> testObjectStoreCreationFallsBackToFirstReachableProviderWhenEnvIsInvalid() {
        return Stream.of(
        // @formatter:off
            // (0) Unknown provider name:
            Arguments.of("WRONG_PROVIDER"),
            // (1) Null env value:
            Arguments.of((String) null),
            // (2) Empty env value:
            Arguments.of("")
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testObjectStoreCreationFallsBackToFirstReachableProviderWhenEnvIsInvalid(String envValue) {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(envValue);

        ObjectStoreSelectorFactory selector = createSelector();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, selector.fileStorage());
        assertInstanceOf(AwsTransientErrorClassifier.class, selector.classifier());
    }

    @Test
    void testObjectStoreCreationFallsBackToNextProviderWhenFirstFails() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(null);
        doThrow(new IllegalStateException("AWS connection failed")).when(awsS3ObjectStoreFileStorage)
                                                                   .testConnection();

        ObjectStoreSelectorFactory selector = createSelector();

        assertInstanceOf(JCloudsObjectStoreFileStorage.class, selector.fileStorage());
        assertInstanceOf(JCloudsTransientErrorClassifier.class, selector.classifier());
        verify(jCloudsObjectStoreFileStorage)
               .testConnection();
    }

    @Test
    void testGcpFactoryReceivesPopulatedCredentialsWhenEnvIsGcp() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);

        createSelector();

        assertNotNull(capturedGcpServiceInfo);
        assertEquals(Constants.GOOGLE_CLOUD_STORAGE, capturedGcpServiceInfo.getProvider());
        assertNotNull(capturedGcpServiceInfo.getCredentials(),
                      "GCP factory must receive an ObjectStoreServiceInfo carrying the bound credentials map");
        assertEquals(ACCESS_KEY_ID_VALUE, capturedGcpServiceInfo.getCredentials()
                                                                .get(Constants.ACCESS_KEY_ID));
        assertEquals(BUCKET_VALUE, capturedGcpServiceInfo.getCredentials()
                                                         .get(Constants.BUCKET));
    }

    @Test
    void testAzureFactoryReceivesPopulatedCredentialsWhenEnvIsAzure() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AZURE);

        createSelector();

        assertNotNull(capturedAzureServiceInfo);
        assertEquals(Constants.AZUREBLOB, capturedAzureServiceInfo.getProvider());
        assertNotNull(capturedAzureServiceInfo.getCredentials(),
                      "Azure factory must receive an ObjectStoreServiceInfo carrying the bound credentials map");
        assertEquals(SECRET_ACCESS_KEY_VALUE, capturedAzureServiceInfo.getCredentials()
                                                                      .get(Constants.SECRET_ACCESS_KEY));
    }

    @Test
    void testGcpEnvWithNoMatchingProviderInfoFallsThroughToNoValidStore() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);
        providersOverride.set(List.of(buildServiceInfo(Constants.AWS_S_3)));
        try {
            Exception exception = assertThrows(IllegalStateException.class, this::createSelector);
            assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
            assertNull(capturedGcpServiceInfo, "GCP factory must not be invoked when no matching provider info is bound");
        } finally {
            providersOverride.remove();
        }
    }

    @Test
    void testObjectStoreCreationWhenEnvProviderFailsToConnect() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);
        doThrow(new IllegalStateException("Cannot create object store")).when(awsS3ObjectStoreFileStorage)
                                                                        .testConnection();

        var exception = assertThrows(IllegalStateException.class, this::createSelector);
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    @Test
    void testObjectStoreCreationWhenEnvProviderNotFoundInAvailableProviders() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);
        providersOverride.set(List.of(buildServiceInfo(Constants.AWS_S_3)));
        try {
            var exception = assertThrows(IllegalStateException.class, this::createSelector);
            assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
        } finally {
            providersOverride.remove();
        }
    }

    @Test
    void testObjectStoreCreationWhenAllProvidersFail() {
        mockCfService();
        doThrow(new IllegalStateException("Cannot create object store")).when(awsS3ObjectStoreFileStorage)
                                                                        .testConnection();
        doThrow(new IllegalStateException("Cannot create object store")).when(jCloudsObjectStoreFileStorage)
                                                                        .testConnection();
        doThrow(new IllegalStateException("Cannot create object store")).when(gcpObjectStoreFileStorage)
                                                                        .testConnection();
        doThrow(new IllegalStateException("Cannot create object store")).when(azureObjectStoreFileStorage)
                                                                        .testConnection();

        var exception = assertThrows(IllegalStateException.class, this::createSelector);
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    private ObjectStoreSelectorFactory createSelector() {
        return new ObjectStoreSelectorFactoryMock(SERVICE_NAME, environmentServicesFinder, applicationConfiguration);
    }

    private void mockCfService() {
        CfService cfService = mock(CfService.class);
        CfCredentials cfCredentials = mock(CfCredentials.class);
        when(cfCredentials.getMap()).thenReturn(buildCredentials());
        when(cfService.getCredentials()).thenReturn(cfCredentials);
        when(environmentServicesFinder.findService(SERVICE_NAME)).thenReturn(cfService);
    }

    private static Map<String, Object> buildCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.ACCESS_KEY_ID, ACCESS_KEY_ID_VALUE);
        credentials.put(Constants.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY_VALUE);
        credentials.put(Constants.BUCKET, BUCKET_VALUE);
        return credentials;
    }

    private ObjectStoreServiceInfo buildServiceInfo(String provider) {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(provider)
                                              .credentials(buildCredentials())
                                              .build();
    }

    private class ObjectStoreSelectorFactoryMock extends ObjectStoreSelectorFactory {

        ObjectStoreSelectorFactoryMock(String serviceName, EnvironmentServicesFinder environmentServicesFinder,
                                       ApplicationConfiguration applicationConfiguration) {
            super(serviceName, environmentServicesFinder, applicationConfiguration);
        }

        @Override
        public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
            List<ObjectStoreServiceInfo> override = providersOverride.get();
            if (override != null) {
                return override;
            }
            CfService service = environmentServicesFinder.findService(SERVICE_NAME);
            if (service != null) {
                return new ObjectStoreServiceInfoCreator().getAllProvidersServiceInfo(service.getCredentials()
                                                                                             .getMap());
            }
            return List.of();
        }

        @Override
        protected JCloudsObjectStoreFileStorage createFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo, BlobStoreContext context) {
            return ObjectStoreSelectorFactoryTest.this.jCloudsObjectStoreFileStorage;
        }

        @Override
        protected GcpObjectStoreFileStorage createGcpFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            ObjectStoreSelectorFactoryTest.this.capturedGcpServiceInfo = objectStoreServiceInfo;
            return ObjectStoreSelectorFactoryTest.this.gcpObjectStoreFileStorage;
        }

        @Override
        protected AzureObjectStoreFileStorage createAzureFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            ObjectStoreSelectorFactoryTest.this.capturedAzureServiceInfo = objectStoreServiceInfo;
            return ObjectStoreSelectorFactoryTest.this.azureObjectStoreFileStorage;
        }

        @Override
        protected AwsS3ObjectStoreFileStorage createAwsS3FileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            return ObjectStoreSelectorFactoryTest.this.awsS3ObjectStoreFileStorage;
        }

    }

}
