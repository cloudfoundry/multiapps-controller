package org.cloudfoundry.multiapps.controller.web.configuration.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.jclouds.domain.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.google.common.base.Supplier;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;

class ObjectStoreServiceInfoCreatorTest {

    private static final String ACCESS_KEY_ID_VALUE = "access_key_id_value";
    private static final String SECRET_ACCESS_KEY_VALUE = "secret_access_key_value";
    private static final String BUCKET_VALUE = "bucket_value";
    private static final String REGION_VALUE = "region_value";
    private static final String ENDPOINT_VALUE = "endpoint_value";
    private static final String ACCOUNT_NAME_VALUE = "account_name_value";
    private static final String SAS_TOKEN_VALUE = "sas_token_value";
    private static final String CONTAINER_NAME_VALUE = "container_name_value";
    private static final String CONTAINER_URI_VALUE = "https://container.com:8080";
    private static final Supplier<Credentials> CREDENTIALS_SUPPLIER = () -> null;

    private ObjectStoreServiceInfoCreator objectStoreServiceInfoCreator;

    @BeforeEach
    void setUp() {
        objectStoreServiceInfoCreator = new ObjectStoreServiceInfoCreatorMock();
    }

    static Stream<Arguments> testDifferentProviders() throws MalformedURLException {
        return Stream.of(Arguments.of(buildCfService(buildAliCloudCredentials()), buildAliCloudObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildAwsCredentials()), buildAwsObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildAzureCredentials()), buildAzureObjectStoreServiceInfo()),
                         Arguments.of(buildCfService(buildGcpCredentials()), buildGcpObjectStoreServiceInfo()));
    }

    @ParameterizedTest
    @MethodSource
    void testDifferentProviders(CfService cfService, ObjectStoreServiceInfo exprectedObjectStoreServiceInfo) {
        List<ObjectStoreServiceInfo> providersServiceInfo = objectStoreServiceInfoCreator.getAllProvidersServiceInfo(cfService);
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

    private static Map<String, Object> buildAzureCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.ACCOUNT_NAME, ACCOUNT_NAME_VALUE);
        credentials.put(Constants.SAS_TOKEN, SAS_TOKEN_VALUE);
        credentials.put(Constants.CONTAINER_NAME, CONTAINER_NAME_VALUE);
        credentials.put(Constants.CONTAINER_URI, CONTAINER_URI_VALUE);
        return credentials;
    }

    private static ObjectStoreServiceInfo buildAzureObjectStoreServiceInfo() throws MalformedURLException {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AZUREBLOB)
                                              .identity(ACCOUNT_NAME_VALUE)
                                              .credential(SAS_TOKEN_VALUE)
                                              .endpoint(new URL("https", "container.com", 8080, "").toString())
                                              .container(CONTAINER_NAME_VALUE)
                                              .build();
    }

    private static Map<String, Object> buildGcpCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put(Constants.BUCKET, BUCKET_VALUE);
        credentials.put(Constants.REGION, REGION_VALUE);
        credentials.put(Constants.BASE_64_ENCODED_PRIVATE_KEY_DATA, Base64.getEncoder()
                                                                          .encodeToString("encoded_data".getBytes(StandardCharsets.UTF_8)));
        return credentials;
    }

    private static ObjectStoreServiceInfo buildGcpObjectStoreServiceInfo() {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.GOOGLE_CLOUD_STORAGE_CUSTOM)
                                              .credentialsSupplier(CREDENTIALS_SUPPLIER)
                                              .container(BUCKET_VALUE)
                                              .region(REGION_VALUE)
                                              .build();
    }

    private static class ObjectStoreServiceInfoCreatorMock extends ObjectStoreServiceInfoCreator {

        @Override
        protected Supplier<Credentials> getGcpCredentialsSupplier(Map<String, Object> credentials) {
            return ObjectStoreServiceInfoCreatorTest.CREDENTIALS_SUPPLIER;
        }
    }
}
