package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FlowableHistoricDataCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";
    private static final String OPERATION_ID_3 = "3";
    private static final int PAGE_SIZE = 2;

    @Mock
    private HistoryService historyService;
    private FlowableHistoricDataCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        this.cleaner = new FlowableHistoricDataCleaner(historyService, PAGE_SIZE);
    }

    @Test
    public void testExecuteWithMultiplePages() {
        HistoricProcessInstance process1 = mockHistoricProcessInstanceWithId(OPERATION_ID_1);
        HistoricProcessInstance process2 = mockHistoricProcessInstanceWithId(OPERATION_ID_2);
        HistoricProcessInstance process3 = mockHistoricProcessInstanceWithId(OPERATION_ID_3);
        List<HistoricProcessInstance> page1 = Arrays.asList(process1, process2);
        List<HistoricProcessInstance> page2 = Arrays.asList(process3);
        List<HistoricProcessInstance> page3 = Collections.emptyList();

        HistoricProcessInstanceQuery query = mockHistoricProcessInstanceQueryWithPages(Arrays.asList(page1, page2, page3));
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);

        cleaner.execute(EXPIRATION_TIME);
        verify(historyService).deleteHistoricProcessInstance(OPERATION_ID_1);
        verify(historyService).deleteHistoricProcessInstance(OPERATION_ID_2);
        verify(historyService).deleteHistoricProcessInstance(OPERATION_ID_3);
    }

    private HistoricProcessInstance mockHistoricProcessInstanceWithId(String id) {
        HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);
        when(historicProcessInstance.getId()).thenReturn(id);
        return historicProcessInstance;
    }

    private HistoricProcessInstanceQuery mockHistoricProcessInstanceQueryWithPages(List<List<HistoricProcessInstance>> pages) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
        when(historicProcessInstanceQuery.startedBefore(EXPIRATION_TIME)).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenAnswer(AdditionalAnswers.returnsElementsOf(pages));
        long processesCount = getTotalProcessesCount(pages);
        when(historicProcessInstanceQuery.count()).thenReturn(processesCount);
        return historicProcessInstanceQuery;
    }

    private long getTotalProcessesCount(List<List<HistoricProcessInstance>> pages) {
        return pages.stream()
                    .flatMap(page -> page.stream())
                    .count();
    }

    @Test
    public void testExecuteResilience() {
        HistoricProcessInstance process1 = mockHistoricProcessInstanceWithId(OPERATION_ID_1);
        HistoricProcessInstance process2 = mockHistoricProcessInstanceWithId(OPERATION_ID_2);
        List<HistoricProcessInstance> page1 = Arrays.asList(process1, process2);
        List<HistoricProcessInstance> page2 = Collections.emptyList();

        HistoricProcessInstanceQuery query = mockHistoricProcessInstanceQueryWithPages(Arrays.asList(page1, page2));
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        doThrow(new FlowableObjectNotFoundException("Oops! Someone was faster than you!")).when(historyService)
                                                                                          .deleteHistoricProcessInstance(OPERATION_ID_1);

        cleaner.execute(EXPIRATION_TIME);
        verify(historyService).deleteHistoricProcessInstance(OPERATION_ID_1);
        verify(historyService).deleteHistoricProcessInstance(OPERATION_ID_2);
    }

}
