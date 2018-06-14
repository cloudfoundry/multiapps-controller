package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component
public class OperationDtoDao {

    @Autowired
    @Qualifier("operationEntityManagerFactory")
    EntityManagerFactory emf;

    public void add(OperationDto operation) throws ConflictException {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            if (existsInternal(manager, operation.getProcessId())) {
                throw new ConflictException(Messages.OPERATION_ALREADY_EXISTS, operation.getProcessId());
            }
            manager.persist(operation);
            return null;
        });
    }

    private EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    private boolean existsInternal(EntityManager manager, String processId) {
        return manager.find(OperationDto.class, processId) != null;
    }

    public void remove(String processId) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            OperationDto dto = manager.find(OperationDto.class, processId);
            if (dto == null) {
                throw new NotFoundException(Messages.OPERATION_NOT_FOUND, processId);
            }
            manager.remove(dto);
            return null;
        });
    }

    public OperationDto find(String processId) {
        return new Executor<OperationDto>(createEntityManager()).execute(manager -> manager.find(OperationDto.class, processId));
    }

    public OperationDto findRequired(String processId) throws NotFoundException {
        OperationDto dto = find(processId);
        if (dto == null) {
            throw new NotFoundException(Messages.OPERATION_NOT_FOUND, processId);
        }
        return dto;
    }

    public List<OperationDto> find(OperationFilter filter) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute(manager -> createQuery(manager, filter).getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<OperationDto> findAll() {
        return new Executor<List<OperationDto>>(createEntityManager()).execute(manager -> manager.createNamedQuery("find_all")
            .getResultList());
    }

    public void merge(OperationDto operation) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            OperationDto dto = manager.find(OperationDto.class, operation.getProcessId());
            if (dto == null) {
                throw new NotFoundException(Messages.OPERATION_NOT_FOUND, operation.getProcessId());
            }
            manager.merge(operation);
            return null;
        });
    }

    private TypedQuery<OperationDto> createQuery(EntityManager manager, OperationFilter operationFilter) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<OperationDto> query = criteriaBuilder.createQuery(OperationDto.class);
        Root<OperationDto> root = query.from(OperationDto.class);

        Predicate[] predicates = getPredicates(operationFilter, criteriaBuilder, root);

        query.select(root)
            .where(predicates);
        if (operationFilter.getOrderAttribute() != null) {
            setOrdering(operationFilter, criteriaBuilder, query, root);
        }

        TypedQuery<OperationDto> typedQuery = manager.createQuery(query);
        if (operationFilter.getMaxResults() != null) {
            typedQuery.setMaxResults(operationFilter.getMaxResults());
        }
        return typedQuery;
    }

    private Predicate[] getPredicates(OperationFilter operationFilter, CriteriaBuilder criteriaBuilder, Root<OperationDto> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (operationFilter.getSpaceId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.SPACE_ID), operationFilter.getSpaceId()));
        }
        if (operationFilter.getMtaId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.MTA_ID), operationFilter.getMtaId()));
        }
        if (operationFilter.getUser() != null) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.USER), operationFilter.getUser()));
        }
        if (operationFilter.isInNonFinalState()) {
            predicates.add(root.get(OperationDto.AttributeNames.FINAL_STATE)
                .isNull());
        }
        if (operationFilter.isInFinalState()) {
            predicates.add(root.get(OperationDto.AttributeNames.FINAL_STATE)
                .isNotNull());
        }
        if (operationFilter.hasNotAcquiredLock()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.ACQUIRED_LOCK), false));
        }
        if (operationFilter.hasAcquiredLock()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.ACQUIRED_LOCK), true));
        }
        if (operationFilter.isCleanedUp()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.CLEANED_UP), true));
        }
        if (operationFilter.isNotCleanedUp()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.CLEANED_UP), false));
        }
        if (operationFilter.getStates() != null) {
            predicates.add(root.get(OperationDto.AttributeNames.FINAL_STATE)
                .in(toStrings(operationFilter.getStates())));
        }
        if (operationFilter.getStartTimeUpperBound() != null) {
            predicates
                .add(criteriaBuilder.lessThan(root.get(OperationDto.AttributeNames.STARTED_AT), operationFilter.getStartTimeUpperBound()));
        }
        if (operationFilter.getEndTimeUpperBound() != null) {
            predicates
                .add(criteriaBuilder.lessThan(root.get(OperationDto.AttributeNames.ENDED_AT), operationFilter.getEndTimeUpperBound()));
        }
        if (operationFilter.getEndTimeLowerBound() != null) {
            predicates
                .add(criteriaBuilder.greaterThan(root.get(OperationDto.AttributeNames.ENDED_AT), operationFilter.getEndTimeLowerBound()));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private void setOrdering(OperationFilter operationFilter, CriteriaBuilder criteriaBuilder, CriteriaQuery<OperationDto> query,
        Root<OperationDto> root) {
        if (operationFilter.getOrderDirection() == OrderDirection.ASCENDING) {
            query.orderBy(criteriaBuilder.asc(root.get(operationFilter.getOrderAttribute())));
        } else {
            query.orderBy(criteriaBuilder.desc(root.get(operationFilter.getOrderAttribute())));
        }
    }

    private Object toStrings(List<State> states) {
        return states.stream()
            .map(state -> state.toString())
            .collect(Collectors.toList());
    }

}
