package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ObjectStoreServiceInfoCreatorTest {

    private static final String ACCESS_KEY_ID_VALUE = "access_key_id_value";
    private static final String SECRET_ACCESS_KEY_VALUE = "secret_access_key_value";
    private static final String BUCKET_VALUE = "bucket_value";
    private static final String REGION_VALUE = "region_value";
    private static final String ENDPOINT_VALUE = "endpoint_value";
    private static final Map<String, Object> CREDENTIALS = Map.of("test", "test1");

    private ObjectStoreServiceInfoCreator objectStoreServiceInfoCreator;

    @BeforeEach
    void setUp() {
        objectStoreServiceInfoCreator = new ObjectStoreServiceInfoCreatorMock();
    }

    static Stream<Arguments> testDifferentProviders() {
        return Stream.of(Arguments.of(buildCfService(buildAliCloudCredentials()), buildAliCloudObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildAwsCredentials()), buildAwsObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildSdkCredentials()), buildAzureObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildSdkCredentials()), buildGcoObjectStoreServiceInfo()));
    }

    @ParameterizedTest
    @MethodSource
    void testDifferentProviders(CfService cfService, ObjectStoreServiceInfo exprectedObjectStoreServiceInfo) {
        List<ObjectStoreServiceInfo> providersServiceInfo = objectStoreServiceInfoCreator.getAllProvidersServiceInfo(
            cfService.getCredentials()
                     .getMap());
        assertTrue(providersServiceInfo.contains(exprectedObjectStoreServiceInfo));
    }

    private static CfService buildCfService(Map<String, Object> credentials) {
        CfService cfService = Mockito.mock(CfService.class);
        CfCredentials cfCredentials = Mockito.mock(CfCredentials.class);
        when(cfCredentials.getMap()).thenReturn(credentials);
        when(cfService.getCredentials()).thenReturn(cfCredentials);
        return cfService;
    }

    private static Map<String, Object> buildAliCloudCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.ACCESS_KEY_ID, ACCESS_KEY_ID_VALUE);
        credentials.put(Constants.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY_VALUE);
        credentials.put(Constants.BUCKET, BUCKET_VALUE);
        credentials.put(Constants.REGION, REGION_VALUE);
        credentials.put(Constants.ENDPOINT, ENDPOINT_VALUE);
        return credentials;
    }

    private static ObjectStoreServiceInfo buildAliCloudObjectStoreServiceInfo() {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.ALIYUN_OSS)
                                              .identity(ACCESS_KEY_ID_VALUE)
                                              .credential(SECRET_ACCESS_KEY_VALUE)
                                              .container(BUCKET_VALUE)
                                              .endpoint(ENDPOINT_VALUE)
                                              .region(REGION_VALUE)
                                              .build();
    }

    private static Map<String, Object> buildAwsCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.ACCESS_KEY_ID, ACCESS_KEY_ID_VALUE);
        credentials.put(Constants.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY_VALUE);
        credentials.put(Constants.BUCKET, BUCKET_VALUE);
        return credentials;
    }

    private static ObjectStoreServiceInfo buildAwsObjectStoreServiceInfo() {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AWS_S_3)
                                              .identity(ACCESS_KEY_ID_VALUE)
                                              .credential(SECRET_ACCESS_KEY_VALUE)
                                              .container(BUCKET_VALUE)
                                              .build();
    }

    private static Map<String, Object> buildSdkCredentials() {
        return CREDENTIALS;
    }

    private static ObjectStoreServiceInfo buildAzureObjectStoreServiceInfo() {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AZUREBLOB)
                                              .credentials(CREDENTIALS)
                                              .build();
    }

    private static ObjectStoreServiceInfo buildGcoObjectStoreServiceInfo() {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.GOOGLE_CLOUD_STORAGE)
                                              .credentials(CREDENTIALS)
                                              .build();
    }

    private static class ObjectStoreServiceInfoCreatorMock extends ObjectStoreServiceInfoCreator {
    }
}
