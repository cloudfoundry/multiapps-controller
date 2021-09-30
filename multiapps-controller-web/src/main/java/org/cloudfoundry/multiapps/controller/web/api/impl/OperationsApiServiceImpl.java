package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.security.Principal;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.api.OperationsApiService;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableLog;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMessage;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Log;
import org.cloudfoundry.multiapps.controller.api.model.Message;
import org.cloudfoundry.multiapps.controller.api.model.MessageType;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConversion;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
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
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

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
    public ResponseEntity<List<Operation>> getOperations(String spaceGuid, String mtaId, List<String> stateStrings, Integer last) {
        List<Operation.State> states = getStates(stateStrings);
        List<Operation> operations = filterByQueryParameters(last, states, spaceGuid, mtaId);
        return ResponseEntity.ok()
                             .body(operations);
    }

    @Override
    public ResponseEntity<Void> executeOperationAction(HttpServletRequest request, String spaceGuid, String operationId, String actionId) {
        Operation operation = getOperationByOperationGuidAndSpaceGuid(operationId, spaceGuid);
        List<String> availableOperations = getAvailableActions(operation);
        if (!availableOperations.contains(actionId)) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.ACTION_0_CANNOT_BE_EXECUTED_OVER_OPERATION_1_IN_STATE_2,
                                                                    actionId, operationId, operation.getState()));
        }
        ProcessAction action = processActionRegistry.getAction(Action.fromString(actionId));
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
            getOperationByOperationGuidAndSpaceGuid(operationId, spaceGuid);
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
        Operation operation = getOperationByOperationGuidAndSpaceGuid(operationId, spaceGuid);
        if (!operation.getSpaceId()
                      .equals(spaceGuid)) {
            LOGGER.info(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.OPERATION_SPACE_MISMATCH, operationId,
                                             operation.getSpaceId(), spaceGuid));
            throw new NotFoundException(org.cloudfoundry.multiapps.controller.persistence.Messages.OPERATION_NOT_FOUND, operationId);
        }
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
        return ListUtils.emptyIfNull(statusList)
                        .stream()
                        .map(Operation.State::fromValue)
                        .collect(Collectors.toList());
    }

    private List<Operation> filterByQueryParameters(Integer lastRequestedOperationsCount, List<Operation.State> states, String spaceGuid,
                                                    String mtaId) {
        OperationQuery operationQuery = operationService.createQuery()
                                                        .orderByStartTime(OrderDirection.ASCENDING)
                                                        .spaceId(spaceGuid);
        if (mtaId != null) {
            operationQuery.mtaId(mtaId);
        }
        if (lastRequestedOperationsCount != null) {
            operationQuery.limitOnSelect(lastRequestedOperationsCount)
                          .orderByStartTime(OrderDirection.DESCENDING);
        }
        if (!states.isEmpty()) {
            operationQuery.withStateAnyOf(states);
        }
        return operationsHelper.releaseLocksIfNeeded(operationQuery.list());
    }

    @Override
    public ResponseEntity<List<String>> getOperationActions(String spaceGuid, String operationId) {
        Operation operation = getOperationByOperationGuidAndSpaceGuid(operationId, spaceGuid);
        return ResponseEntity.ok()
                             .body(getAvailableActions(operation));
    }

    private Operation getOperationByOperationGuidAndSpaceGuid(String operationId, String spaceGuid) {
        try {
            Operation operation = operationService.createQuery()
                                                  .processId(operationId)
                                                  .spaceId(spaceGuid)
                                                  .singleResult();
            operation = operationsHelper.addErrorType(operation);
            return operationsHelper.releaseLockIfNeeded(operation);
        } catch (NoResultException e) {
            throw new NotFoundException(e, Messages.OPERATION_0_NOT_FOUND, operationId);
        }
    }

    private List<String> getAvailableActions(Operation operation) {
        switch (operation.getState()) {
            case FINISHED:
            case ABORTED:
                return Collections.emptyList();
            case ERROR:
                return Arrays.asList(Action.ABORT.getActionId(), Action.RETRY.getActionId());
            case RUNNING:
                return Collections.singletonList(Action.ABORT.getActionId());
            case ACTION_REQUIRED:
                return Arrays.asList(Action.ABORT.getActionId(), Action.RESUME.getActionId());
        }
        throw new IllegalStateException(MessageFormat.format("State \"{0}\" not recognized!", operation.getState()));
    }

    private Operation addServiceParameters(Operation operation, String spaceGuid, String user) {
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(operation);
        Map<String, Object> parameters = new HashMap<>(operation.getParameters());
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        CloudSpace space = client.getSpace(UUID.fromString(spaceGuid));
        CloudOrganization organization = space.getOrganization();
        parameters.put(Constants.VARIABLE_NAME_SERVICE_ID, processDefinitionKey);
        parameters.put(Variables.USER.getName(), user);
        parameters.put(Variables.SPACE_NAME.getName(), space.getName());
        parameters.put(Variables.SPACE_GUID.getName(), spaceGuid);
        parameters.put(Variables.ORGANIZATION_NAME.getName(), organization.getName());
        parameters.put(Variables.ORGANIZATION_GUID.getName(), organization.getMetadata()
                                                                          .getGuid()
                                                                          .toString());
        String namespace = operation.getNamespace();
        if (namespace != null) {
            parameters.put(Variables.MTA_NAMESPACE.getName(), namespace);
            parameters.put(Variables.APPLY_NAMESPACE.getName(), true);
        }

        return ImmutableOperation.copyOf(operation)
                                 .withParameters(parameters);
    }

    private Operation addParameterValues(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> parameters = new HashMap<>(operation.getParameters());
        parameters.putAll(ParameterConversion.toFlowableVariables(predefinedParameters, parameters));
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
        if (request.getUserPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Principal principal = request.getUserPrincipal();
        String username = SecurityContextUtil.getUsername(principal);
        LOGGER.debug(MessageFormat.format("Authenticated user is: {0}", username));
        return username;
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClientWithNoCorrelation(userInfo.getName(), spaceGuid);
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
                               .timestamp(toZonedDateTime(progressMessage.getTimestamp()))
                               .type(getMessageType(progressMessage.getType()))
                               .build();

    }

    private ZonedDateTime toZonedDateTime(Date timestamp) {
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
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
