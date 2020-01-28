package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.persistence.Constants;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.ResumeProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.RetryProcessAction;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.util.OperationsHelper;
import com.sap.cloud.lm.sl.cf.web.api.OperationsApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableLog;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableMessage;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Message;
import com.sap.cloud.lm.sl.cf.web.api.model.MessageType;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterTypeFactory;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
public class OperationsApiServiceImpl implements OperationsApiService {

    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private OperationService operationService;
    @Inject
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    @Inject
    private ProcessLogsPersistenceService logsService;
    @Inject
    private FlowableFacade flowableFacade;
    @Inject
    private OperationsHelper operationsHelper;
    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private ProcessActionRegistry processActionRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsApiServiceImpl.class);

    @Override
    public ResponseEntity<List<Operation>> getOperations(String spaceGuid, List<String> stateStrings, Integer last) {
        List<Operation.State> states = getStates(stateStrings);
        List<Operation> operations = filterByQueryParameters(last, states, spaceGuid);
        return ResponseEntity.ok()
                             .body(operations);
    }

    public ResponseEntity<Void> executeOperationAction(HttpServletRequest request, String spaceGuid, String operationId, String actionId) {
        Operation operation = getOperation(operationId);
        List<String> availableOperations = getAvailableActions(operation);
        if (!availableOperations.contains(actionId)) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.ACTION_0_CANNOT_BE_EXECUTED_OVER_OPERATION_1, actionId,
                                                                    operationId));
        }
        ProcessAction action = processActionRegistry.getAction(actionId);
        action.execute(getAuthenticatedUser(request), operationId);
        AuditLoggingProvider.getFacade()
                            .logAboutToStart(MessageFormat.format("{0} over operation with id {1}", action, operation.getProcessId()));
        return ResponseEntity.accepted()
                             .header("Location", getLocationHeader(operationId, spaceGuid))
                             .build();
    }

    @Override
    public ResponseEntity<List<Log>> getOperationLogs(String spaceGuid, String operationId) {
        try {
            getOperation(operationId);
            List<String> logIds = logsService.getLogNames(spaceGuid, operationId);
            List<Log> logs = logIds.stream()
                                   .map(id -> ImmutableLog.builder()
                                                          .id(id)
                                                          .build())
                                   .collect(Collectors.toList());
            return ResponseEntity.ok()
                                 .body(logs);
        } catch (FileStorageException e) {
            throw new ContentException(e, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> getOperationLogContent(String spaceGuid, String operationId, String logId) {
        try {
            String content = logsService.getLogContent(spaceGuid, operationId, logId);
            return ResponseEntity.ok()
                                 .body(content);
        } catch (FileStorageException e) {
            throw new ContentException(e, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Operation> startOperation(HttpServletRequest request, String spaceGuid, Operation operation) {
        String user = getAuthenticatedUser(request);
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(operation);
        Set<ParameterMetadata> predefinedParameters = operationMetadataMapper.getOperationMetadata(operation.getProcessType())
                                                                             .getParameters();
        operation = addServiceParameters(operation, spaceGuid, user);
        operation = addDefaultParameters(operation, predefinedParameters);
        operation = addParameterValues(operation, predefinedParameters);
        ensureRequiredParametersSet(operation, predefinedParameters);
        ProcessInstance processInstance = flowableFacade.startProcess(processDefinitionKey, operation.getParameters());
        AuditLoggingProvider.getFacade()
                            .logConfigCreate(operation);
        return ResponseEntity.accepted()
                             .header("Location", getLocationHeader(processInstance.getProcessInstanceId(), spaceGuid))
                             .build();
    }

    @Override
    public ResponseEntity<Operation> getOperation(String spaceGuid, String operationId, String embed) {
        Operation operation = getOperation(operationId);
        if (!operation.getSpaceId()
                      .equals(spaceGuid)) {
            LOGGER.info(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.OPERATION_SPACE_MISMATCH, operationId,
                                             operation.getSpaceId(), spaceGuid));
            throw new NotFoundException(com.sap.cloud.lm.sl.cf.core.message.Messages.OPERATION_NOT_FOUND, operationId);
        }
        operation = operationsHelper.addState(operation);
        operation = operationsHelper.addErrorType(operation);
        if ("messages".equals(embed)) {
            operation = ImmutableOperation.copyOf(operation)
                                          .withMessages(getOperationMessages(operation));
            if (operation.getState() == Operation.State.ERROR && !hasErrorMessage(operation)) {
                LOGGER.error("MTA operation \"{}\" is in error state, but has no error messages.", operation.getProcessId());
            }
        }
        return ResponseEntity.ok()
                             .body(operation);
    }

    private List<Operation.State> getStates(List<String> statusList) {
        return ObjectUtils.defaultIfNull(statusList, Collections.<String> emptyList())
                          .stream()
                          .map(Operation.State::valueOf)
                          .collect(Collectors.toList());
    }

    private List<Operation> filterByQueryParameters(Integer lastRequestedOperationsCount, List<Operation.State> statusList,
                                                    String spaceGuid) {
        OperationQuery operationQuery = operationService.createQuery()
                                                        .orderByStartTime(OrderDirection.ASCENDING)
                                                        .spaceId(spaceGuid);
        if (lastRequestedOperationsCount != null) {
            operationQuery.limitOnSelect(lastRequestedOperationsCount)
                          .orderByStartTime(OrderDirection.DESCENDING);
        }
        if (CollectionUtils.isNotEmpty(statusList) && containsOnlyFinishedStates(statusList)) {
            operationQuery.withStateAnyOf(statusList);
        }
        List<Operation> operations = operationQuery.list();
        return operationsHelper.findOperations(operations, statusList);
    }

    private boolean containsOnlyFinishedStates(List<Operation.State> statusList) {
        return Collections.disjoint(statusList, Operation.State.getNonFinalStates());
    }

    @Override
    public ResponseEntity<List<String>> getOperationActions(String spaceGuid, String operationId) {
        Operation operation = getOperation(operationId);
        return ResponseEntity.ok()
                             .body(getAvailableActions(operation));
    }

    private Operation getOperation(String operationId) {
        try {
            return operationService.createQuery()
                                   .processId(operationId)
                                   .singleResult();
        } catch (NoResultException e) {
            throw new NotFoundException(e, Messages.OPERATION_0_NOT_FOUND, operationId);
        }
    }

    private List<String> getAvailableActions(Operation operation) {
        operation = operationsHelper.addState(operation);
        switch (operation.getState()) {
            case FINISHED:
            case ABORTED:
                return Collections.emptyList();
            case ERROR:
                return new ArrayList<>(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, RetryProcessAction.ACTION_ID_RETRY));
            case RUNNING:
                return new ArrayList<>(Collections.singletonList(AbortProcessAction.ACTION_ID_ABORT));
            case ACTION_REQUIRED:
                return new ArrayList<>(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, ResumeProcessAction.ACTION_ID_RESUME));
        }
        throw new IllegalStateException(MessageFormat.format("State \"{0}\" not recognized!", operation.getState()));
    }

    private Operation addServiceParameters(Operation operation, String spaceGuid, String user) {
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(operation);
        Map<String, Object> parameters = new HashMap<>(operation.getParameters());
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        CloudSpace space = client.getSpace(UUID.fromString(spaceGuid));
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_MTA_ID,
                       parameters.get(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_MTA_ID));
        parameters.put(Constants.VARIABLE_NAME_SPACE_ID, spaceGuid);
        parameters.put(Constants.VARIABLE_NAME_SERVICE_ID, processDefinitionKey);
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_ORG, space.getOrganization()
                                                                              .getName());
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_SPACE, space.getName());
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_USER, user);
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_ORG_ID, space.getOrganization()
                                                                                 .getMetadata()
                                                                                 .getGuid()
                                                                                 .toString());
        return ImmutableOperation.copyOf(operation)
                                 .withParameters(parameters);
    }

    private Operation addDefaultParameters(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> parameters = new HashMap<>(operation.getParameters());
        for (ParameterMetadata predefinedParameter : predefinedParameters) {
            if (!parameters.containsKey(predefinedParameter.getId()) && predefinedParameter.getDefaultValue() != null) {
                parameters.put(predefinedParameter.getId(), predefinedParameter.getDefaultValue());
            }
        }
        return ImmutableOperation.copyOf(operation)
                                 .withParameters(parameters);
    }

    private Operation addParameterValues(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> parameters = new HashMap<>(operation.getParameters());
        ParameterTypeFactory parameterTypeFactory = new ParameterTypeFactory(parameters, predefinedParameters);
        parameters.putAll(parameterTypeFactory.getParametersValues());
        return ImmutableOperation.copyOf(operation)
                                 .withParameters(parameters);
    }

    private void ensureRequiredParametersSet(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> operationParameters = operation.getParameters();
        Set<ParameterMetadata> requiredParameters = getRequiredParameters(predefinedParameters);
        List<ParameterMetadata> missingRequiredParameters = requiredParameters.stream()
                                                                              .filter(parameter -> !operationParameters.containsKey(parameter.getId()))
                                                                              .collect(Collectors.toList());
        if (!missingRequiredParameters.isEmpty()) {
            throw new ContentException("Required parameters " + getParameterIds(missingRequiredParameters) + " are not set!");
        }
    }

    private String getParameterIds(List<ParameterMetadata> missingRequiredParameters) {
        return missingRequiredParameters.stream()
                                        .map(ParameterMetadata::getId)
                                        .collect(Collectors.joining(","));
    }

    private Set<ParameterMetadata> getRequiredParameters(Set<ParameterMetadata> parameters) {
        return parameters.stream()
                         .filter(ParameterMetadata::getRequired)
                         .collect(Collectors.toSet());
    }

    private String getLocationHeader(String processInstanceId, String spaceId) {
        return "spaces/" + spaceId + "/operations/" + processInstanceId + "?embed=messages";
    }

    private String getAuthenticatedUser(HttpServletRequest request) {
        String user = null;
        if (request.getUserPrincipal() != null) {
            user = request.getUserPrincipal()
                          .getName();
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        LOGGER.debug(MessageFormat.format("Authenticated user is: {0}", user));
        return user;
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
    }

    private List<Message> getOperationMessages(Operation operation) {
        List<ProgressMessage> progressMessages = progressMessageService.createQuery()
                                                                       .processId(operation.getProcessId())
                                                                       .orderById(OrderDirection.ASCENDING)
                                                                       .list();
        return progressMessages.stream()
                               .map(this::getMessage)
                               .collect(Collectors.toList());
    }

    private Message getMessage(ProgressMessage progressMessage) {
        return ImmutableMessage.builder()
                               .id(progressMessage.getId())
                               .text(progressMessage.getText())
                               .type(getMessageType(progressMessage.getType()))
                               .build();

    }

    private MessageType getMessageType(ProgressMessageType type) {
        return MessageType.fromValue(type.toString());
    }

    private boolean hasErrorMessage(Operation operation) {
        return operation.getMessages()
                        .stream()
                        .anyMatch(message -> message.getType() == MessageType.ERROR);
    }

}
