package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;

import org.activiti.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionException;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;

public class ActivitiHistoricDataCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";

    @Mock
    private ActivitiFacade activitiFacade;
    @InjectMocks
    private ActivitiHistoricDataCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRemoveActivitiHistoricDataWithSubProcesses() throws JobExecutionException {
        HistoricProcessInstance mockedProcess1 = mock(HistoricProcessInstance.class);
        when(mockedProcess1.getId()).thenReturn(OPERATION_ID_1);
        HistoricProcessInstance mockedProcess2 = mock(HistoricProcessInstance.class);
        when(mockedProcess2.getId()).thenReturn(OPERATION_ID_2);
        when(activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(EXPIRATION_TIME))
            .thenReturn(Arrays.asList(mockedProcess1, mockedProcess2));

        cleaner.execute(EXPIRATION_TIME);
        verify(activitiFacade).deleteHistoricProcessInstance(OPERATION_ID_1);
        verify(activitiFacade).deleteHistoricProcessInstance(OPERATION_ID_2);
    }

}
