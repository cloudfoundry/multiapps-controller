package org.cloudfoundry.multiapps.controller.web.api.impl;

import static org.cloudfoundry.multiapps.controller.core.util.SecurityUtil.USER_INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.Action;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessActionRegistry;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.cloudfoundry.multiapps.controller.process.util.OperationsHelper;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;

class OperationsApiServiceImplTest {

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
    private static final String MTA_ID = "testMta";

    private static final String ORG_NAME = "orgName";
    private static final String SPACE_NAME = "spaceName";

    private static final String EXAMPLE_USER = "someUser123";

    private static final String FINISHED_PROCESS = "1";
    private static final String RUNNING_PROCESS = "2";
    private static final String ERROR_PROCESS = "3";
    private static final String ABORTED_PROCESS = "4";

    private List<Operation> operations;
    private String processId;
    private List<Operation.State> operationStatesToFilter;

    @BeforeEach
    public void initialize() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new LinkedList<>();
        operations.add(createOperation(FINISHED_PROCESS, Operation.State.FINISHED, Collections.emptyMap()));
        operations.add(createOperation(RUNNING_PROCESS, Operation.State.RUNNING, Collections.emptyMap()));
        operations.add(createOperation(ERROR_PROCESS, Operation.State.ERROR, Collections.emptyMap()));
        operations.add(createOperation(ABORTED_PROCESS, Operation.State.ABORTED, Collections.emptyMap()));

        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
        setupOperationServiceMock();
        setupOperationsHelperMock();
        mockProcessActionRegistry();
        mockFlowableFacade();
        mockClientProvider(EXAMPLE_USER);
    }

    @Test
    void testGetOperations() {
        ResponseEntity<List<Operation>> response = testedClass.getOperations(SPACE_GUID, null, List.of(Operation.State.FINISHED.toString(),
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
    void testGetOperationsNotFound() {
        ResponseEntity<List<Operation>> response = testedClass.getOperations(SPACE_GUID, MTA_ID,
                                                                             Collections.singletonList(Operation.State.ACTION_REQUIRED.toString()),
                                                                             1);

        List<Operation> operations = response.getBody();
        assertTrue(operations.isEmpty());

    }

    @Test
    void testGetOperation() {
        String processId = FINISHED_PROCESS;
        ResponseEntity<Operation> response = testedClass.getOperation(SPACE_GUID, processId, null);
        Operation operation = response.getBody();
        assertEquals(processId, operation.getProcessId());
        assertEquals(Operation.State.FINISHED, operation.getState());
    }

    @Test
    void testGetOperationMissing() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getOperation(SPACE_GUID, "notPresent", null));
    }

    @Test
    void testExecuteOperationAction() {
        String processId = RUNNING_PROCESS;
        testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, processId, Action.ABORT.getActionId());
        Mockito.verify(processAction)
               .execute(Mockito.eq(EXAMPLE_USER), Mockito.eq(processId));
    }

    @Test
    void testExecuteOperationActionMissingProcess() {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID,
                                                                         "notavalidpprocess", Action.ABORT.getActionId()));
    }

    @Test
    void testExecuteOperationActionInvalidAction() {
        assertThrows(IllegalArgumentException.class,
                     () -> testedClass.executeOperationAction(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, RUNNING_PROCESS,
                                                              Action.START.getActionId()));
    }

    @Test
    void testExecuteOperationActionUnauthorized() {
        Assertions.assertThrows(ResponseStatusException.class,
                                () -> testedClass.executeOperationAction(mockHttpServletRequest(null), SPACE_GUID, RUNNING_PROCESS,
                                                                         Action.ABORT.getActionId()));
    }

    @Test
    void testStartOperation() {
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test");
        Operation operation = createOperation(null, null, parameters);
        Mockito.when(operationsHelper.getProcessDefinitionKey(operation))
               .thenReturn("deploy");
        testedClass.startOperation(mockHttpServletRequest(EXAMPLE_USER), SPACE_GUID, operation);
        Mockito.verify(flowableFacade)
               .startProcess(Mockito.any(), Mockito.anyMap());
    }

    @Test
    void testGetOperationLogs() throws Exception {
        String processId = FINISHED_PROCESS;
        testedClass.getOperationLogs(SPACE_GUID, processId);
        Mockito.verify(logsService)
               .getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId));
    }

    @Test
    void testGetOperationLogsNotFoundOperation() {
        assertThrows(NotFoundException.class, () -> testedClass.getOperationLogs(SPACE_GUID, "notarealop"));
    }

    @Test
    void testGetOperationLogsServiceException() throws Exception {
        String processId = FINISHED_PROCESS;
        Mockito.when(logsService.getLogNames(Mockito.eq(SPACE_GUID), Mockito.eq(processId)))
               .thenThrow(new FileStorageException("something went wrong"));
        Assertions.assertThrows(ContentException.class, () -> testedClass.getOperationLogs(SPACE_GUID, processId));
    }

    @Test
    void testGetOperationLogContent() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "OPERATION.log";
        String expectedLogContent = "somelogcontentstring\n1234";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenReturn(expectedLogContent);
        ResponseEntity<String> response = testedClass.getOperationLogContent(SPACE_GUID, processId, logName);
        String logContent = response.getBody();
        assertEquals(expectedLogContent, logContent);
    }

    @Test
    void testGetOperationLogContentNotFound() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "OPERATION.log";
        Mockito.when(logsService.getLogContent(Mockito.eq(SPACE_GUID), Mockito.eq(processId), Mockito.eq(logName)))
               .thenThrow(new NoResultException("log file not found"));
        Assertions.assertThrows(NoResultException.class, () -> testedClass.getOperationLogContent(SPACE_GUID, processId, logName));
    }

    @Test
    void testGetOperationActionsForRunning() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(Action.ABORT.getActionId()), actions);
    }

    @Test
    void testGetOperationActionsForFinished() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, FINISHED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    void testGetOperationActionsForAborted() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, ABORTED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    void testGetOperationActionsForError() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, ERROR_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(List.of(Action.ABORT.getActionId(), Action.RETRY.getActionId()), actions);
    }

    @Test
    void testGetOperationActionsOperationNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getOperationActions(SPACE_GUID, "notarealprocess"));
    }

    @Test
    void testGetOperationActionsNotFound() {
        ResponseEntity<List<String>> response = testedClass.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(Action.ABORT.getActionId()), actions);
    }

    private void mockFlowableFacade() {
        Mockito.when(flowableFacade.startProcess(Mockito.any(), Mockito.anyMap()))
               .thenReturn(Mockito.mock(ProcessInstance.class));
    }

    private void mockClientProvider(String user) {
        org.cloudfoundry.multiapps.controller.core.util.UserInfo userInfo = new org.cloudfoundry.multiapps.controller.core.util.UserInfo(null,
                                                                                                                                         user,
                                                                                                                                         null);
        OAuth2AuthenticationToken auth = Mockito.mock(OAuth2AuthenticationToken.class);
        Map<String, Object> attributes = Map.of(USER_INFO, userInfo);
        OAuth2User principal = Mockito.mock(OAuth2User.class);
        Mockito.when(principal.getAttributes())
               .thenReturn(attributes);
        Mockito.when(auth.getPrincipal())
               .thenReturn(principal);
        org.springframework.security.core.context.SecurityContext securityContextMock = Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        Mockito.when(securityContextMock.getAuthentication())
               .thenReturn(auth);
        CloudControllerClient mockedClient = mockClient();
        Mockito.when(clientProvider.getControllerClientWithNoCorrelation(Mockito.any(), Mockito.any()))
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

    @SuppressWarnings("unchecked")
    private void setupOperationServiceMock() {
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);

        Mockito.doAnswer(invocation -> {
            processId = (String) invocation.getArguments()[0];
            return operationQuery;
        })
               .when(operationQuery)
               .processId(Mockito.anyString());
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
               .when(operationQuery)
               .singleResult();

        Mockito.doAnswer(invocation -> {
            operationStatesToFilter = (List<Operation.State>) invocation.getArguments()[0];
            return operationQuery;
        })
               .when(operationQuery)
               .withStateAnyOf(Mockito.anyList());
        Mockito.doAnswer(invocation -> operations.stream()
                                                 .filter(operation -> operationStatesToFilter == null
                                                     || operationStatesToFilter.contains(operation.getState()))
                                                 .collect(Collectors.toList()))
               .when(operationQuery)
               .list();
    }

    @SuppressWarnings("unchecked")
    private void setupOperationsHelperMock() {
        Mockito.when(operationsHelper.addErrorType(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(operationsHelper.releaseLockIfNeeded(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(operationsHelper.releaseLocksIfNeeded(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Operation createOperation(String processId, Operation.State state, Map<String, Object> parameters) {
        return ImmutableOperation.builder()
                                 .state(state)
                                 .spaceId(SPACE_GUID)
                                 .processId(processId)
                                 .processType(ProcessType.DEPLOY)
                                 .parameters(parameters)
                                 .build();
    }
}
