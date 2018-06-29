package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.history.HistoricProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class CleanUpJobTest {

    private static final String SPACE_ID = "space";

    @InjectMocks
    private CleanUpJob cleanUpJob;

    @Mock
    private OperationDao dao;

    @Mock
    private ActivitiFacade activitiFacade;

    private List<Operation> operationsList;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private AbstractFileService fileService;

    @Mock
    private ProgressMessageService progressMessageService;

    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockOperationDao();
        when(configuration.getMaxTtlForOldData()).thenReturn(1L);
    }

    @Test
    public void testAbortOldOperationsInActiveStateOK() throws JobExecutionException {
        Operation olderOperationThatIsRunning = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.RUNNING)
            .processId("1")
            .cleanedUp(false);
        Operation newOperationThatIsRunning = new Operation().startedAt(dateToZonedDate(new Date()))
            .state(State.RUNNING)
            .processId("2");
        Operation olderOperationThatIsAborted = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.ABORTED)
            .processId("3");
        Operation olderOperationThatIsError = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.ERROR)
            .processId("4");

        operationsList = Arrays.asList(olderOperationThatIsRunning, newOperationThatIsRunning, olderOperationThatIsAborted,
            olderOperationThatIsError);
        cleanUpJob.execute(null);
        verify(activitiFacade, times(2)).deleteProcessInstance(any(), any(), any());
    }

    @Mock
    private ApplicationConfiguration configuration;

    @Test
    public void testAll() throws JobExecutionException {
        cleanUpJob.execute(null);
    }

    @Test
    public void testAbortOldOperationsInActiveStateErrorResilience() throws JobExecutionException {
        Operation olderOperationThatIsRunning = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.RUNNING)
            .processId("1");
        Operation olderOperationThatIsError = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.ERROR)
            .processId("2");

        operationsList = Arrays.asList(olderOperationThatIsRunning, olderOperationThatIsError);

        doThrow(new ActivitiOptimisticLockingException("I'm an exception")).when(activitiFacade)
            .deleteProcessInstance(any(), eq(olderOperationThatIsRunning.getProcessId()), any());
        cleanUpJob.execute(null);
        verify(activitiFacade, times(2)).deleteProcessInstance(any(), any(), any());

    }

    @Test
    public void testCleanUpFinishedOperationsDataOK() throws JobExecutionException {
        Operation newerOperationThatIsAborted = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis())))
            .state(State.ABORTED)
            .processId("1")
            .spaceId(SPACE_ID);
        Operation olderOperationThatIsFinished = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.FINISHED)
            .processId("2")
            .spaceId(SPACE_ID);
        operationsList = Arrays.asList(newerOperationThatIsAborted, olderOperationThatIsFinished);

        cleanUpJob.execute(null);
        verify(dao, times(1)).merge(any());
    }

    @Test
    public void testCleanUpFinishedOperationsDataNoMergeError() throws JobExecutionException {
        Operation newerOperationThatIsAborted = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis())))
            .state(State.ABORTED)
            .processId("1")
            .spaceId(SPACE_ID);
        Operation olderOperationThatIsFinished = new Operation().startedAt(dateToZonedDate(new Date(System.currentTimeMillis() - 2000)))
            .state(State.FINISHED)
            .processId("2")
            .spaceId(SPACE_ID);
        operationsList = Arrays.asList(newerOperationThatIsAborted, olderOperationThatIsFinished);

        when(progressMessageService.removeAllByProcessIds(any())).thenThrow(new SLException("I'm also an exception"));

        cleanUpJob.execute(null);
        verify(dao, never()).merge(any());
    }

    @Test
    public void testRemoveActivitiHistoricDataWithSubProcesses() throws JobExecutionException {
        HistoricProcessInstance mockedProcess1 = mock(HistoricProcessInstance.class);
        when(mockedProcess1.getId()).thenReturn("1");
        HistoricProcessInstance mockedProcess2 = mock(HistoricProcessInstance.class);
        when(mockedProcess2.getId()).thenReturn("2");
        when(activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(any())).thenReturn(Arrays.asList(mockedProcess1));
        when(activitiFacade.getHistoricSubProcessIds(eq(mockedProcess1.getId())))
            .thenReturn(new LinkedList<>(Arrays.asList("2", "3", "4")));

        cleanUpJob.execute(null);
        verify(activitiFacade, times(1)).deleteHistoricProcessInstance(eq("2"));
        verify(activitiFacade, times(4)).deleteHistoricProcessInstance(anyString());
    }

    @Test
    public void testSplitAllFilesInChunks() {
        Map<String, List<String>> spaceToFileIds = new HashMap<>();
        List<String> fileIds = new ArrayList<String>();
        fileIds.add(null);
        fileIds.add("9f87be64-6519-4576-b426-42548840f2ec");
        fileIds.add("9f87be64-6519-4516-b426-42543845f2az,9f87ne64-6519-1234-b426-42548840f2gh,9f87be64-1239-4567-b426-34548840f2oq");
        spaceToFileIds.put(SPACE_ID, fileIds);
        Map<String, List<String>> splitAllFilesInChunks = cleanUpJob.splitAllFilesInChunks(spaceToFileIds);
        assertEquals("All file chunks must be five.", 5, splitAllFilesInChunks.get(SPACE_ID)
            .size());

        List<String> expectedFileIds = new ArrayList<String>();
        expectedFileIds.add("9f87be64-6519-4516-b426-42543845f2az");
        expectedFileIds.add("9f87ne64-6519-1234-b426-42548840f2gh");
        expectedFileIds.add("9f87be64-1239-4567-b426-34548840f2oq");
        expectedFileIds.add("9f87be64-6519-4576-b426-42548840f2ec");

        assertTrue("Splited file Ids must match with given ones.", splitAllFilesInChunks.get(SPACE_ID)
            .containsAll(expectedFileIds));
    }

    @Test
    public void testRemoveExpiredTokens() throws JobExecutionException {
        OAuth2AccessToken expiredToken = mock(OAuth2AccessToken.class);
        when(expiredToken.isExpired()).thenReturn(true);
        OAuth2AccessToken activeToken = mock(OAuth2AccessToken.class);
        when(activeToken.isExpired()).thenReturn(false);

        when(tokenStore.findTokensByClientId(anyString())).thenReturn(Arrays.asList(expiredToken, activeToken));

        cleanUpJob.execute(null);
        verify(tokenStore, times(1)).removeAccessToken(eq(expiredToken));
        verify(tokenStore, never()).removeAccessToken(eq(activeToken));
    }

    private void mockOperationDao() {
        when(dao.find((OperationFilter) any())).thenAnswer(new Answer<List<Operation>>() {
            @Override
            public List<Operation> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                OperationFilter filter = (OperationFilter) args[0];

                List<Operation> result = getOperationsList().stream()
                    .filter(operation -> filterOperations(operation, filter))
                    .collect(Collectors.toList());

                return result;
            }
        });

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Operation operation = (Operation) args[0];
                assertTrue("OperationDao should only merge cleaned up operations!", operation.isCleanedUp());
                return null;
            }
        }).when(dao)
            .merge(any());
    }

    private List<Operation> getOperationsList() {
        return operationsList;
    }

    private boolean filterOperations(Operation operation, OperationFilter filter) {
        if (operation.isCleanedUp() != null && filter.isCleanedUp() != operation.isCleanedUp()) {
            return false;
        }

        if (filter.isInNonFinalState() && !State.getActiveStates()
            .contains(operation.getState())) {
            return false;
        }

        Instant beforeDateInstant = filter.getStartTimeUpperBound()
            .toInstant();
        Instant startedAtInstant = operation.getStartedAt()
            .toInstant();

        return beforeDateInstant.isAfter(startedAtInstant);
    }

    private ZonedDateTime dateToZonedDate(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
