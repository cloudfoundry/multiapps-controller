package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.util.ResilientCloudOperationExecutor;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class RecentLogsRetrieverTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String LOGGING_ENDPOINT_URL = "https://api.cf.sap.hana.ondemand.com/log";
    private static final String APP_NAME = "my-app";
    private static final UUID APP_UUID = UUID.randomUUID();

    private final Tester tester = Tester.forClass(getClass());

    @Mock
    private CloudControllerClient client;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private RestTemplate restTemplate;
    private RecentLogsRetriever recentLogsRetriever;

    private final ResilientCloudOperationExecutor fastRetrier = new ResilientCloudOperationExecutor().withRetryCount(1)
                                                                                                     .withWaitTimeBetweenRetriesInMillis(0);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.recentLogsRetriever = createRecentLogsRetriever();
        CloudInfo cloudInfo = Mockito.mock(CloudInfo.class);
        Mockito.when(cloudInfo.getLoggingEndpoint())
               .thenReturn(LOGGING_ENDPOINT_URL);
        Mockito.when(client.getCloudInfo())
               .thenReturn(cloudInfo);
        Mockito.when(client.getApplication(APP_NAME))
               .thenReturn(createDummyApp());
        Mockito.when(client.getCloudControllerUrl())
               .thenReturn(new URL(CONTROLLER_URL));
        Mockito.when(restTemplateFactory.getRestTemplate(client))
               .thenReturn(restTemplate);
    }

    private RecentLogsRetriever createRecentLogsRetriever() {
        return new RecentLogsRetriever(restTemplateFactory).withErrorHandlerFactory(() -> new CustomControllerClientErrorHandler().withExecutorFactory(() -> fastRetrier));
    }

    @Test
    public void testGetRecentLogsWithError() {
        Mockito.when(restTemplate.exchange(LOGGING_ENDPOINT_URL + RecentLogsRetriever.RECENT_LOGS_ENDPOINT, HttpMethod.GET, null,
                                           Resource.class, APP_UUID))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));

        tester.test(() -> recentLogsRetriever.getRecentLogs(client, APP_NAME),
                    new Expectation(Expectation.Type.EXCEPTION, "500 Something fails"));
    }

    @Test
    public void testGetRecentLogsWithErrorFailSafe() {
        Mockito.when(restTemplate.exchange(LOGGING_ENDPOINT_URL + RecentLogsRetriever.RECENT_LOGS_ENDPOINT, HttpMethod.GET, null,
                                           Resource.class, APP_UUID))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));
        assertEquals(Collections.emptyList(), recentLogsRetriever.getRecentLogsSafely(client, APP_NAME));
    }

    private CloudApplication createDummyApp() {
        return ImmutableCloudApplication.builder()
                                        .metadata(ImmutableCloudMetadata.builder()
                                                                        .guid(APP_UUID)
                                                                        .build())
                                        .name(APP_NAME)
                                        .build();
    }

}
