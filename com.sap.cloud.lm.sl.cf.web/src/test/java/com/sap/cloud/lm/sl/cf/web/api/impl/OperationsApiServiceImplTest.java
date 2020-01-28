package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.RetryProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.StartProcessAction;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.util.OperationsHelper;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

public class OperationsApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private OperationService operationService;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;
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
    private String processId;

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        operations = new LinkedList<>();
        operations.add(createOperation(FINISHED_PROCESS, Operation.State.FINISHED));
        operations.add(createOperation(RUNNING_PROCESS, Operation.State.RUNNING));
        operations.add(createOperation(ERROR_PROCESS, Operation.State.ERROR));
        operations.add(createOperation(ABORTED_PROCESS, Operation.State.ABORTED));

        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
        setupOperationServiceMock();
        setupOperationsHelperMock();
        mockProcessActionRegistry();
        mockFlowableFacade();
        mockClientProvider(EXAMPLE_USER);
    }

    @Test
    public void testGetOperations() {
        ResponseEntity<List<Operation>> response = testedClass.getOperations(SPACE_GUID, Arrays.asList(Operation.State.FINISHED.toString(),
                                                                                                       Operation.State.ABORTED.toString()),
                                                                             1);

        List<Operation> operations = response.getBody();
        assertEquals(2, operations.size());
        assertEquals(Operation.State.FINISHED, operations.get(0)
                                                         .getState());
        assertEquals(Operation.State.ABORTED, operations.get(1)
                                                        .getState());

    }

    @Test
    public void testGetOperationsNotFound() {
        ResponseEntity<List<Operation>> response = testedClass.getOperations(SPACE_GUID,
                                                                             Collections.singletonList(Operation.State.ACTION_REQUIRED.toString()),
                                                                             1);

        List<Operation> operations = response.getBody();
        assertTrue(operations.isEmpty());

    }

    @Test
    public void testGetOperation() {
        String processId = FINISHED_PROCESS;
        ResponseEntity<Operation> response = testedClass.getOperation(SPACE_GUID, processId, null);
        Operation operation = response.getBody();
        assertEquals(processId, operation.getProcessId());
        assertEquals(Operation.State.FINISHED, operation.getState());
    }

    @Test
    public void testGetOperationMissing() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getOperation(SPACE_GUID, "notPresent", null));
    }

    @Test
    public void testExecuteOperationAction() {
        String processId = RUNNING_PROCESS;
        testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, processId, AbortProcessAction.ACTION_ID_ABORT);
        Mockito.verify(processAction)
               .execute(Mockito.eq(EXAMPLE_USER), Mockito.eq(processId));
    }

    @Test
    public void testExecuteOperationActionMissingProcess() {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID,
                                                                         "notavalidpprocess", AbortProcessAction.ACTION_ID_ABORT));
    }

    @Test
    public void testExecuteOperationActionInvalidAction() {
        assertThrows(IllegalArgumentException.class,
                     () -> testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, RUNNING_PROCESS,
                                                              StartProcessAction.ACTION_ID_START));
    }

    @Test
    public void testExecuteOperationActionUnauthorized() {
        Assertions.assertThrows(ResponseStatusException.class,
                                () -> testedClass.executeOperationAction(mockHttpServletRequest(null), SPACE_GUID, RUNNING_PROCESS,
                                                                         AbortProcessAction.ACTION_ID_ABORT));
    }

    @Test
    public void testStartOperation() {
        testedClass.startOperation(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, createOperation(null, null));
        Mockito.verify(flowableFacade)
               .startProcess(Mockito.any(), Mockito.anyMap());
    }

    @Test
    public void testGetOperationLogs() throws Exception {
        String processId = FINISHED_PROCESS;
        testedClass.getOperationLogs(SPACE_GUID, processId);
        Mockito.verify(logsService)
               .getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId));
    }

    @Test
    public void testGetOperationLogsNotFoundOperation() {
        assertThrows(NotFoundException.class, () -> testedClass.getOperationLogs(SPACE_GUID, "notarealop"));
    }

    @Test
    public void testGetOperationLogsServiceException() throws Exception {
        String processId = FINISHED_PROCESS;
        Mockito.when(logsService.getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId)))
               .thenThrow(new FileStorageException("something went wrong"));
        Assertions.assertThrows(ContentException.class, () -> testedClass.getOperationLogs(SPACE_GUID, processId));
    }

    @Test
    public void testGetOperationLogContent() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "MAIN_LOG";
        String expectedLogContent = "somelogcontentstring\n1234";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenReturn(expectedLogContent);
        ResponseEntity<String> response = testedClass.getOperationLogContent(SPACE_GUID, processId, logName);
        String logContent = response.getBody();
        assertEquals(expectedLogContent, logContent);
    }

    @Test
    public void testGetOperationLogContentNotFound() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "MAIN_LOG";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenThrow(new NoResultException("log file not found"));
        Assertions.assertThrows(NoResultException.class, () -> testedClass.getOperationLogContent(SPACE_GUID, processId, logName));
    }

    @Test
    public void testGetOperationActionsForRunning() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(AbortProcessAction.ACTION_ID_ABORT), actions);
    }

    @Test
    public void testGetOperationActionsForFinished() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, FINISHED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    public void testGetOperationActionsForAborted() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, ABORTED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    public void testGetOperationActionsForError() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, ERROR_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, RetryProcessAction.ACTION_ID_RETRY), actions);
    }

    @Test
    public void testGetOperationActionsOperationNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getOperationActions(SPACE_GUID, "notarealprocess"));
    }

    @Test
    public void testGetOperationActionsNotFound() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(AbortProcessAction.ACTION_ID_ABORT), actions);
    }

    private void mockFlowableFacade() {
        Mockito.when(flowableFacade.startProcess(Mockito.any(), Mockito.anyMap()))
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
        Mockito.when(clientProvider.getControllerClient(Mockito.any(), Mockito.any()))
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
        Mockito.when(processActionRegistry.getAction(Mockito.any()))
               .thenReturn(processAction);
    }

    private HttpServletRequest mockHttpServletRequest(String user) {
        HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
        if (user != null) {
            Principal principalMock = Mockito.mock(Principal.class);
            Mockito.when(principalMock.getName())
                   .thenReturn(user);
            Mockito.when(requestMock.getUserPrincipal())
                   .thenReturn(principalMock);
        }
        return requestMock;
    }

    private void setupOperationServiceMock() {
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);
        OperationQuery operationQueryMock = new MockBuilder<>(operationQuery).on(query -> query.processId(Mockito.any()),
                                                                                 invocation -> processId = (String) invocation.getArguments()[0])
                                                                             .build();
        Mockito.doAnswer(invocation -> {
            Optional<Operation> foundOperation = operations.stream()
                                                           .filter(operation -> operation.getProcessId()
                                                                                         .equals(processId))
                                                           .findFirst();
            if (!foundOperation.isPresent()) {
                throw new NoResultException("not found");
            }
            return foundOperation.get();
        })
               .when(operationQueryMock)
               .singleResult();
    }

    @SuppressWarnings("unchecked")
    private void setupOperationsHelperMock() {
        Mockito.when(operationsHelper.findOperations(Mockito.any(), Mockito.anyList()))
               .thenAnswer(new Answer<List<Operation>>() {

                   @Override
                   public List<Operation> answer(InvocationOnMock invocation) {
                       List<Operation.State> states = (List<Operation.State>) invocation.getArguments()[1];
                       return operations.stream()
                                        .filter(operation -> states.contains(operation.getState()))
                                        .collect(Collectors.toList());
                   }
               });
        Mockito.when(operationsHelper.addState(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(operationsHelper.addErrorType(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Operation createOperation(String processId, Operation.State state) {
        return ImmutableOperation.builder()
                                 .state(state)
                                 .spaceId(SPACE_GUID)
                                 .processId(processId)
                                 .processType(ProcessType.DEPLOY)
                                 .build();
    }
}
