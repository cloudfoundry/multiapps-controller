package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ImmutableApplicationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import com.sap.cloud.lm.sl.cf.core.util.ImmutableLogsOffset;
import com.sap.cloud.lm.sl.cf.core.util.LogsOffset;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class RecentLogsRetrieverTest {

    private static final String APP_NAME = "my-app";
    private static final Calendar TIMESTAMP = new GregorianCalendar(2010, Calendar.JANUARY, 1);

    private final Tester tester = Tester.forClass(RecentLogsRetrieverTest.class);

    @Mock
    private CloudControllerClient client;

    private RecentLogsRetriever recentLogsRetriever = new RecentLogsRetriever();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetRecentLogsWithError() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));

        tester.test(() -> recentLogsRetriever.getRecentLogs(client, APP_NAME, null),
                    new Expectation(Expectation.Type.EXCEPTION, "500 Something fails"));
    }

    @Test
    public void testGetRecentLogsWithErrorFailSafe() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));
        assertTrue(recentLogsRetriever.getRecentLogsSafely(client, APP_NAME, null)
                                      .isEmpty());
    }

    @Test
    public void testGetRecentLogsWithNoPriorOffset() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(1, "")));
        assertEquals(Arrays.asList(createAppLog(1, "")), recentLogsRetriever.getRecentLogs(client, APP_NAME, null));
    }

    @Test
    public void testGetRecentLogsWithOffsetReturnsNoLogs() {
        LogsOffset offset = createLogsOffset(1, "");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(0, "")));
        assertTrue(recentLogsRetriever.getRecentLogs(client, APP_NAME, offset)
                                      .isEmpty());
    }

    @Test
    public void testGetRecentLogsWithOffsetSameMessageReturnsNoLogs() {
        LogsOffset offset = createLogsOffset(1, "msg");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(1, "msg")));
        assertTrue(recentLogsRetriever.getRecentLogs(client, APP_NAME, offset)
                                      .isEmpty());
    }

    @Test
    public void testGetRecentLogsWithOffsetReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(1, ""), createAppLog(2, "")));
        assertEquals(Arrays.asList(createAppLog(2, "")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    @Test
    public void testGetRecentLogsWithOffsetSameTimestampReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "msg");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(1, "msg"), createAppLog(1, "msg1")));
        assertEquals(Arrays.asList(createAppLog(1, "msg1")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    @Test
    public void testGetRecentLogsWithOffsetSameMessageReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "msg1");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(Arrays.asList(createAppLog(1, "msg"), createAppLog(1, "msg1")));
        assertEquals(Arrays.asList(createAppLog(1, "msg")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    private ApplicationLog createAppLog(int milis, String message) {
        Calendar cal = (Calendar) TIMESTAMP.clone();
        cal.add(Calendar.MILLISECOND, milis);
        return ImmutableApplicationLog.builder()
                                      .sourceId("")
                                      .sourceName("")
                                      .messageType(ApplicationLog.MessageType.STDOUT)
                                      .message(message)
                                      .applicationGuid("")
                                      .timestamp(cal.getTime())
                                      .build();
    }

    private LogsOffset createLogsOffset(int milis, String message) {
        Calendar cal = (Calendar) TIMESTAMP.clone();
        cal.add(Calendar.MILLISECOND, milis);
        return ImmutableLogsOffset.builder()
                                  .timestamp(cal.getTime())
                                  .message(message)
                                  .build();
    }
}
