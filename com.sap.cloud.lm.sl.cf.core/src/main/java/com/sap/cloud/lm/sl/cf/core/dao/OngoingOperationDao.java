package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OngoingOperationDto;
import com.sap.cloud.lm.sl.cf.core.helpers.OngoingOperationFactory;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.lmsl.slp.SlpTaskState;

@Component
public class OngoingOperationDao {

    @Inject
    OngoingOperationDtoDao dao;
    @Inject
    OngoingOperationFactory ongoingOperationFactory;

    public void add(OngoingOperation ongoingOperation) throws ConflictException {
        OngoingOperationDto dto = ongoingOperationFactory.toPersistenceDto(ongoingOperation);
        dao.add(dto);
    }

    public void remove(String processId) throws NotFoundException {
        dao.remove(processId);
    }

    public OngoingOperation find(String processId) {
        OngoingOperationDto dto = dao.find(processId);
        if (dto == null) {
            return null;
        }
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    public OngoingOperation findRequired(String processId) throws NotFoundException {
        OngoingOperationDto dto = dao.findRequired(processId);
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    public List<OngoingOperation> findAll() {
        List<OngoingOperationDto> dtos = dao.findAll();
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findAllInSpace(String spaceId) {
        List<OngoingOperationDto> dtos = dao.findAllInSpace(spaceId);
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findLastOperations(int last, String spaceId) {
        List<OngoingOperationDto> dtos = dao.findLastOperations(last, spaceId);
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findActiveOperations(String spaceId, List<SlpTaskState> requestedActiveStates) {
        List<OngoingOperationDto> dtos = dao.findActiveOperations(spaceId, requestedActiveStates);
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findFinishedOperations(String spaceId, List<SlpTaskState> requestedFinishedStates) {
        List<OngoingOperationDto> dtos = dao.findFinishedOperations(spaceId, requestedFinishedStates);
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findAllInSpaceByStatus(List<SlpTaskState> requestedStates, String spaceId) {
        List<OngoingOperationDto> dtos = dao.findAllInSpaceByStatus(requestedStates, spaceId);
        return toOngoingOperations(dtos);
    }

    public List<OngoingOperation> findOperationsByStatus(List<SlpTaskState> requestedStates, String spaceId) {
        List<OngoingOperationDto> dtos = dao.findOperationsByStatus(requestedStates, spaceId);
        return toOngoingOperations(dtos);
    }

    public void merge(OngoingOperation ongoingOperation) throws NotFoundException {
        OngoingOperationDto dto = ongoingOperationFactory.toPersistenceDto(ongoingOperation);
        dao.merge(dto);
    }

    public OngoingOperation findProcessWithLock(String mtaId, String spaceId) throws SLException {
        OngoingOperationDto dto = dao.findProcessWithLock(mtaId, spaceId);
        if (dto == null) {
            return null;
        }
        return ongoingOperationFactory.fromPersistenceDto(dto);
    }

    private List<OngoingOperation> toOngoingOperations(List<OngoingOperationDto> dtos) {
        return dtos.stream().map(dto -> ongoingOperationFactory.fromPersistenceDto(dto)).collect(Collectors.toList());
    }

}
