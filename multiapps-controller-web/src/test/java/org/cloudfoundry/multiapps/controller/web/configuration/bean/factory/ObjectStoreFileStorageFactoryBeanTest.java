package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.AzureObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.GcpObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.JCloudsObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ImmutableObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfoCreator;
import org.jclouds.blobstore.BlobStoreContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStoreFileStorageFactoryBeanTest {

    private static final String ACCESS_KEY_ID_VALUE = "access_key_id_value";
    private static final String SECRET_ACCESS_KEY_VALUE = "secret_access_key_value";
    private static final String BUCKET_VALUE = "bucket_value";

    private ObjectStoreFileStorageFactoryBean objectStoreFileStorageFactoryBean;

    @Mock
    private EnvironmentServicesFinder environmentServicesFinder;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private JCloudsObjectStoreFileStorage jCloudsObjectStoreFileStorage;

    @Mock
    private GcpObjectStoreFileStorage gcpObjectStoreFileStorage;

    @Mock
    private AzureObjectStoreFileStorage azureObjectStoreFileStorage;

    private ObjectStoreServiceInfo capturedGcpServiceInfo;
    private ObjectStoreServiceInfo capturedAzureServiceInfo;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(applicationConfiguration.getObjectStoreRegions()).thenReturn(Set.of());
        objectStoreFileStorageFactoryBean = new ObjectStoreFileStorageFactoryBeanMock("deploy-service-os", environmentServicesFinder,
                                                                                      applicationConfiguration);
    }

    @Test
    void testObjectStoreCreationWithoutServiceInstance() {
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        FileStorage objectStoreFileStorage = objectStoreFileStorageFactoryBean.getObject();
        assertNull(objectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWithValidServiceInstance() {
        mockCfService();
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        FileStorage objectStoreFileStorage = objectStoreFileStorageFactoryBean.getObject();
        assertNotNull(objectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAws() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);
        ObjectStoreFileStorageFactoryBean spy = spy(objectStoreFileStorageFactoryBean);

        spy.afterPropertiesSet();
        FileStorage createdObjectStoreFileStorage = spy.getObject();

        assertNotNull(createdObjectStoreFileStorage);
        verify(spy, never())
            .createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(jCloudsObjectStoreFileStorage, times(1))
            .testConnection();
        assertTrue(createdObjectStoreFileStorage instanceof JCloudsObjectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAzure() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AZURE);
        ObjectStoreFileStorageFactoryBean spy = spy(objectStoreFileStorageFactoryBean);

        spy.afterPropertiesSet();
        FileStorage createdObjectStoreFileStorage = spy.getObject();

        assertNotNull(createdObjectStoreFileStorage);
        verify(spy, never())
            .createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(azureObjectStoreFileStorage, times(1))
            .testConnection();
        assertTrue(createdObjectStoreFileStorage instanceof AzureObjectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValid() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);
        ObjectStoreFileStorageFactoryBean spy = spy(objectStoreFileStorageFactoryBean);

        spy.afterPropertiesSet();
        FileStorage createdObjectStoreFileStorage = spy.getObject();

        assertNotNull(createdObjectStoreFileStorage);
        verify(spy, never())
            .createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(gcpObjectStoreFileStorage, times(1))
            .testConnection();
        assertTrue(createdObjectStoreFileStorage instanceof GcpObjectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWhenEnvIsInvalid() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn("WRONG_PROVIDER");

        ObjectStoreFileStorageFactoryBean spy = spy(objectStoreFileStorageFactoryBean);

        spy.afterPropertiesSet();
        FileStorage createdObjectStoreFileStorage = spy.getObject();

        assertNotNull(createdObjectStoreFileStorage);
        verify(spy, times(1))
            .createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
    }

    @Test
    void testGcpFactoryReceivesPopulatedCredentialsWhenEnvIsGcp() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);

        objectStoreFileStorageFactoryBean.afterPropertiesSet();

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

        objectStoreFileStorageFactoryBean.afterPropertiesSet();

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

        ObjectStoreFileStorageFactoryBean bean = new ObjectStoreFileStorageFactoryBean("deploy-service-os", environmentServicesFinder,
                                                                                       applicationConfiguration) {
            @Override
            public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
                return List.of(ImmutableObjectStoreServiceInfo.builder()
                                                              .provider(Constants.AWS_S_3)
                                                              .identity(ACCESS_KEY_ID_VALUE)
                                                              .credential(SECRET_ACCESS_KEY_VALUE)
                                                              .container(BUCKET_VALUE)
                                                              .build());
            }
        };

        Exception exception = assertThrows(IllegalStateException.class, bean::afterPropertiesSet);
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
        assertNull(capturedGcpServiceInfo, "GCP factory must not be invoked when no matching provider info is bound");
    }

    @Test
    void testObjectStoreCreationWhenEnvProviderFailsToConnect() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);
        doThrow(new IllegalStateException("Cannot create object store")).when(jCloudsObjectStoreFileStorage)
                                                                        .testConnection();

        Exception exception = assertThrows(IllegalStateException.class, () -> objectStoreFileStorageFactoryBean.afterPropertiesSet());
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    @Test
    void testObjectStoreCreationWithoutValidServiceInstance() {
        mockCfService();
        doThrow(new IllegalStateException("Cannot create object store")).when(jCloudsObjectStoreFileStorage)
                                                                        .testConnection();
        doThrow(new IllegalStateException("Cannot create object store")).when(gcpObjectStoreFileStorage)
                                                                        .testConnection();
        doThrow(new IllegalStateException("Cannot create object store")).when(azureObjectStoreFileStorage)
                                                                        .testConnection();
        Exception exception = assertThrows(IllegalStateException.class, () -> objectStoreFileStorageFactoryBean.afterPropertiesSet());
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    private void mockCfService() {
        CfService cfService = Mockito.mock(CfService.class);
        CfCredentials cfCredentials = Mockito.mock(CfCredentials.class);
        when(cfCredentials.getMap()).thenReturn(buildCredentials());
        when(cfService.getCredentials()).thenReturn(cfCredentials);
        when(environmentServicesFinder.findService("deploy-service-os")).thenReturn(cfService);
    }

    private static Map<String, Object> buildCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.ACCESS_KEY_ID, ACCESS_KEY_ID_VALUE);
        credentials.put(Constants.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY_VALUE);
        credentials.put(Constants.BUCKET, BUCKET_VALUE);
        return credentials;
    }

    private class ObjectStoreFileStorageFactoryBeanMock extends ObjectStoreFileStorageFactoryBean {

        public ObjectStoreFileStorageFactoryBeanMock(String serviceName, EnvironmentServicesFinder environmentServicesFinder,
                                                     ApplicationConfiguration applicationConfiguration) {
            super(serviceName, environmentServicesFinder, applicationConfiguration);
        }

        @Override
        protected JCloudsObjectStoreFileStorage createFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo, BlobStoreContext context) {
            return ObjectStoreFileStorageFactoryBeanTest.this.jCloudsObjectStoreFileStorage;
        }

        @Override
        protected GcpObjectStoreFileStorage createGcpFileStorage(ObjectStoreServiceInfo credentials) {
            ObjectStoreFileStorageFactoryBeanTest.this.capturedGcpServiceInfo = credentials;
            return ObjectStoreFileStorageFactoryBeanTest.this.gcpObjectStoreFileStorage;
        }

        @Override
        protected AzureObjectStoreFileStorage createAzureFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            ObjectStoreFileStorageFactoryBeanTest.this.capturedAzureServiceInfo = objectStoreServiceInfo;
            return ObjectStoreFileStorageFactoryBeanTest.this.azureObjectStoreFileStorage;
        }

        @Override
        public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
            CfService service = environmentServicesFinder.findService("deploy-service-os");
            if (service != null) {
                return new ObjectStoreServiceInfoCreatorMock().getAllProvidersServiceInfo(service.getCredentials()
                                                                                                 .getMap());
            } else {
                return List.of();
            }
        }
    }

    private class ObjectStoreServiceInfoCreatorMock extends ObjectStoreServiceInfoCreator {
    }
}
