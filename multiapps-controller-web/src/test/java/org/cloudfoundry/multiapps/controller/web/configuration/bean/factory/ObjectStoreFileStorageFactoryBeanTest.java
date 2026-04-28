package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.AwsS3ObjectStoreFileStorage;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStoreFileStorageFactoryBeanTest {

    private static final String SERVICE_NAME = "deploy-service-os";
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
    @Mock
    private AwsS3ObjectStoreFileStorage awsS3ObjectStoreFileStorage;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(applicationConfiguration.getObjectStoreRegions()).thenReturn(Set.of());
        objectStoreFileStorageFactoryBean = new ObjectStoreFileStorageFactoryBeanMock(SERVICE_NAME, environmentServicesFinder,
                                                                                      applicationConfiguration);
    }

    @Test
    void testGetObjectType() {
        assertEquals(FileStorage.class, objectStoreFileStorageFactoryBean.getObjectType());
    }

    @Test
    void testGetObjectBeforeInitialization() {
        assertNull(objectStoreFileStorageFactoryBean.getObject());
    }

    @Test
    void testObjectStoreCreationWithoutServiceInstance() {
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        assertNull(objectStoreFileStorageFactoryBean.getObject());
    }

    @Test
    void testObjectStoreCreationWithValidServiceInstance() {
        mockCfService();
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        assertNotNull(objectStoreFileStorageFactoryBean.getObject());
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAws() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy, never()).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(awsS3ObjectStoreFileStorage).testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForAzure() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AZURE);
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(AzureObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy, never()).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(azureObjectStoreFileStorage).testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvIsValidForGcp() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(GcpObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy, never()).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
        verify(gcpObjectStoreFileStorage).testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvIsInvalidFallsBackToFirstReachableProvider() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn("WRONG_PROVIDER");
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
    }

    @Test
    void testObjectStoreCreationWhenEnvIsNullFallsBackToFirstReachableProvider() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(null);
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
    }

    @Test
    void testObjectStoreCreationWhenEnvIsEmptyFallsBackToFirstReachableProvider() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn("");
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);

        factoryBeanSpy.afterPropertiesSet();
        var result = factoryBeanSpy.getObject();

        assertInstanceOf(AwsS3ObjectStoreFileStorage.class, result);
        verify(factoryBeanSpy).createObjectStoreFromFirstReachableProvider(anyMap(), anyList());
    }

    @Test
    void testObjectStoreCreationFallsBackToNextProviderWhenFirstFails() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(null);
        doThrow(new IllegalStateException("AWS connection failed")).when(awsS3ObjectStoreFileStorage)
                                                                   .testConnection();

        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        var result = objectStoreFileStorageFactoryBean.getObject();

        // AWS (two entries) fails, then Aliyun (JClouds default path) succeeds
        assertInstanceOf(JCloudsObjectStoreFileStorage.class, result);
        verify(jCloudsObjectStoreFileStorage).testConnection();
    }

    @Test
    void testObjectStoreCreationFallsBackToAzureWhenAwsAndJCloudsFail() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(null);
        doThrow(new IllegalStateException("AWS connection failed")).when(awsS3ObjectStoreFileStorage)
                                                                   .testConnection();
        doThrow(new IllegalStateException("JClouds connection failed")).when(jCloudsObjectStoreFileStorage)
                                                                       .testConnection();

        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        var result = objectStoreFileStorageFactoryBean.getObject();

        assertInstanceOf(AzureObjectStoreFileStorage.class, result);
        verify(azureObjectStoreFileStorage).testConnection();
    }

    @Test
    void testObjectStoreCreationWhenEnvProviderFailsToConnect() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.AWS);
        doThrow(new IllegalStateException("Cannot create object store")).when(awsS3ObjectStoreFileStorage)
                                                                        .testConnection();

        var exception = assertThrows(IllegalStateException.class, () -> objectStoreFileStorageFactoryBean.afterPropertiesSet());
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    @Test
    void testObjectStoreCreationWhenEnvProviderNotFoundInAvailableProviders() {
        mockCfService();
        when(applicationConfiguration.getObjectStoreClientType()).thenReturn(Constants.GCP);
        var factoryBeanSpy = spy(objectStoreFileStorageFactoryBean);
        doReturn(List.of(buildServiceInfo(Constants.AWS_S_3))).when(factoryBeanSpy)
                                                              .getProvidersServiceInfo();

        var exception = assertThrows(IllegalStateException.class, factoryBeanSpy::afterPropertiesSet);
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
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

        var exception = assertThrows(IllegalStateException.class, () -> objectStoreFileStorageFactoryBean.afterPropertiesSet());
        assertEquals(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND, exception.getMessage());
    }

    private ObjectStoreServiceInfo buildServiceInfo(String provider) {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(provider)
                                              .credentials(buildCredentials())
                                              .build();
    }

    private void mockCfService() {
        CfService cfService = Mockito.mock(CfService.class);
        CfCredentials cfCredentials = Mockito.mock(CfCredentials.class);
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
        protected GcpObjectStoreFileStorage createGcpFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            return ObjectStoreFileStorageFactoryBeanTest.this.gcpObjectStoreFileStorage;
        }

        @Override
        protected AzureObjectStoreFileStorage createAzureFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            return ObjectStoreFileStorageFactoryBeanTest.this.azureObjectStoreFileStorage;
        }

        @Override
        protected AwsS3ObjectStoreFileStorage createAwsS3FileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
            return ObjectStoreFileStorageFactoryBeanTest.this.awsS3ObjectStoreFileStorage;
        }

        @Override
        public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
            CfService service = environmentServicesFinder.findService(SERVICE_NAME);
            if (service != null) {
                return new ObjectStoreServiceInfoCreator().getAllProvidersServiceInfo(service.getCredentials()
                                                                                             .getMap());
            }
            return List.of();
        }
    }

}
