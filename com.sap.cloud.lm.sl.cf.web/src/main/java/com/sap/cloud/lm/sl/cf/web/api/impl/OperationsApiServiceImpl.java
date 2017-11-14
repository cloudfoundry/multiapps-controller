package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.api.activiti.ActivitiAction;
import com.sap.cloud.lm.sl.cf.api.activiti.ActivitiActionFactory;
import com.sap.cloud.lm.sl.cf.api.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedSpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.api.OperationsApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Message;
import com.sap.cloud.lm.sl.cf.web.api.model.MessageType;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterTypeFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

@RequestScoped
@Component
public class OperationsApiServiceImpl implements OperationsApiService {

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Inject
    private OperationDao dao;

    @Inject
    private ProcessTypeToOperationMetadataMapper metadataMapper;

    @Inject
    private ProcessLogsService logsService;

    @Inject
    private ActivitiFacade activitiFacade;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsApiServiceImpl.class);

    @Override
    public Response getMtaOperations(Integer last, List<String> state, SecurityContext securityContext, String spaceGuid) {
        List<Operation> existingOperations = getOperations(last, state, spaceGuid);
        return Response.ok().entity(existingOperations).build();
    }

    public List<Operation> getOperations(Integer last, List<String> statusList, String spaceGuid) {
        List<State> states = getStates(statusList);
        List<Operation> foundOperations = filterByQueryParameters(last, states, spaceGuid);
        List<Operation> existingOperations = filterExistingOngoingOperations(foundOperations);
        addOngoingOperationsState(existingOperations);
        return existingOperations;
    }

    @Override
    public Response executeOperationAction(String operationId, String actionId, SecurityContext securityContext, String spaceGuid) {
        Operation operation = dao.findRequired(operationId);
        List<String> availableOperations = getAvailableActions(operation);
        ActivitiAction action = ActivitiActionFactory.getAction(actionId, activitiFacade, getAuthenticatedUser(securityContext));
        if (!availableOperations.contains(actionId)) {
            return Response.status(Status.BAD_REQUEST).entity(
                "Action " + actionId + " cannot be executed over operation " + operationId).build();
        }
        action.executeAction(operationId);
        return Response.accepted().header("Location", getLocationHeader(operationId, spaceGuid)).build();
    }

    @Override
    public Response getMtaOperationLogs(String operationId, SecurityContext securityContext, String spaceGuid) {
        try {
            Operation operation = dao.find(operationId);
            if (operation == null) {
                return Response.status(Status.NOT_FOUND).entity("Operation with id " + operationId + " not found").build();
            }
            List<String> logIds = logsService.getLogNames(spaceGuid, operationId);
            List<Log> logs = logIds.stream().map(id -> new Log().id(id)).collect(Collectors.toList());
            return Response.ok().entity(logs).build();
        } catch (FileStorageException e) {
            throw new ContentException(e);
        }
    }

    @Override
    public Response getMtaOperationLogContent(String operationId, String logId, SecurityContext securityContext, String spaceGuid) {
        try {
            String content = logsService.getLogContent(spaceGuid, operationId, logId);
            return Response.ok().entity(content).build();
        } catch (FileStorageException e) {
            throw new ContentException(e);
        }
    }

    @Override
    public Response startMtaOperation(Operation operation, SecurityContext securityContext, String spaceGuid) {
        String userId = getAuthenticatedUser(securityContext);
        String processDefinitionKey = getProcessDefinitionKey(operation);
        addServiceParameters(operation, spaceGuid);
        addDefaultParameters(operation, spaceGuid);
        addParameterValues(operation);
        ProcessInstance processInstance = activitiFacade.startProcess(userId, processDefinitionKey, operation.getParameters());
        return Response.accepted().header("Location", getLocationHeader(processInstance.getProcessInstanceId(), spaceGuid)).build();
    }

    @Override
    public Response getMtaOperation(String operationId, String embed, SecurityContext securityContext, String spaceGuid) {
        Operation operation = getOperation(operationId, embed, spaceGuid);
        return Response.ok().entity(operation).build();
    }

    public Operation getOperation(String operationId, String embed, String spaceId) {
        Operation operation = dao.findRequired(operationId);
        if (!isProcessFound(operation) || !operation.getSpaceId().equals(spaceId)) {
            throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, operationId, spaceId);
        }
        addState(operation);
        if ("messages".equals(embed)) {
            operation.setMessages(getOperationMessages(operation));
        }
        return operation;
    }

    private List<State> getStates(List<String> statusList) {
        return statusList.stream().map(status -> State.valueOf(status)).collect(Collectors.toList());
    }

    private List<Operation> filterByQueryParameters(Integer lastRequestedOperationsCount, List<State> statusList, String spaceGuid) {
        if (lastRequestedOperationsCount == null && statusList.isEmpty()) {
            return getAllOperations(spaceGuid);
        }

        if (lastRequestedOperationsCount != null && statusList.isEmpty()) {
            return getLastOperations(lastRequestedOperationsCount, spaceGuid);
        }

        if (lastRequestedOperationsCount == null && !statusList.isEmpty()) {
            return getOperationsByStatus(statusList, spaceGuid);
        }

        if (lastRequestedOperationsCount != null && !statusList.isEmpty()) {
            return getLastOperationsFilteredByStatus(lastRequestedOperationsCount, getOperationsByStatus(statusList, spaceGuid));
        }

        return Collections.emptyList();
    }

    private List<Operation> getAllOperations(String spaceGuid) throws SLException {
        return dao.findAllInSpace(spaceGuid);
    }

    private List<Operation> getLastOperations(Integer lastOperationsCount, String spaceGuid) throws SLException {
        return dao.findLastOperations(lastOperationsCount, spaceGuid);
    }

    private List<Operation> getOperationsByStatus(List<State> statusList, String spaceGuid) throws SLException {
        if (statusList.isEmpty()) {
            return Collections.emptyList();
        }

        return dao.findOperationsByStatus(statusList, spaceGuid);
    }

    private List<Operation> getLastOperationsFilteredByStatus(int lastRequestedOperationsCount, List<Operation> operationsByStatus) {
        int operationsByStatusSize = operationsByStatus.size();
        if (lastRequestedOperationsCount > operationsByStatusSize) {
            return operationsByStatus;
        }
        if (lastRequestedOperationsCount < 0) {
            return Collections.emptyList();
        }

        return operationsByStatus.subList(operationsByStatusSize - lastRequestedOperationsCount, operationsByStatusSize);
    }

    private List<Operation> filterExistingOngoingOperations(List<Operation> ongoingOperations) {
        return ongoingOperations.stream().filter(operation -> isProcessFound(operation)).collect(Collectors.toList());
    }

    protected boolean isProcessFound(Operation ongoingOperation) throws SLException {
        String processDefinitionKey = getProcessDefinitionKey(ongoingOperation);
        HistoricProcessInstance historicInstance = getHistoricInstance(ongoingOperation, processDefinitionKey);
        return historicInstance != null;
    }

    private HistoricProcessInstance getHistoricInstance(Operation ongoingOperation, String processDefinitionKey) {
        return activitiFacade.getHistoricProcessInstanceBySpaceId(processDefinitionKey, ongoingOperation.getSpaceId(),
            ongoingOperation.getProcessId());
    }

    private String getProcessDefinitionKey(Operation ongoingOperation) {
        return metadataMapper.getOperationMetadata(ongoingOperation.getProcessType()).getActivitiProcessId();
    }

    private void addOngoingOperationsState(List<Operation> existingOngoingOperations) {
        for (Operation ongoingOperation : existingOngoingOperations) {
            addState(ongoingOperation);
        }
    }

    protected void addState(Operation ongoingOperation) throws SLException {
        ongoingOperation.setState(getOngoingOperationState(ongoingOperation));
    }

    protected State getOngoingOperationState(Operation ongoingOperation) throws SLException {
        if (ongoingOperation.getState() != null) {
            return ongoingOperation.getState();
        }
        State state = computeState(ongoingOperation);
        // Fixes bug XSBUG-2035: Inconsistency in 'ongoing_operations', 'act_hi_procinst' and 'act_ru_execution' tables
        if (ongoingOperation.isAcquiredLock() && (state.equals(State.ABORTED) || state.equals(State.FINISHED))) {
            ongoingOperation.acquiredLock(false);
            ongoingOperation.setState(state);
            this.dao.merge(ongoingOperation);
        }
        return state;
    }

    protected State computeState(Operation ongoingOperation) throws SLException {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, ongoingOperation.getProcessType(),
            ongoingOperation.getProcessId()));
        return activitiFacade.getOngoingOperationState(ongoingOperation);
    }

    @Override
    public Response getOperationActions(String operationId, SecurityContext securityContext, String spaceGuid) {
        Operation operation = dao.findRequired(operationId);
        return Response.ok().entity(getAvailableActions(operation)).build();
    }

    private List<String> getAvailableActions(Operation operation) {
        State operationState = operation.getState() != null ? operation.getState() : computeState(operation);
        switch (operationState) {
            case FINISHED:
            case ABORTED:
                return Collections.emptyList();
            case ERROR:
            case RUNNING:
                return new ArrayList<>(Arrays.asList("abort", "retry"));
            case ACTION_REQUIRED:
                return new ArrayList<>(Arrays.asList("abort", "resume"));
        }

        throw new IllegalStateException("State " + operationState.value() + " not recognised!");
    }

    private void addServiceParameters(Operation operation, String spaceGuid) {
        String processDefinitionKey = getProcessDefinitionKey(operation);
        Map<String, Object> parameters = operation.getParameters();
        CloudFoundryOperations client = getCloudFoundryClient(spaceGuid);
        CloudSpace space = new CFOptimizedSpaceGetter().getSpace(client, spaceGuid);
        parameters.put("__SPACE_ID", spaceGuid);
        parameters.put("__SERVICE_ID", processDefinitionKey);
        parameters.put("org", space.getOrganization().getName());
        parameters.put("space", space.getName());
    }

    private void addDefaultParameters(Operation operation, String spaceGuid) {
        Map<String, Object> parameters = operation.getParameters();
        Set<ParameterMetadata> serviceParameters = metadataMapper.getOperationMetadata(operation.getProcessType()).getParameters();
        for (ParameterMetadata serviceParameter : serviceParameters) {
            if (!parameters.containsKey(serviceParameter.getId()) && serviceParameter.getDefaultValue() != null) {
                parameters.put(serviceParameter.getId(), serviceParameter.getDefaultValue());
            }
        }
    }

    private void addParameterValues(Operation operation) {
        Map<String, Object> parameters = operation.getParameters();
        Set<ParameterMetadata> serviceParameters = metadataMapper.getOperationMetadata(operation.getProcessType()).getParameters();
        ParameterTypeFactory parameterTypeFactory = new ParameterTypeFactory(parameters, serviceParameters);
        parameters.putAll(parameterTypeFactory.getParametersValues());
        operation.setParameters(parameters);
    }

    private String getLocationHeader(String processInstanceId, String spaceId) {
        StringBuilder builder = new StringBuilder("spaces/");
        return builder.append(spaceId).append("/operations/").append(processInstanceId).append("?embed=messages").toString();
    }

    protected String getAuthenticatedUser(SecurityContext securityContext) {
        String user = null;
        if (securityContext.getUserPrincipal() != null) {
            user = securityContext.getUserPrincipal().getName();
        } else {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        LOGGER.debug("Authenticated user is: " + user);
        return user;
    }

    private CloudFoundryOperations getCloudFoundryClient(String spaceGuid) throws SLException {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getCloudFoundryClient(userInfo.getToken(), spaceGuid);
    }

    private List<Message> getOperationMessages(Operation operation) {
        List<ProgressMessage> progressMessages = ProgressMessageService.getInstance().findByProcessId(operation.getProcessId());
        return progressMessages.stream().filter(message -> message.getType() != ProgressMessageType.TASK_STARTUP).map(
            message -> getMessage(message)).collect(Collectors.toList());
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
