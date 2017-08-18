package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.OngoingOperationDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.OngoingOperationsDto;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.OperationsBean;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.core.util.AuthorizationUtil;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToServiceMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiProcess;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiService;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiServiceFactory;
import com.sap.lmsl.slp.SlpTaskState;

@Component
@Path("/{org}/{space}/operations")
@Produces(MediaType.APPLICATION_XML)
public class OngoingOperationsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OngoingOperationsResource.class);

    @PathParam("org")
    private String organization;

    @PathParam("space")
    private String space;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Inject
    private OngoingOperationDao dao;

    @Inject
    private ActivitiServiceFactory activitiServiceFactory;

    @GET
    public OngoingOperationsDto getOngoingOperations(@BeanParam OperationsBean operationsBean) throws SLException {
        Integer lastRequestedOperationsCount = operationsBean.getLastRequestedOperationsCount();
        List<String> statusList = operationsBean.getStatusList();

        List<OngoingOperation> foundOngoingOperations = filterByQueryParameters(lastRequestedOperationsCount, statusList);

        List<OngoingOperation> existingOngoingOperations = filterExistingOngoingOperations(foundOngoingOperations);

        addOngoingOperationsState(existingOngoingOperations);

        return new OngoingOperationsDto(wrap(existingOngoingOperations));
    }

    private void addOngoingOperationsState(List<OngoingOperation> existingOngoingOperations) {
        for (OngoingOperation ongoingOperation : existingOngoingOperations) {
            addState(ongoingOperation);
        }
    }

    private List<OngoingOperation> filterByQueryParameters(Integer lastRequestedOperationsCount, List<String> statusList) {
        if (lastRequestedOperationsCount == null && statusList.isEmpty()) {
            return getAllOperations();
        }

        if (lastRequestedOperationsCount != null && statusList.isEmpty()) {
            return getLastOperations(lastRequestedOperationsCount);
        }

        if (lastRequestedOperationsCount == null && !statusList.isEmpty()) {
            return getOperationsByStatus(statusList);
        }

        if (lastRequestedOperationsCount != null && !statusList.isEmpty()) {
            return getLastOperationsFilteredByStatus(lastRequestedOperationsCount, getOperationsByStatus(statusList));
        }

        return Collections.emptyList();
    }

    private List<OngoingOperation> getLastOperations(Integer lastOperationsCount) throws SLException {
        return dao.findLastOperations(lastOperationsCount, getSpaceId());
    }

    private List<OngoingOperation> getLastOperationsFilteredByStatus(int lastRequestedOperationsCount,
        List<OngoingOperation> operationsByStatus) {
        int operationsByStatusSize = operationsByStatus.size();
        if (lastRequestedOperationsCount > operationsByStatusSize) {
            return operationsByStatus;
        }
        if (lastRequestedOperationsCount < 0) {
            return Collections.emptyList();
        }

        return operationsByStatus.subList(operationsByStatusSize - lastRequestedOperationsCount, operationsByStatusSize);
    }

    private List<OngoingOperation> getOperationsByStatus(List<String> statusList) throws SLException {
        if (statusList.isEmpty()) {
            return Collections.emptyList();
        }
        return dao.findOperationsByStatus(getSlpTaskStateList(statusList), getSpaceId());
    }

    private List<SlpTaskState> getSlpTaskStateList(List<String> statusList) {
        return statusList.stream().map(status -> SlpTaskState.valueOf(status)).collect(Collectors.toList());
    }

    private List<OngoingOperation> getAllOperations() throws SLException {
        return dao.findAllInSpace(getSpaceId());
    }

    private List<OngoingOperationDto> wrap(List<OngoingOperation> ongoingOperations) throws SLException {
        return ongoingOperations.stream().map(operation -> toOngoingOperationDto(operation)).collect(Collectors.toList());
    }

    private List<OngoingOperation> filterExistingOngoingOperations(List<OngoingOperation> ongoingOperations) {
        return ongoingOperations.stream().filter(operation -> getProcessForOperation(operation) != null).collect(Collectors.toList());
    }

    @GET
    @Path("/{processId}")
    public OngoingOperationDto getOngoingOperation(@PathParam("processId") String processId) throws SLException {
        String spaceId = getSpaceIdByProcessId(processId);
        OngoingOperation ongoingOperation = dao.findRequired(processId);
        if (getProcessForOperation(ongoingOperation) != null && ongoingOperation.getSpaceId().equals(spaceId)) {
            addState(ongoingOperation);
            return toOngoingOperationDto(ongoingOperation);
        }
        throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId, spaceId);
    }

    protected void addState(OngoingOperation ongoingOperation) throws SLException {
        SlpTaskState state = getOngoingOperationState(ongoingOperation);
        ongoingOperation.setFinalState(state);
    }

    protected OngoingOperationDto toOngoingOperationDto(OngoingOperation ongoingOperation) throws SLException {
        return new OngoingOperationDto(ongoingOperation.getProcessId(), ongoingOperation.getProcessType(), ongoingOperation.getStartedAt(),
            ongoingOperation.getSpaceId(), ongoingOperation.getMtaId(), ongoingOperation.getUser(),
            ongoingOperation.getFinalState().toString(), ongoingOperation.hasAcquiredLock());
    }

    protected SlpTaskState getOngoingOperationState(OngoingOperation ongoingOperation) throws SLException {
        if (ongoingOperation.getFinalState() != null) {
            return ongoingOperation.getFinalState();
        }
        SlpTaskState state = computeState(ongoingOperation);
        // Fixes bug XSBUG-2035: Inconsistency in 'ongoing_operations', 'act_hi_procinst' and 'act_ru_execution' tables
        if (ongoingOperation.hasAcquiredLock()
            && (state.equals(SlpTaskState.SLP_TASK_STATE_ABORTED) || state.equals(SlpTaskState.SLP_TASK_STATE_FINISHED))) {
            ongoingOperation.setHasAcquiredLock(false);
            ongoingOperation.setFinalState(state);
            this.dao.merge(ongoingOperation);
        }
        return state;
    }

    protected SlpTaskState computeState(OngoingOperation ongoingOperation) throws SLException {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, ongoingOperation.getProcessType(),
            ongoingOperation.getProcessId()));
        return getProcessForOperation(ongoingOperation).getCurrentState();
    }

    protected ActivitiProcess getProcessForOperation(OngoingOperation ongoingOperation) throws SLException {
        ActivitiService activitiService = getActivitiService(ongoingOperation.getProcessType());
        return activitiService.getActivitiProcess(ongoingOperation.getSpaceId(), ongoingOperation.getProcessId());
    }

    private ActivitiService getActivitiService(ProcessType processType) throws SLException {
        return activitiServiceFactory.createActivitiService(ProcessTypeToServiceMetadataMapper.getServiceMetadata(processType));
    }

    protected String getSpaceId() throws SLException {
        return AuthorizationUtil.getSpaceId(clientProvider, SecurityContextUtil.getUserInfo(), organization, space, null);
    }

    protected String getSpaceIdByProcessId(String processId) throws SLException {
        return AuthorizationUtil.getProcessSpaceId(processId, clientProvider, SecurityContextUtil.getUserInfo(), organization, space);
    }

}
