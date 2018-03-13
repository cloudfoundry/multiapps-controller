package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension.FieldNames;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component
public class ContextExtensionDao {

    @Autowired
    @Qualifier("contextExtensionEntityManagerFactory")
    EntityManagerFactory emf;

    public ContextExtension add(ContextExtension entry) throws ConflictException {
        try {
            return new TransactionalExecutor<ContextExtension>(createEntityManager()).execute((manager) -> {

                manager.persist(entry);
                return entry;

            });
        } catch (RollbackException e) {
            throw new ConflictException(e, Messages.CONTEXT_EXTENSION_ENTRY_ALREADY_EXISTS, entry.getProcessId(), entry.getName(),
                entry.getValue());
        }
    }

    public ContextExtension update(long id, ContextExtension entryDelta) throws ConflictException, NotFoundException {
        try {
            return new TransactionalExecutor<ContextExtension>(createEntityManager()).execute((manager) -> {

                ContextExtension existingEntry = findInternal(manager, id);
                if (existingEntry == null) {
                    throw new NotFoundException(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, id);
                }

                ContextExtension entry = merge(entryDelta, existingEntry);
                manager.merge(entry);
                return entry;

            });
        } catch (RollbackException e) {
            ContextExtension entry = merge(find(id), entryDelta);
            throw new ConflictException(e, Messages.CONTEXT_EXTENSION_ENTRY_ALREADY_EXISTS, entry.getId(), entry.getProcessId(),
                entry.getName(), entry.getValue());
        }
    }

    public ContextExtension addOrUpdate(String processId, String name, String value) throws SLException {
        return new TransactionalExecutor<ContextExtension>(createEntityManager()).execute((manager) -> {

            ContextExtension entryToPersist = new ContextExtension(processId, name, value, new Date(), new Date());
            ContextExtension existingEntry = findInternal(processId, name, manager);
            if (existingEntry == null) {
                return add(entryToPersist);
            }
            entryToPersist.setCreateTime(existingEntry.getCreateTime());
            return update(existingEntry.getId(), entryToPersist);

        });
    }

    public void remove(long id) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {

            ContextExtension entry = findInternal(manager, id);
            if (entry == null) {
                throw new NotFoundException(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, id);
            }
            manager.remove(entry);
            return null;

        });
    }

    @SuppressWarnings("unchecked")
    public List<ContextExtension> findAll() {
        return new Executor<List<ContextExtension>>(createEntityManager()).execute((manager) -> {

            return manager.createNamedQuery(NamedQueries.FIND_ALL_CONTEXT_EXTENSION_ENTRIES).getResultList();

        });
    }

    @SuppressWarnings("unchecked")
    public List<ContextExtension> findAll(String processId) {
        return new Executor<List<ContextExtension>>(createEntityManager()).execute((manager) -> {

            return manager.createNamedQuery(NamedQueries.FIND_ALL_CONTEXT_EXTENSION_ENTRIES_BY_PROCESS_ID).setParameter(
                FieldNames.PROCESS_ID, processId).getResultList();

        });
    }

    public ContextExtension find(long id) throws NotFoundException {
        return new Executor<ContextExtension>(createEntityManager()).execute((manager) -> {

            ContextExtension entry = findInternal(manager, id);
            if (entry == null) {
                throw new NotFoundException(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, id);
            }
            return entry;
        });
    }

    public ContextExtension find(String processId, String name) {
        return new Executor<ContextExtension>(createEntityManager()).execute((manager) -> {

            return findInternal(processId, name, manager);

        });

    }

    public boolean exists(long id) {
        return new Executor<Boolean>(createEntityManager()).execute((manager) -> {

            return findInternal(manager, id) != null;

        });
    }

    private ContextExtension merge(ContextExtension entry, ContextExtension existingEntry) {
        long id = existingEntry.getId();
        String processId = CommonUtil.merge(existingEntry.getProcessId(), entry.getProcessId(), null);
        String name = CommonUtil.merge(existingEntry.getName(), entry.getName(), null);
        String value = CommonUtil.merge(existingEntry.getValue(), entry.getValue(), null);
        Date createTime = CommonUtil.merge(existingEntry.getCreateTime(), entry.getCreateTime(), null);
        Date updateTime = CommonUtil.merge(existingEntry.getLastUpdatedTime(), entry.getLastUpdatedTime(), null);
        return new ContextExtension(id, processId, name, value, createTime, updateTime);
    }

    private ContextExtension findInternal(String processId, String name, EntityManager manager) {
        TypedQuery<ContextExtension> query = createQuery(processId, name, manager);

        setQueryParameter(query, ContextExtension.FieldNames.PROCESS_ID, processId);
        setQueryParameter(query, ContextExtension.FieldNames.NAME, name);

        List<ContextExtension> contextExtensions = query.getResultList();
        if (contextExtensions.isEmpty()) {
            return null;
        }
        return contextExtensions.get(0);
    }

    private TypedQuery<ContextExtension> createQuery(String processId, String name, EntityManager manager) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<ContextExtension> query = builder.createQuery(ContextExtension.class);
        Root<ContextExtension> root = query.from(ContextExtension.class);

        List<Predicate> predicates = new ArrayList<>();
        if (processId != null) {
            predicates.add(builder.equal(root.get(ContextExtension.FieldNames.PROCESS_ID),
                builder.parameter(String.class, ContextExtension.FieldNames.PROCESS_ID)));
        }
        if (name != null) {
            predicates.add(builder.equal(root.get(ContextExtension.FieldNames.NAME),
                builder.parameter(String.class, ContextExtension.FieldNames.NAME)));
        }

        return manager.createQuery(query.select(root).where(predicates.toArray(new Predicate[0])));

    }

    private void setQueryParameter(TypedQuery<ContextExtension> query, String parameterName, String parameterValue) {
        if (parameterValue != null) {
            query.setParameter(parameterName, parameterValue);
        }
    }

    private ContextExtension findInternal(EntityManager manager, long id) {
        return manager.find(ContextExtension.class, id);
    }

    private EntityManager createEntityManager() {
        return emf.createEntityManager();
    }
}
