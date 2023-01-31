package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.util.ImmutableLogsOffset;
import org.cloudfoundry.multiapps.controller.core.util.LogsOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;

class RecentLogsRetrieverTest {

    private static final String APP_NAME = "my-app";
    private static final Calendar TIMESTAMP = new GregorianCalendar(2010, Calendar.JANUARY, 1);

    private final Tester tester = Tester.forClass(RecentLogsRetrieverTest.class);

    @Mock
    private CloudControllerClient client;

    private final RecentLogsRetriever recentLogsRetriever = new RecentLogsRetriever();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetRecentLogsWithError() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));

        tester.test(() -> recentLogsRetriever.getRecentLogs(client, APP_NAME, null),
                    new Expectation(Expectation.Type.EXCEPTION, "500 Something fails"));
    }

    @Test
    void testGetRecentLogsWithErrorFailSafe() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));
        assertTrue(recentLogsRetriever.getRecentLogsSafely(client, APP_NAME, null)
                                      .isEmpty());
    }

    @Test
    void testGetRecentLogsWithNoPriorOffset() {
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(1, "")));
        assertEquals(List.of(createAppLog(1, "")), recentLogsRetriever.getRecentLogs(client, APP_NAME, null));
    }

    @Test
    void testGetRecentLogsWithOffsetReturnsNoLogs() {
        LogsOffset offset = createLogsOffset(1, "");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(0, "")));
        assertTrue(recentLogsRetriever.getRecentLogs(client, APP_NAME, offset)
                                      .isEmpty());
    }

    @Test
    void testGetRecentLogsWithOffsetSameMessageReturnsNoLogs() {
        LogsOffset offset = createLogsOffset(1, "msg");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(1, "msg")));
        assertTrue(recentLogsRetriever.getRecentLogs(client, APP_NAME, offset)
                                      .isEmpty());
    }

    @Test
    void testGetRecentLogsWithOffsetReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(1, ""), createAppLog(2, "")));
        assertEquals(List.of(createAppLog(2, "")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    @Test
    void testGetRecentLogsWithOffsetSameTimestampReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "msg");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(1, "msg"), createAppLog(1, "msg1")));
        assertEquals(List.of(createAppLog(1, "msg1")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    @Test
    void testGetRecentLogsWithOffsetSameMessageReturnsFilteredLogs() {
        LogsOffset offset = createLogsOffset(1, "msg1");
        Mockito.when(client.getRecentLogs(APP_NAME))
               .thenReturn(List.of(createAppLog(1, "msg"), createAppLog(1, "msg1")));
        assertEquals(List.of(createAppLog(1, "msg")), recentLogsRetriever.getRecentLogs(client, APP_NAME, offset));
    }

    private ApplicationLog createAppLog(int milis, String message) {
        Calendar cal = (Calendar) TIMESTAMP.clone();
        cal.add(Calendar.MILLISECOND, milis);
        return ImmutableApplicationLog.builder()
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
