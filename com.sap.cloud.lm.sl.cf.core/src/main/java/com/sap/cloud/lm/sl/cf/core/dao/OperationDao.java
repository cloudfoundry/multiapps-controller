package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Component
public class OperationDao extends AbstractDao<Operation, OperationDto, String> {

    @Inject
    OperationDtoDao dao;
    @Inject
    OperationFactory operationFactory;

    public void update(Operation operation) {
        update(operation.getProcessId(), operation);
    }

    public void removeBy(List<String> processIds) {
        for (String processId : processIds) {
            dao.remove(processId);
        }
    }

    public int removeExpiredInFinalState(Date expirationTime) {
        return dao.removeExpiredInFinalState(expirationTime);
    }

    public Operation findRequired(String processId) {
        OperationDto dto = dao.findRequired(processId);
        return fromDto(dto);
    }

    public List<Operation> find(OperationFilter filter) {
        List<OperationDto> dtos = dao.find(filter);
        return fromDtos(dtos);
    }

    @Override
    protected AbstractDtoDao<OperationDto, String> getDtoDao() {
        return dao;
    }

    @Override
    protected Operation fromDto(OperationDto operationDto) {
        return operationDto != null ? operationFactory.fromPersistenceDto(operationDto) : null;
    }

    @Override
    protected OperationDto toDto(Operation operation) {
        return operationFactory.toPersistenceDto(operation);
    }
}
