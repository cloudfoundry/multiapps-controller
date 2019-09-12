package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.RetryProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.StartProcessAction;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.util.OperationsHelper;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

public class OperationsApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private OperationDao dao;
    @Spy
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    @Mock
    private ProcessLogsPersistenceService logsService;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private OperationsHelper operationsHelper;
    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProcessActionRegistry processActionRegistry;
    @Mock
    private ProcessAction processAction;

    @InjectMocks
    private OperationsApiServiceImpl testedClass;

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String ORG_GUID = "0a42c085-b772-4b1e-bf4d-75c463aab5f6";

    private static final String ORG_NAME = "orgName";
    private static final String SPACE_NAME = "spaceName";

    private static final String EXAMPLE_USER = "someUser123";

    private static final String FINISHED_PROCESS = "1";
    private static final String RUNNING_PROCESS = "2";
    private static final String ERROR_PROCESS = "3";
    private static final String ABORTED_PROCESS = "4";

    private List<Operation> operations;

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        operations = new LinkedList<>();
        operations.add(createOperation(FINISHED_PROCESS, State.FINISHED));
        operations.add(createOperation(RUNNING_PROCESS, State.RUNNING));
        operations.add(createOperation(ERROR_PROCESS, State.ERROR));
        operations.add(createOperation(ABORTED_PROCESS, State.ABORTED));

        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
        setupDaoMock();
        setupOperationsHelperMock();
        mockProcessActionRegistry();
        mockFlowableFacade();
    }

    @Test
    public void testGetMtaOperations() throws Exception {
        Response response = testedClass.getMtaOperations(1, Arrays.asList(State.FINISHED.toString(), State.ABORTED.toString()),
                                                         mockSecurityContext(EXAMPLE_USER), SPACE_GUID);

        @SuppressWarnings("unchecked")
        List<Operation> operations = (List<Operation>) response.getEntity();
        assertEquals(2, operations.size());
        assertEquals(State.FINISHED, operations.get(0)
                                               .getState());
        assertEquals(State.ABORTED, operations.get(1)
                                              .getState());

    }

    @Test
    public void testGetMtaOperationsNotFound() throws Exception {
        Response response = testedClass.getMtaOperations(1, Arrays.asList(State.ACTION_REQUIRED.toString()),
                                                         mockSecurityContext(EXAMPLE_USER), SPACE_GUID);

        @SuppressWarnings("unchecked")
        List<Operation> operations = (List<Operation>) response.getEntity();
        assertTrue(operations.isEmpty());

    }

    @Test
    public void testGetMtaOperation() throws Exception {
        String processId = FINISHED_PROCESS;
        Response response = testedClass.getMtaOperation(processId, null, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        Operation operation = (Operation) response.getEntity();
        assertEquals(processId, operation.getProcessId());
        assertEquals(State.FINISHED, operation.getState());
    }

    @Test
    public void testGetMtaOperationMissing() throws Exception {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.getMtaOperation("notPresent", null, mockSecurityContext(EXAMPLE_USER), SPACE_GUID));
    }

    @Test
    public void testExecuteOperationAction() throws Exception {
        String processId = RUNNING_PROCESS;
        testedClass.executeOperationAction(processId, AbortProcessAction.ACTION_ID_ABORT, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        Mockito.verify(processAction)
               .execute(Mockito.eq(EXAMPLE_USER), Mockito.eq(processId));
    }

    @Test
    public void testExecuteOperationActionMissingProcess() throws Exception {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.executeOperationAction("notavalidpprocess", AbortProcessAction.ACTION_ID_ABORT,
                                                                         mockSecurityContext(EXAMPLE_USER), SPACE_GUID));
    }

    @Test
    public void testExecuteOperationActionInvalidAction() throws Exception {
        Response response = testedClass.executeOperationAction(RUNNING_PROCESS, StartProcessAction.ACTION_ID_START,
                                                               mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void testExecuteOperationActionUnauthorized() throws Exception {
        Assertions.assertThrows(WebApplicationException.class,
                                () -> testedClass.executeOperationAction(RUNNING_PROCESS, AbortProcessAction.ACTION_ID_ABORT,
                                                                         mockSecurityContext(null), SPACE_GUID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStartMtaOperation() throws Exception {
        testedClass.startMtaOperation(createOperation(null, null), mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        Mockito.verify(flowableFacade)
               .startProcess(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap());
    }

    @Test
    public void testGetMtaOperationLogs() throws Exception {
        String processId = FINISHED_PROCESS;
        testedClass.getMtaOperationLogs(processId, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        Mockito.verify(logsService)
               .getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId));
    }

    @Test
    public void testGetMtaOperationLogsNotFoundOperation() throws Exception {
        Response response = testedClass.getMtaOperationLogs("notarealop", mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetMtaOperationLogsServiceException() throws Exception {
        String processId = FINISHED_PROCESS;
        Mockito.when(logsService.getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId)))
               .thenThrow(new FileStorageException("something went wrong"));
        Assertions.assertThrows(ContentException.class,
                                () -> testedClass.getMtaOperationLogs(processId, mockSecurityContext(EXAMPLE_USER), SPACE_GUID));
    }

    @Test
    public void testGetMtaOperationLogContent() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "MAIN_LOG";
        String expectedLogContent = "somelogcontentstring\n1234";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenReturn(expectedLogContent);
        Response response = testedClass.getMtaOperationLogContent(processId, logName, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        String logContent = (String) response.getEntity();
        assertEquals(expectedLogContent, logContent);
    }

    @Test
    public void testGetMtaOperationLogContentNotFound() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "MAIN_LOG";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenThrow(new NotFoundException("log file not found"));
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.getMtaOperationLogContent(processId, logName, mockSecurityContext(EXAMPLE_USER),
                                                                            SPACE_GUID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationActionsForRunning() throws Exception {
        Response response = testedClass.getOperationActions(RUNNING_PROCESS, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        List<String> actions = (List<String>) response.getEntity();
        assertEquals(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT), actions);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationActionsForFinished() throws Exception {
        Response response = testedClass.getOperationActions(FINISHED_PROCESS, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        List<String> actions = (List<String>) response.getEntity();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationActionsForAborted() throws Exception {
        Response response = testedClass.getOperationActions(ABORTED_PROCESS, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        List<String> actions = (List<String>) response.getEntity();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationActionsForError() throws Exception {
        Response response = testedClass.getOperationActions(ERROR_PROCESS, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        List<String> actions = (List<String>) response.getEntity();
        assertEquals(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, RetryProcessAction.ACTION_ID_RETRY), actions);
    }

    @Test
    public void testGetOperationActionsOperationNotFound() throws Exception {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.getOperationActions("notarealprocess", mockSecurityContext(EXAMPLE_USER), SPACE_GUID));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperationActionsNotFound() throws Exception {
        Response response = testedClass.getOperationActions(RUNNING_PROCESS, mockSecurityContext(EXAMPLE_USER), SPACE_GUID);
        List<String> actions = (List<String>) response.getEntity();
        assertEquals(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT), actions);
    }

    @SuppressWarnings("unchecked")
    private void mockFlowableFacade() {
        Mockito.when(flowableFacade.startProcess(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
               .thenReturn(Mockito.mock(ProcessInstance.class));
    }

    private void mockClientProvider(String user) {
        com.sap.cloud.lm.sl.cf.core.util.UserInfo userInfo = new com.sap.cloud.lm.sl.cf.core.util.UserInfo(null, user, null);
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getPrincipal())
               .thenReturn(userInfo);
        org.springframework.security.core.context.SecurityContext securityContextMock = Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        Mockito.when(securityContextMock.getAuthentication())
               .thenReturn(auth);
        CloudControllerClient mockedClient = mockClient();
        Mockito.when(clientProvider.getControllerClient(Mockito.anyString(), Mockito.anyString()))
               .thenReturn(mockedClient);
    }

    private CloudControllerClient mockClient() {
        CloudControllerClient client = Mockito.mock(CloudControllerClient.class);
        ImmutableCloudOrganization organization = ImmutableCloudOrganization.builder()
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .guid(UUID.fromString(ORG_GUID))
                                                                                                            .build())
                                                                            .name(ORG_NAME)
                                                                            .build();
        ImmutableCloudSpace space = ImmutableCloudSpace.builder()
                                                       .metadata(ImmutableCloudMetadata.builder()
                                                                                       .guid(UUID.fromString(SPACE_GUID))
                                                                                       .build())
                                                       .name(SPACE_NAME)
                                                       .organization(organization)
                                                       .build();
        Mockito.when(client.getSpace((UUID) Mockito.any()))
               .thenReturn(space);
        return client;
    }

    private void mockProcessActionRegistry() {
        Mockito.when(processActionRegistry.getAction(Mockito.anyString()))
               .thenReturn(processAction);
    }

    private SecurityContext mockSecurityContext(String user) {
        SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
        if (user != null) {
            Principal principalMock = Mockito.mock(Principal.class);
            Mockito.when(principalMock.getName())
                   .thenReturn(user);
            Mockito.when(securityContextMock.getUserPrincipal())
                   .thenReturn(principalMock);
            mockClientProvider(user);
        }
        return securityContextMock;
    }

    private void setupDaoMock() {
        Mockito.when(dao.find(Mockito.anyString()))
               .then(new Answer<Operation>() {
                   @Override
                   public Operation answer(InvocationOnMock invocation) throws Throwable {
                       String processId = (String) invocation.getArguments()[0];
                       Optional<Operation> found = operations.stream()
                                                             .filter(operation -> operation.getProcessId()
                                                                                           .equals(processId))
                                                             .findFirst();
                       if (!found.isPresent()) {
                           return null;
                       } else {
                           return found.get();
                       }
                   }
               });

        Mockito.when(dao.findRequired(Mockito.anyString()))
               .thenAnswer(new Answer<Operation>() {
                   @Override
                   public Operation answer(InvocationOnMock invocation) throws Throwable {
                       String processId = (String) invocation.getArguments()[0];
                       Optional<Operation> found = operations.stream()
                                                             .filter(operation -> operation.getProcessId()
                                                                                           .equals(processId))
                                                             .findFirst();
                       if (!found.isPresent()) {
                           throw new NotFoundException("not found");
                       } else {
                           return found.get();
                       }
                   }
               });
    }

    @SuppressWarnings("unchecked")
    private void setupOperationsHelperMock() {
        Mockito.when(operationsHelper.findOperations(Mockito.any(), Mockito.anyList()))
               .thenAnswer(new Answer<List<Operation>>() {

                   @Override
                   public List<Operation> answer(InvocationOnMock invocation) throws Throwable {
                       List<State> states = (List<State>) invocation.getArguments()[1];
                       return operations.stream()
                                        .filter(operation -> states.contains(operation.getState()))
                                        .collect(Collectors.toList());
                   }
               });
    }

    private Operation createOperation(String processId, State state) {
        Operation operation = new Operation();
        operation.setState(state);
        operation.setSpaceId(SPACE_GUID);
        operation.setProcessId(processId);
        operation.setProcessType(ProcessType.DEPLOY);
        return operation;
    }
}
