package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.jclouds.blobstore.BlobStoreContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;

class ObjectStoreFileStorageFactoryBeanTest {

    private static final String ACCESS_KEY_ID_VALUE = "access_key_id_value";
    private static final String SECRET_ACCESS_KEY_VALUE = "secret_access_key_value";
    private static final String BUCKET_VALUE = "bucket_value";

    private ObjectStoreFileStorageFactoryBean objectStoreFileStorageFactoryBean;

    @Mock
    private EnvironmentServicesFinder environmentServicesFinder;
    @Mock
    private ObjectStoreFileStorage objectStoreFileStorage;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        objectStoreFileStorageFactoryBean = new ObjectStoreFileStorageFactoryBeanMock("deploy-service-os", environmentServicesFinder);
    }

    @Test
    void testObjectStoreCreationWithoutServiceInstance() {
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        ObjectStoreFileStorage objectStoreFileStorage = objectStoreFileStorageFactoryBean.getObject();
        assertNull(objectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWithValidServiceInstance() {
        mockCfService();
        objectStoreFileStorageFactoryBean.afterPropertiesSet();
        ObjectStoreFileStorage objectStoreFileStorage = objectStoreFileStorageFactoryBean.getObject();
        assertNotNull(objectStoreFileStorage);
    }

    @Test
    void testObjectStoreCreationWithoutValidServiceInstance() {
        mockCfService();
        doThrow(new IllegalStateException("Cannot create object store")).when(objectStoreFileStorage)
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

        public ObjectStoreFileStorageFactoryBeanMock(String serviceName, EnvironmentServicesFinder environmentServicesFinder) {
            super(serviceName, environmentServicesFinder);
        }

        @Override
        protected ObjectStoreFileStorage createFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo, BlobStoreContext context) {
            return ObjectStoreFileStorageFactoryBeanTest.this.objectStoreFileStorage;
        }
    }
}
