package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.core.auditlogging.OperationsApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
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
import org.cloudfoundry.multiapps.controller.web.monitoring.ApiUsageLogger;
import org.cloudfoundry.multiapps.controller.web.util.OperationRateLimitExceededException;
import org.cloudfoundry.multiapps.controller.web.util.OperationRateLimiter;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import static org.cloudfoundry.multiapps.controller.core.util.SecurityUtil.USER_INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationsApiServiceImplTest {

    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private TokenService tokenService;
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
    @Mock
    private OperationsApiServiceAuditLog operationsApiServiceAuditLog;
    @Mock
    private ApiUsageLogger apiUsageLogger;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private OperationRateLimiter operationRateLimiter;

    private OperationsApiServiceImpl operationsApiService;

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String ORG_GUID = "0a42c085-b772-4b1e-bf4d-75c463aab5f6";
    private static final String MTA_ID = "testMta";

    private static final String ORG_NAME = "orgName";
    private static final String SPACE_NAME = "spaceName";

    private static final String EXAMPLE_USER = "someUser123";
    private static final String USER_GUID = "123-456-789";

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
        operationsApiService = new OperationsApiServiceImpl(clientFactory, tokenService, operationService, operationMetadataMapper,
                                                            logsService, flowableFacade, operationsHelper, progressMessageService,
                                                            processActionRegistry, operationsApiServiceAuditLog, apiUsageLogger,
                                                            httpServletRequest, operationRateLimiter);
        operations = new LinkedList<>();
        operations.add(createOperation(FINISHED_PROCESS, Operation.State.FINISHED, Collections.emptyMap()));
        operations.add(createOperation(RUNNING_PROCESS, Operation.State.RUNNING, Collections.emptyMap()));
        operations.add(createOperation(ERROR_PROCESS, Operation.State.ERROR, Collections.emptyMap()));
        operations.add(createOperation(ABORTED_PROCESS, Operation.State.ABORTED, Collections.emptyMap()));

        setupOperationServiceMock();
        setupOperationsHelperMock();
        mockProcessActionRegistry();
        mockFlowableFacade();
        mockClientProvider(true);
    }

    @Test
    void testGetOperations() {
        ResponseEntity<List<Operation>> response = operationsApiService.getOperations(SPACE_GUID, null,
                                                                                      List.of(Operation.State.FINISHED.toString(),
                                                                                              Operation.State.ABORTED.toString()), 1);

        List<Operation> operations = response.getBody();
        assertEquals(2, operations.size());
        assertEquals(Operation.State.FINISHED, operations.get(0)
                                                         .getState());
        assertEquals(Operation.State.ABORTED, operations.get(1)
                                                        .getState());
    }

    @Test
    void testGetOperationsNotFound() {
        ResponseEntity<List<Operation>> response = operationsApiService.getOperations(SPACE_GUID, MTA_ID, Collections.singletonList(
            Operation.State.ACTION_REQUIRED.toString()), 1);

        List<Operation> operations = response.getBody();
        assertTrue(operations.isEmpty());

    }

    @Test
    void testGetOperation() {
        String processId = FINISHED_PROCESS;
        ResponseEntity<Operation> response = operationsApiService.getOperation(SPACE_GUID, processId, null);
        Operation operation = response.getBody();
        assertEquals(processId, operation.getProcessId());
        assertEquals(Operation.State.FINISHED, operation.getState());
    }

    @Test
    void testGetOperationMissing() {
        Assertions.assertThrows(NotFoundException.class, () -> operationsApiService.getOperation(SPACE_GUID, "notPresent", null));
    }

    @Test
    void testExecuteOperationAction() {
        String processId = RUNNING_PROCESS;
        operationsApiService.executeOperationAction(SPACE_GUID, processId, Action.ABORT.getActionId());
        verify(processAction)
            .execute(argThat(userInfo -> EXAMPLE_USER.equals(userInfo.getName())), eq(processId));
    }

    @Test
    void testExecuteOperationActionMissingProcess() {
        Assertions.assertThrows(NotFoundException.class, () -> operationsApiService.executeOperationAction(SPACE_GUID, "notavalidpprocess",
                                                                                                           Action.ABORT.getActionId()));
    }

    @Test
    void testExecuteOperationActionInvalidAction() {
        assertThrows(IllegalArgumentException.class,
                     () -> operationsApiService.executeOperationAction(SPACE_GUID, RUNNING_PROCESS, Action.START.getActionId()));
    }

    @Test
    void testExecuteOperationActionUnauthorized() {
        mockClientProvider(false);
        Assertions.assertThrows(ResponseStatusException.class,
                                () -> operationsApiService.executeOperationAction(SPACE_GUID, RUNNING_PROCESS, Action.ABORT.getActionId()));
    }

    @Test
    void testStartOperation() {
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test");
        Operation operation = createOperation(null, null, parameters);
        when(operationsHelper.getProcessDefinitionKey(operation))
            .thenReturn("deploy");
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL())
            .thenReturn(new StringBuffer("test/api/path"));
        operationsApiService.startOperation(SPACE_GUID, operation, httpServletRequestMock);
        verify(flowableFacade)
            .startProcess(any(), anyMap());
    }

    @Test
    void testStartOperationWhenRateLimitAllowsStartsProcess() {
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test");
        Operation operation = createOperation(null, null, parameters);
        when(operationsHelper.getProcessDefinitionKey(operation))
            .thenReturn("deploy");
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL())
            .thenReturn(new StringBuffer("test/api/path"));

        ResponseEntity<Operation> response = operationsApiService.startOperation(SPACE_GUID, operation, httpServletRequestMock);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(operationRateLimiter)
            .checkStartAllowed(EXAMPLE_USER, SPACE_GUID);
        verify(flowableFacade)
            .startProcess(any(), anyMap());
    }

    @Test
    void testStartOperationWhenRateLimitExceededReturnsTooManyRequests() {
        long retryAfterSeconds = 42;
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test");
        Operation operation = createOperation(null, null, parameters);
        doThrow(new OperationRateLimitExceededException("Operation rate limit exceeded", retryAfterSeconds))
            .when(operationRateLimiter)
            .checkStartAllowed(EXAMPLE_USER, SPACE_GUID);
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);

        ResponseEntity<Operation> response = operationsApiService.startOperation(SPACE_GUID, operation, httpServletRequestMock);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(String.valueOf(retryAfterSeconds), response.getHeaders()
                                                                .getFirst(HttpHeaders.RETRY_AFTER));
        verify(flowableFacade, never())
            .startProcess(any(), anyMap());
    }

    @Test
    void testStartOperationLogsUserGuidAndOriginButNotUsername() {
        String processInstanceId = "process-instance-id-1";
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId())
            .thenReturn(processInstanceId);
        when(flowableFacade.startProcess(any(), anyMap()))
            .thenReturn(processInstance);

        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test");
        Operation operation = createOperation(null, null, parameters);
        when(operationsHelper.getProcessDefinitionKey(operation))
            .thenReturn("deploy");
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL())
            .thenReturn(new StringBuffer("test/api/path"));

        OperationsApiServiceImpl operationsApiServiceSpy = spy(operationsApiService);
        operationsApiServiceSpy.startOperation(SPACE_GUID, operation, httpServletRequestMock);

        ArgumentCaptor<UserInfo> userInfoCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(operationsApiServiceSpy)
            .logStartOperation(eq(processInstanceId), userInfoCaptor.capture());
        UserInfo authenticatedUser = userInfoCaptor.getValue();
        assertEquals(USER_GUID, authenticatedUser.getId(), "logStartOperation must receive the user GUID");
        assertEquals("test-origin", authenticatedUser.getToken()
                                                     .getAdditionalInfo()
                                                     .get("origin"),
                     "logStartOperation must receive a UserInfo whose token has the origin");
        Assertions.assertNotEquals(EXAMPLE_USER, authenticatedUser.getId(),
                                   "logStartOperation must receive the user GUID and not the their username");
    }

    @Test
    void testStartOperationWithInvalidParametersForTheProcess() {
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test", Variables.EXT_DESCRIPTOR_FILE_ID.getName(), "ext_test",
                                                Variables.CTS_PROCESS_ID.getName(), "cts_test", Variables.DEPLOY_URI.getName(),
                                                "deploy_test");
        Operation operation = createOperation(null, null, parameters);
        when(operationsHelper.getProcessDefinitionKey(operation))
            .thenReturn("deploy");

        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL())
            .thenReturn(new StringBuffer("test/api/path"));
        operationsApiService.startOperation(SPACE_GUID, operation, httpServletRequestMock);

        verify(flowableFacade)
            .startProcess(eq("deploy"), argThat(
                map -> map.containsKey(Variables.MTA_ID.getName()) && map.containsKey(Variables.EXT_DESCRIPTOR_FILE_ID.getName())
                    && !map.containsKey(Variables.CTS_PROCESS_ID.getName()) && !map.containsKey(Variables.CTS_PASSWORD.getName())));
    }

    @Test
    void testStartOperationWithValidParametersForTheProcess() {
        Map<String, Object> parameters = Map.of(Variables.MTA_ID.getName(), "test", Variables.EXT_DESCRIPTOR_FILE_ID.getName(), "ext_test",
                                                Variables.NO_START.getName(), false, Variables.MTA_NAMESPACE.getName(), "namespace_test");
        Operation operation = createOperation(null, null, parameters);
        when(operationsHelper.getProcessDefinitionKey(operation))
            .thenReturn("deploy");
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL())
            .thenReturn(new StringBuffer("test/api/path"));
        operationsApiService.startOperation(SPACE_GUID, operation, httpServletRequestMock);

        verify(flowableFacade)
            .startProcess(eq("deploy"), argThat(
                map -> map.containsKey(Variables.MTA_ID.getName()) && map.containsKey(Variables.EXT_DESCRIPTOR_FILE_ID.getName())
                    && map.containsKey(Variables.NO_START.getName()) && map.containsKey(Variables.MTA_NAMESPACE.getName())));
    }

    @Test
    void testGetOperationLogs() throws Exception {
        String processId = FINISHED_PROCESS;
        operationsApiService.getOperationLogs(SPACE_GUID, processId);
        verify(logsService)
            .getLogNames(eq(SPACE_GUID), eq(processId));
    }

    @Test
    void testGetOperationLogsNotFoundOperation() {
        assertThrows(NotFoundException.class, () -> operationsApiService.getOperationLogs(SPACE_GUID, "notarealop"));
    }

    @Test
    void testGetOperationLogsServiceException() throws Exception {
        String processId = FINISHED_PROCESS;
        when(logsService.getLogNames(eq(SPACE_GUID), eq(processId)))
            .thenThrow(new FileStorageException("something went wrong"));
        Assertions.assertThrows(ContentException.class, () -> operationsApiService.getOperationLogs(SPACE_GUID, processId));
    }

    @Test
    void testGetOperationLogContent() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "OPERATION.log";
        String expectedLogContent = "somelogcontentstring\n1234";
        when(logsService.getOperationLog(eq(SPACE_GUID), eq(processId), eq(logName)))
            .thenReturn(expectedLogContent);
        ResponseEntity<String> response = operationsApiService.getOperationLogContent(SPACE_GUID, processId, logName);
        String logContent = response.getBody();
        assertEquals(expectedLogContent, logContent);
    }

    @Test
    void testGetOperationLogContentNotFound() throws Exception {
        String processId = FINISHED_PROCESS;
        String logName = "OPERATION.log";
        when(logsService.getOperationLog(eq(SPACE_GUID), eq(processId), eq(logName)))
            .thenThrow(new NoResultException("log file not found"));
        Assertions.assertThrows(NoResultException.class, () -> operationsApiService.getOperationLogContent(SPACE_GUID, processId, logName));
    }

    @Test
    void testGetOperationActionsForRunning() {
        ResponseEntity<List<String>> response = operationsApiService.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(Action.ABORT.getActionId()), actions);
    }

    @Test
    void testGetOperationActionsForFinished() {
        ResponseEntity<List<String>> response = operationsApiService.getOperationActions(SPACE_GUID, FINISHED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    void testGetOperationActionsForAborted() {
        ResponseEntity<List<String>> response = operationsApiService.getOperationActions(SPACE_GUID, ABORTED_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.emptyList(), actions);
    }

    @Test
    void testGetOperationActionsForError() {
        ResponseEntity<List<String>> response = operationsApiService.getOperationActions(SPACE_GUID, ERROR_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(List.of(Action.ABORT.getActionId(), Action.RETRY.getActionId()), actions);
    }

    @Test
    void testGetOperationActionsOperationNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> operationsApiService.getOperationActions(SPACE_GUID, "notarealprocess"));
    }

    @Test
    void testGetOperationActionsNotFound() {
        ResponseEntity<List<String>> response = operationsApiService.getOperationActions(SPACE_GUID, RUNNING_PROCESS);
        List<String> actions = response.getBody();
        assertEquals(Collections.singletonList(Action.ABORT.getActionId()), actions);
    }

    private void mockFlowableFacade() {
        when(flowableFacade.startProcess(any(), anyMap())).thenReturn(mock(ProcessInstance.class));
    }

    private void mockClientProvider(boolean shouldReturnAuthorizedClient) {
        mockClientAuth(shouldReturnAuthorizedClient);
        CloudSpaceClient mockedClient = mockClient();
        when(clientFactory.createSpaceClient(any())).thenReturn(mockedClient);
    }

    private void mockClientAuth(boolean shouldReturnAuthorizedClient) {
        org.springframework.security.core.context.SecurityContext securityContextMock = mock(
            org.springframework.security.core.context.SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        if (shouldReturnAuthorizedClient) {
            UserInfo userInfo = new UserInfo(USER_GUID, EXAMPLE_USER, new OAuth2AccessTokenWithAdditionalInfo(null,
                                                                                                              Map.of(
                                                                                                                  TokenProperties.USER_ID_KEY,
                                                                                                                  USER_GUID, "origin",
                                                                                                                  "test-origin")));
            OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
            Map<String, Object> attributes = Map.of(USER_INFO, userInfo);
            OAuth2User principal = mock(OAuth2User.class);
            when(principal.getAttributes())
                .thenReturn(attributes);
            when(auth.getPrincipal())
                .thenReturn(principal);
            when(securityContextMock.getAuthentication())
                .thenReturn(auth);
            return;
        }
        when(securityContextMock.getAuthentication())
            .thenReturn(null);
    }

    private CloudSpaceClient mockClient() {
        CloudSpaceClient client = mock(CloudSpaceClient.class);
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
        when(client.getSpace(any()))
            .thenReturn(space);
        return client;
    }

    private void mockProcessActionRegistry() {
        when(processActionRegistry.getAction(any()))
            .thenReturn(processAction);
    }

    private HttpServletRequest mockHttpServletRequest(String user) {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        if (user != null) {
            Principal principalMock = mock(Principal.class);
            when(principalMock.getName())
                .thenReturn(user);
            when(requestMock.getUserPrincipal())
                .thenReturn(principalMock);
        }
        return requestMock;
    }

    @SuppressWarnings("unchecked")
    private void setupOperationServiceMock() {
        when(operationService.createQuery())
            .thenReturn(operationQuery);

        doAnswer(invocation -> {
            processId = (String) invocation.getArguments()[0];
            return operationQuery;
        })
            .when(operationQuery)
            .processId(anyString());
        doAnswer(invocation -> {
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

        doAnswer(invocation -> {
            operationStatesToFilter = (List<Operation.State>) invocation.getArguments()[0];
            return operationQuery;
        })
            .when(operationQuery)
            .withStateAnyOf(anyList());
        doAnswer(invocation -> operations.stream()
                                         .filter(operation -> operationStatesToFilter == null || operationStatesToFilter.contains(
                                             operation.getState()))
                                         .collect(Collectors.toList()))
            .when(operationQuery)
            .list();
    }

    @SuppressWarnings("unchecked")
    private void setupOperationsHelperMock() {
        when(operationsHelper.addErrorType(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationsHelper.releaseLockIfNeeded(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationsHelper.releaseLocksIfNeeded(any()))
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
