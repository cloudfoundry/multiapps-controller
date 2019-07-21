package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component
public class OperationDtoDao extends AbstractDtoDao<OperationDto, String> {

    @Inject
    public OperationDtoDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public int removeExpiredInFinalState(Date expirationTime) {
        return executeInTransaction(
            manager -> manager.createNamedQuery(PersistenceMetadata.NamedQueries.DELETE_EXPIRED_OPERATIONS_IN_FINAL_STATE)
                .setParameter("expirationTime", expirationTime, TemporalType.TIMESTAMP)
                .executeUpdate());
    }

    public OperationDto findRequired(String processId) {
        OperationDto dto = find(processId);
        if (dto == null) {
            throw new NotFoundException(Messages.OPERATION_NOT_FOUND, processId);
        }
        return dto;
    }

    public List<OperationDto> find(OperationFilter filter) {
        return execute(manager -> createQuery(manager, filter).getResultList());
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
        if (operationFilter.getFirstElement() != null) {
            typedQuery.setFirstResult(operationFilter.getFirstElement());
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
        if (operationFilter.isWithoutAcquiredLock()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.ACQUIRED_LOCK), false));
        }
        if (operationFilter.isWithAcquiredLock()) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.ACQUIRED_LOCK), true));
        }
        if (operationFilter.getStates() != null) {
            predicates.add(root.get(OperationDto.AttributeNames.FINAL_STATE)
                .in(toStrings(operationFilter.getStates())));
        }
        if (operationFilter.getStartedBefore() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(OperationDto.AttributeNames.STARTED_AT), operationFilter.getStartedBefore()));
        }
        if (operationFilter.getEndedBefore() != null) {
            predicates.add(criteriaBuilder.lessThan(root.get(OperationDto.AttributeNames.ENDED_AT), operationFilter.getEndedBefore()));
        }
        if (operationFilter.getEndedAfter() != null) {
            predicates.add(criteriaBuilder.greaterThan(root.get(OperationDto.AttributeNames.ENDED_AT), operationFilter.getEndedAfter()));
        }
        if (operationFilter.getProcessType() != null) {
            predicates.add(criteriaBuilder.equal(root.get(OperationDto.AttributeNames.PROCESS_TYPE), operationFilter.getProcessType()));
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

    private List<String> toStrings(List<State> states) {
        return states.stream()
            .map(State::toString)
            .collect(Collectors.toList());
    }

    @Override
    protected OperationDto merge(OperationDto existingDto, OperationDto newDto) {
        return newDto;
    }

    @Override
    protected void onEntityNotFound(String processId) {
        throw new NotFoundException(Messages.OPERATION_NOT_FOUND, processId);
    }

    @Override
    protected void onEntityConflict(OperationDto operation, Throwable t) {
        String processId = operation.getPrimaryKey();
        throw (ConflictException) new ConflictException(Messages.OPERATION_ALREADY_EXISTS, processId).initCause(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<OperationDto> getDtoClass() {
        return OperationDto.class;
    }

    @Override
    protected String getFindAllNamedQuery() {
        return PersistenceMetadata.NamedQueries.FIND_ALL_OPERATIONS;
    }

}
