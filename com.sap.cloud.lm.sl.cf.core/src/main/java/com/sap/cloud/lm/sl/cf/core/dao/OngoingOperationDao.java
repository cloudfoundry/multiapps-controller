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

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation.SlpTaskStates;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.lmsl.slp.SlpTaskState;

@Component
public class OngoingOperationDao {

    @Autowired
    @Qualifier("ongoingOperationEntityManagerFactory")
    EntityManagerFactory emf;

    public void add(OngoingOperation ongoingOperation) throws ConflictException {
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
        return manager.find(OngoingOperation.class, processId) != null;
    }

    public void remove(String processId) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            OngoingOperation oo = manager.find(OngoingOperation.class, processId);
            if (oo == null) {
                throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId);
            }
            manager.remove(oo);
            return null;
        });
    }

    public void remove(OngoingOperation ongoingOperation) throws NotFoundException {
        remove(ongoingOperation.getProcessId());
    }

    public OngoingOperation find(String processId) {
        return new Executor<OngoingOperation>(createEntityManager()).execute((manager) -> {
            return manager.find(OngoingOperation.class, processId);
        });
    }

    public OngoingOperation findRequired(String processId) throws NotFoundException {
        OngoingOperation oo = find(processId);
        if (oo == null) {
            throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, processId);
        }
        return oo;
    }

    @SuppressWarnings("unchecked")
    public List<OngoingOperation> findAll() {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all").getResultList();
        });
    }

    @SuppressWarnings("unchecked")
    public List<OngoingOperation> findAllInSpace(String spaceId) {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all_in_space").setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID,
                spaceId).getResultList();
        });
    }

    @SuppressWarnings("unchecked")
    public List<OngoingOperation> findLastOperations(int last, String spaceId) {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return manager.createNamedQuery("find_all_in_space_desc").setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID,
                spaceId).setMaxResults(last).getResultList();
        });
    }

    public List<OngoingOperation> findActiveOperations(String spaceId, List<SlpTaskState> requestedActiveStates) {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, true, requestedActiveStates, manager).getResultList();
        });
    }

    public List<OngoingOperation> findFinishedOperations(String spaceId, List<SlpTaskState> requestedFinishedStates) {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, false, requestedFinishedStates, manager).getResultList();
        });
    }

    public List<OngoingOperation> findAllInSpaceByStatus(List<SlpTaskState> requestedStates, String spaceId) {
        return new Executor<List<OngoingOperation>>(createEntityManager()).execute((manager) -> {
            return createQuery(spaceId, null, requestedStates, manager).getResultList();
        });
    }

    public List<OngoingOperation> findOperationsByStatus(List<SlpTaskState> requestedStates, String spaceId) {
        boolean parametersContainsOnlyFinishedStates = Collections.disjoint(requestedStates, SlpTaskStates.getActiveSlpTaskStates());
        boolean parametersContainsOnlyActiveStates = Collections.disjoint(requestedStates, SlpTaskStates.getFinishedSlpTaskStates());
        if (parametersContainsOnlyActiveStates) {
            return findActiveOperations(spaceId, requestedStates);
        } else if (parametersContainsOnlyFinishedStates) {
            return findFinishedOperations(spaceId, requestedStates);
        }
        return findAllInSpaceByStatus(requestedStates, spaceId);
    }

    public void merge(OngoingOperation ongoingProcess) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            OngoingOperation oo = manager.find(OngoingOperation.class, ongoingProcess.getProcessId());
            if (oo == null) {
                throw new NotFoundException(Messages.ONGOING_OPERATION_NOT_FOUND, ongoingProcess.getProcessId());
            }
            manager.merge(ongoingProcess);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public OngoingOperation findProcessWithLock(String mtaId, String spaceId) throws SLException {
        return new Executor<OngoingOperation>(createEntityManager()).execute((manager) -> {
            List<OngoingOperation> processes = manager.createNamedQuery("find_mta_lock").setParameter(
                TableColumnNames.ONGOING_OPERATION_MTA_ID, mtaId).setParameter(TableColumnNames.ONGOING_OPERATION_SPACE_ID,
                    spaceId).getResultList();
            if (processes.size() == 0) {
                return null;
            }
            if (processes.size() == 1) {
                return processes.get(0);
            }
            throw new SLException(Messages.MULTIPLE_OPERATIONS_WITH_LOCK_FOUND, mtaId, spaceId);
        });
    }

    private TypedQuery<OngoingOperation> createQuery(String spaceId, Boolean shouldFinalStateBeNull, List<SlpTaskState> statusList,
        EntityManager manager) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<OngoingOperation> query = criteriaBuilder.createQuery(OngoingOperation.class);
        Root<OngoingOperation> root = query.from(OngoingOperation.class);
        Predicate spaceIdPredicate = null;

        if (spaceId != null) {
            spaceIdPredicate = criteriaBuilder.equal(root.get(TableColumnNames.ONGOING_OPERATION_SPACE_ID), spaceId);
        }

        List<Predicate> predicates = new ArrayList<>();

        if (shouldFinalStateBeNull != null) {
            predicates.add(getFinalStateNullPredicate(shouldFinalStateBeNull, root));
        }

        for (SlpTaskState status : statusList) {
            predicates.add(criteriaBuilder.equal(root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE), status));
        }

        Predicate finalStatePredicate = criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        Predicate wherePredicate = criteriaBuilder.and(spaceIdPredicate, finalStatePredicate);

        return manager.createQuery(query.select(root).where(wherePredicate));
    }

    private Predicate getFinalStateNullPredicate(Boolean shouldBeNull, Root<OngoingOperation> root) {
        if (shouldBeNull) {
            return root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE).isNull();
        }
        return root.get(TableColumnNames.ONGOING_OPERATION_FINAL_STATE).isNotNull();
    }

}
