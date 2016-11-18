package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.ArrayList;
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
import com.sap.cloud.lm.sl.cf.process.metadata.XS2BlueGreenDeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.XS2DeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.XS2UndeployServiceMetadata;
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

        if (lastRequestedOperationsCount == null && statusList.isEmpty()) {
            return new OngoingOperationsDto(getAllOperations());
        }

        if (lastRequestedOperationsCount != null && statusList.isEmpty()) {
            return new OngoingOperationsDto(getLastOperations(lastRequestedOperationsCount));
        }

        if (lastRequestedOperationsCount == null && !statusList.isEmpty()) {
            return new OngoingOperationsDto(getOperationsByStatus(statusList));
        }

        if (lastRequestedOperationsCount != null && !statusList.isEmpty()) {
            List<OngoingOperationDto> lastOperationsByStatus = getLastOperationsFilteredByStatus(lastRequestedOperationsCount,
                getOperationsByStatus(statusList));
            return new OngoingOperationsDto(lastOperationsByStatus);
        }

        return new OngoingOperationsDto(Collections.emptyList());
    }

    private List<OngoingOperationDto> getLastOperations(Integer lastOperationsCount) throws SLException {
        return wrap(dao.findLastOperations(lastOperationsCount, getSpaceId()));
    }

    private List<OngoingOperationDto> getLastOperationsFilteredByStatus(int lastRequestedOperationsCount,
        List<OngoingOperationDto> operationsByStatus) {
        int operationsByStatusSize = operationsByStatus.size();
        if (lastRequestedOperationsCount > operationsByStatusSize) {
            return operationsByStatus;
        }
        if (lastRequestedOperationsCount < 0) {
            return Collections.emptyList();
        }

        return operationsByStatus.subList(operationsByStatusSize - lastRequestedOperationsCount, operationsByStatusSize);
    }

    private List<OngoingOperationDto> getOperationsByStatus(List<String> statusList) throws SLException {
        if (statusList.isEmpty()) {
            return Collections.emptyList();
        }
        return wrap(dao.findOperationsByStatus(getSlpTaskStateList(statusList), getSpaceId()));
    }

    private List<SlpTaskState> getSlpTaskStateList(List<String> statusList) {
        return statusList.stream().map(status -> SlpTaskState.valueOf(status)).collect(Collectors.toList());
    }

    private List<OngoingOperationDto> wrap(List<OngoingOperation> ongoingOperations) throws SLException {
        List<OngoingOperationDto> result = new ArrayList<>();
        for (OngoingOperation ongoingOperation : ongoingOperations) {
            result.add(addState(ongoingOperation));
        }
        return result;
    }

    private List<OngoingOperationDto> getAllOperations() throws SLException {
        return wrap(dao.findAllInSpace(getSpaceId()));
    }

    @GET
    @Path("/{processId}")
    public OngoingOperationDto getOngoingOperation(@PathParam("processId") String processId) throws SLException {
        String spaceId = getSpaceIdByProcessId(processId);
        OngoingOperation ongoingOperation = dao.find(processId);
        if (ongoingOperation.getSpaceId().equals(spaceId)) {
            return addState(ongoingOperation);
        }
        throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId, spaceId);
    }

    protected OngoingOperationDto addState(OngoingOperation ongoingOperation) throws SLException {
        SlpTaskState state = getOngoingOperationState(ongoingOperation);
        return new OngoingOperationDto(ongoingOperation.getProcessId(), ongoingOperation.getProcessType(), ongoingOperation.getStartedAt(),
            ongoingOperation.getSpaceId(), ongoingOperation.getMtaId(), ongoingOperation.getUser(), state.toString(),
            ongoingOperation.hasAcquiredLock());
    }

    protected SlpTaskState getOngoingOperationState(OngoingOperation ongoingOperation) throws SLException {
        if (ongoingOperation.getFinalState() != null) {
            return ongoingOperation.getFinalState();
        } else {
            return computeState(ongoingOperation);
        }
    }

    protected SlpTaskState computeState(OngoingOperation ongoingOperation) throws SLException {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, ongoingOperation.getProcessType(),
            ongoingOperation.getProcessId()));
        ActivitiService activitiService = getActivitiService(ongoingOperation.getProcessType());
        ActivitiProcess activitiProcess = activitiService.getActivitiProcess(ongoingOperation.getSpaceId(),
            ongoingOperation.getProcessId());
        return activitiProcess.getProcessStatus();
    }

    private ActivitiService getActivitiService(ProcessType processType) throws SLException {
        switch (processType) {
            case BLUE_GREEN_DEPLOY:
                return activitiServiceFactory.createActivitiService(new XS2BlueGreenDeployServiceMetadata());
            case UNDEPLOY:
                return activitiServiceFactory.createActivitiService(new XS2UndeployServiceMetadata());
            case DEPLOY:
                return activitiServiceFactory.createActivitiService(new XS2DeployServiceMetadata());
            default:
                throw new SLException(Messages.UNSUPPORTED_PROCESS_TYPE, processType.toString());
        }
    }

    protected String getSpaceId() throws SLException {
        return AuthorizationUtil.getSpaceId(clientProvider, SecurityContextUtil.getUserInfo(), organization, space, null);
    }

    protected String getSpaceIdByProcessId(String processId) throws SLException {
        return AuthorizationUtil.getProcessSpaceId(processId, clientProvider, SecurityContextUtil.getUserInfo(), organization, space);
    }

}
