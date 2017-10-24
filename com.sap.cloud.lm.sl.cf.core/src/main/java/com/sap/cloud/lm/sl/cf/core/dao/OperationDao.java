package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class OperationDao {

    @Inject
    OperationDtoDao dao;
    @Inject
    OperationFactory ongoingOperationFactory;

    public void add(Operation operation) throws ConflictException {
        OperationDto dto = ongoingOperationFactory.toPersistenceDto(operation);
        dao.add(dto);
    }

    public void remove(String processId) throws NotFoundException {
        dao.remove(processId);
    }

    public Operation find(String processId) {
        OperationDto dto = dao.find(processId);
        if (dto == null) {
            return null;
        }
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    public Operation findRequired(String processId) throws NotFoundException {
        OperationDto dto = dao.findRequired(processId);
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    public List<Operation> findAll() {
        List<OperationDto> dtos = dao.findAll();
        return toOperations(dtos);
    }

    public List<Operation> findAllInSpace(String spaceId) {
        List<OperationDto> dtos = dao.findAllInSpace(spaceId);
        return toOperations(dtos);
    }

    public List<Operation> findLastOperations(int last, String spaceId) {
        List<OperationDto> dtos = dao.findLastOperations(last, spaceId);
        return toOperations(dtos);
    }

    public List<Operation> findActiveOperations(String spaceId, List<State> requestedActiveStates) {
        List<OperationDto> dtos = dao.findActiveOperations(spaceId, requestedActiveStates);
        return toOperations(dtos);
    }

    public List<Operation> findFinishedOperations(String spaceId, List<State> requestedFinishedStates) {
        List<OperationDto> dtos = dao.findFinishedOperations(spaceId, requestedFinishedStates);
        return toOperations(dtos);
    }

    public List<Operation> findAllInSpaceByStatus(List<State> requestedStates, String spaceId) {
        List<OperationDto> dtos = dao.findAllInSpaceByStatus(requestedStates, spaceId);
        return toOperations(dtos);
    }

    public List<Operation> findOperationsByStatus(List<State> requestedStates, String spaceId) {
        List<OperationDto> dtos = dao.findOperationsByStatus(requestedStates, spaceId);
        return toOperations(dtos);
    }

    public void merge(Operation ongoingOperation) throws NotFoundException {
        OperationDto dto = ongoingOperationFactory.toPersistenceDto(ongoingOperation);
        dao.merge(dto);
    }

    public Operation findProcessWithLock(String mtaId, String spaceId) throws SLException {
        OperationDto dto = dao.findProcessWithLock(mtaId, spaceId);
        if (dto == null) {
            return null;
        }
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    private List<Operation> toOperations(List<OperationDto> dtos) {
        return dtos.stream().map(dto -> ongoingOperationFactory.fromPersistenceDto(dto)).collect(Collectors.toList());
    }

}
