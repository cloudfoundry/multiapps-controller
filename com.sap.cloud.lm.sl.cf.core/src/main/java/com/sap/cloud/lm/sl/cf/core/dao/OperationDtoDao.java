package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class OperationDtoDao {

    @Autowired
    @Qualifier("operationEntityManagerFactory")
    EntityManagerFactory emf;

    public void add(OperationDto ongoingOperation) throws ConflictException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            if (existsInternal(manager, ongoingOperation.getProcessId())) {
                throw new ConflictException(Messages.ONGOING_OPERATION_ALREADY_EXISTS, ongoingOperation.getProcessId());
            }
            manager.persist(ongoingOperation);
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
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            OperationDto oo = manager.find(OperationDto.class, processId);
            if (oo == null) {
                throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId);
            }
            manager.remove(oo);
            return null;
        });
    }

    public OperationDto find(String processId) {
        return new Executor<OperationDto>(createEntityManager()).execute((manager) -> {
            return manager.find(OperationDto.class, processId);
        });
    }

    public OperationDto findRequired(String processId) throws NotFoundException {
        OperationDto oo = find(processId);
        if (oo == null) {
            throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId);
        }
        return oo;
    }

    @SuppressWarnings("unchecked")
    public List<OperationDto> findAll() {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all").getResultList();
        });
    }

    @SuppressWarnings("unchecked")
    public List<OperationDto> findAllInSpace(String spaceId) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all_in_space").setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID,
                spaceId).getResultList();
        });
    }

    @SuppressWarnings("unchecked")
    public List<OperationDto> findLastOperations(int last, String spaceId) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all_in_space_desc").setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID,
                spaceId).setMaxResults(last).getResultList();
        });
    }

    public List<OperationDto> findActiveOperations(String spaceId, List<State> requestedActiveStates) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, true, requestedActiveStates, manager).getResultList();
        });
    }

    public List<OperationDto> findFinishedOperations(String spaceId, List<State> requestedFinishedStates) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, false, requestedFinishedStates, manager).getResultList();
        });
    }

    public List<OperationDto> findAllInSpaceByStatus(List<State> requestedStates, String spaceId) {
        return new Executor<List<OperationDto>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, null, requestedStates, manager).getResultList();
        });
    }

    public List<OperationDto> findOperationsByStatus(List<State> requestedStates, String spaceId) {
        boolean parametersContainsOnlyFinishedStates = Collections.disjoint(requestedStates, State.getActiveStates());
        boolean parametersContainsOnlyActiveStates = Collections.disjoint(requestedStates, State.getFinishedStates());
        if (parametersContainsOnlyActiveStates) {
            return findActiveOperations(spaceId, requestedStates);
        } else if (parametersContainsOnlyFinishedStates) {
            return findFinishedOperations(spaceId, requestedStates);
        }
        return findAllInSpaceByStatus(requestedStates, spaceId);
    }

    public void merge(OperationDto ongoingOperation) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            OperationDto oo = manager.find(OperationDto.class, ongoingOperation.getProcessId());
            if (oo == null) {
                throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, ongoingOperation.getProcessId());
            }
            manager.merge(ongoingOperation);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public OperationDto findProcessWithLock(String mtaId, String spaceId) throws SLException {
        return new Executor<OperationDto>(createEntityManager()).execute((manager) -> {
            List<OperationDto> processes = manager.createNamedQuery("find_mta_lock").setParameter(TableColumnNames.ONGOING_OPERATION_MTA_ID,
                mtaId).setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID, spaceId).getResultList();
            if (processes.size() == 0) {
                return null;
            }
            if (processes.size() == 1) {
                return processes.get(0);
            }
            throw new SLException(Messages.MULTIPLE_OPERATIONS_WITH_LOCK_FOUND, mtaId, spaceId);
        });
    }

    private TypedQuery<OperationDto> createQuery(String spaceId, Boolean shouldFinalStateBeNull, List<State> statusList,
        EntityManager manager) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<OperationDto> query = criteriaBuilder.createQuery(OperationDto.class);
        Root<OperationDto> root = query.from(OperationDto.class);
        Predicate spaceIdPredicate = null;

        if (spaceId != null) {
            spaceIdPredicate = criteriaBuilder.equal(root.get(TableColumnNames.ONGOING_OPERATION_SPACE_ID), spaceId);
        }

        List<Predicate> predicates = new ArrayList<>();

        if (shouldFinalStateBeNull != null) {
            predicates.add(getFinalStateNullPredicate(shouldFinalStateBeNull, root));
        }

        for (State status : statusList) {
            predicates.add(criteriaBuilder.equal(root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE), status.toString()));
        }

        Predicate finalStatePredicate = criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        Predicate wherePredicate = criteriaBuilder.and(spaceIdPredicate, finalStatePredicate);

        return manager.createQuery(query.select(root).where(wherePredicate));
    }

    private Predicate getFinalStateNullPredicate(Boolean shouldBeNull, Root<OperationDto> root) {
        if (shouldBeNull) {
            return root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE).isNull();
        }
        return root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE).isNotNull();
    }

}
