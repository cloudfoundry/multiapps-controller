package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.flowable.engine.runtime.ProcessInstance;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
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
import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Message;
import com.sap.cloud.lm.sl.cf.web.api.model.MessageType;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterTypeFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
@RequestScoped
public class OperationsApiServiceImpl implements OperationsApiService {

    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private OperationDao dao;
    @Inject
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    @Inject
    private ProcessLogsPersistenceService logsService;
    @Inject
    private FlowableFacade flowableFacade;
    @Inject
    private OperationsHelper operationsHelper;
    @Inject
    private ProgressMessageDao progressMessageDao;
    @Inject
    private ProcessActionRegistry processActionRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsApiServiceImpl.class);

    @Override
    public Response getMtaOperations(Integer last, List<String> state, SecurityContext securityContext, String spaceGuid) {
        List<Operation> existingOperations = getOperations(last, state, spaceGuid);
        return Response.ok()
            .entity(existingOperations)
            .build();
    }

    public List<Operation> getOperations(Integer last, List<String> statusList, String spaceGuid) {
        List<State> states = getStates(statusList);
        return filterByQueryParameters(last, states, spaceGuid);
    }

    @Override
    public Response executeOperationAction(String operationId, String actionId, SecurityContext securityContext, String spaceGuid) {
        Operation operation = dao.findRequired(operationId);
        List<String> availableOperations = getAvailableActions(operation);
        if (!availableOperations.contains(actionId)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("Action " + actionId + " cannot be executed over operation " + operationId)
                .build();
        }
        ProcessAction action = processActionRegistry.getAction(actionId);
        action.execute(getAuthenticatedUser(securityContext), operationId);
        AuditLoggingProvider.getFacade()
            .logAboutToStart(MessageFormat.format("{0} over operation with id {1}", action, operation.getProcessId()));
        return Response.accepted()
            .header("Location", getLocationHeader(operationId, spaceGuid))
            .build();
    }

    @Override
    public Response getMtaOperationLogs(String operationId, SecurityContext securityContext, String spaceGuid) {
        try {
            Operation operation = dao.find(operationId);
            if (operation == null) {
                return Response.status(Status.NOT_FOUND)
                    .entity("Operation with id " + operationId + " not found")
                    .build();
            }
            List<String> logIds = logsService.getLogNames(spaceGuid, operationId);
            List<Log> logs = logIds.stream()
                .map(id -> new Log().id(id))
                .collect(Collectors.toList());
            return Response.ok()
                .entity(logs)
                .build();
        } catch (FileStorageException e) {
            throw new ContentException(e);
        }
    }

    @Override
    public Response getMtaOperationLogContent(String operationId, String logId, SecurityContext securityContext, String spaceGuid) {
        try {
            String content = logsService.getLogContent(spaceGuid, operationId, logId);
            return Response.ok()
                .entity(content)
                .build();
        } catch (FileStorageException e) {
            throw new ContentException(e);
        }
    }

    @Override
    public Response startMtaOperation(Operation operation, SecurityContext securityContext, String spaceGuid) {
        String userId = getAuthenticatedUser(securityContext);
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(operation);
        Set<ParameterMetadata> predefinedParameters = operationMetadataMapper.getOperationMetadata(operation.getProcessType())
            .getParameters();
        addServiceParameters(operation, spaceGuid);
        addDefaultParameters(operation, predefinedParameters);
        addParameterValues(operation, predefinedParameters);
        ensureRequiredParametersSet(operation, predefinedParameters);
        ProcessInstance processInstance = flowableFacade.startProcess(userId, processDefinitionKey, operation.getParameters());
        AuditLoggingProvider.getFacade()
            .logConfigCreate(operation);
        return Response.accepted()
            .header("Location", getLocationHeader(processInstance.getProcessInstanceId(), spaceGuid))
            .build();
    }

    @Override
    public Response getMtaOperation(String operationId, String embed, SecurityContext securityContext, String spaceGuid) {
        Operation operation = getOperation(operationId, embed, spaceGuid);
        return Response.ok()
            .entity(operation)
            .build();
    }

    public Operation getOperation(String operationId, String embed, String spaceId) {
        Operation operation = dao.findRequired(operationId);
        if (!operation.getSpaceId()
            .equals(spaceId)) {
            LOGGER.info(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.OPERATION_SPACE_MISMATCH, operationId,
                operation.getSpaceId(), spaceId));
            throw new NotFoundException(com.sap.cloud.lm.sl.cf.core.message.Messages.OPERATION_NOT_FOUND, operationId);
        }
        operationsHelper.addState(operation);
        if ("messages".equals(embed)) {
            operation.setMessages(getOperationMessages(operation));
        }
        return operation;
    }

    private List<State> getStates(List<String> statusList) {
        return statusList.stream()
            .map(State::valueOf)
            .collect(Collectors.toList());
    }

    private List<Operation> filterByQueryParameters(Integer lastRequestedOperationsCount, List<State> statusList, String spaceGuid) {
        OperationFilter operationFilter = buildOperationFilter(spaceGuid, statusList, lastRequestedOperationsCount);
        return operationsHelper.findOperations(operationFilter, statusList);
    }

    private OperationFilter buildOperationFilter(String spaceGuid, List<State> statusList, Integer lastRequestedOperationsCount) {
        OperationFilter.Builder builder = new OperationFilter.Builder();
        builder.spaceId(spaceGuid);
        if (!CollectionUtils.isEmpty(statusList) && containsOnlyFinishedStates(statusList)) {
            builder.stateIn(statusList);
        }
        builder.orderByStartTime();
        if (lastRequestedOperationsCount != null) {
            builder.maxResults(lastRequestedOperationsCount);
            builder.descending();
        }
        return builder.build();
    }

    private boolean containsOnlyFinishedStates(List<State> statusList) {
        return Collections.disjoint(statusList, State.getActiveStates());
    }

    @Override
    public Response getOperationActions(String operationId, SecurityContext securityContext, String spaceGuid) {
        Operation operation = dao.findRequired(operationId);
        return Response.ok()
            .entity(getAvailableActions(operation))
            .build();
    }

    private List<String> getAvailableActions(Operation operation) {
        State operationState = operation.getState() != null ? operation.getState() : operationsHelper.computeState(operation);
        switch (operationState) {
            case FINISHED:
            case ABORTED:
                return Collections.emptyList();
            case ERROR:
                return new ArrayList<>(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, RetryProcessAction.ACTION_ID_RETRY));
            case RUNNING:
                return new ArrayList<>(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT));
            case ACTION_REQUIRED:
                return new ArrayList<>(Arrays.asList(AbortProcessAction.ACTION_ID_ABORT, ResumeProcessAction.ACTION_ID_RESUME));
        }
        throw new IllegalStateException("State " + operationState.name() + " not recognised!");
    }

    private void addServiceParameters(Operation operation, String spaceGuid) {
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(operation);
        Map<String, Object> parameters = operation.getParameters();
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        CloudSpace space = client.getSpace(UUID.fromString(spaceGuid));
        parameters.put(Constants.VARIABLE_NAME_SPACE_ID, spaceGuid);
        parameters.put(Constants.VARIABLE_NAME_SERVICE_ID, processDefinitionKey);
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_ORG, space.getOrganization()
            .getName());
        parameters.put(com.sap.cloud.lm.sl.cf.process.Constants.VAR_SPACE, space.getName());
    }

    private void addDefaultParameters(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> parameters = operation.getParameters();
        for (ParameterMetadata predefinedParameter : predefinedParameters) {
            if (!parameters.containsKey(predefinedParameter.getId()) && predefinedParameter.getDefaultValue() != null) {
                parameters.put(predefinedParameter.getId(), predefinedParameter.getDefaultValue());
            }
        }
    }

    private void addParameterValues(Operation operation, Set<ParameterMetadata> predefinedParameters) {
        Map<String, Object> parameters = operation.getParameters();
        ParameterTypeFactory parameterTypeFactory = new ParameterTypeFactory(parameters, predefinedParameters);
        parameters.putAll(parameterTypeFactory.getParametersValues());
        operation.setParameters(parameters);
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
        StringBuilder builder = new StringBuilder("spaces/");
        return builder.append(spaceId)
            .append("/operations/")
            .append(processInstanceId)
            .append("?embed=messages")
            .toString();
    }

    protected String getAuthenticatedUser(SecurityContext securityContext) {
        String user = null;
        if (securityContext.getUserPrincipal() != null) {
            user = securityContext.getUserPrincipal()
                .getName();
        } else {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        LOGGER.debug("Authenticated user is: " + user);
        return user;
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
    }

    private List<Message> getOperationMessages(Operation operation) {
        List<ProgressMessage> progressMessages = progressMessageDao.find(operation.getProcessId());
        return progressMessages.stream()
            .filter(message -> message.getType() != ProgressMessageType.TASK_STARTUP)
            .map(this::getMessage)
            .collect(Collectors.toList());
    }

    private Message getMessage(ProgressMessage progressMessage) {
        Message message = new Message();
        message.setId(progressMessage.getId());
        message.setText(progressMessage.getText());
        message.setType(getMessageType(progressMessage.getType()));
        return message;

    }

    private MessageType getMessageType(ProgressMessageType type) {
        return MessageType.fromValue(type.toString());
    }
}
