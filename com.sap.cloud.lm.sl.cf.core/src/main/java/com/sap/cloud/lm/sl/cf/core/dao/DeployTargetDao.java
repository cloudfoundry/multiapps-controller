package com.sap.cloud.lm.sl.cf.core.dao;

import static com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.TableColumnNames.DEPLOY_TARGET_NAME;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public abstract class DeployTargetDao<Tgt extends Target, Dto extends DeployTargetDto<Tgt>> {

    protected final Class<Dto> classVersion;
    protected final String findAllQueryName;

    protected DeployTargetDao(Class<Dto> classVersion, String findAllQueryName) {
        this.findAllQueryName = findAllQueryName;
        this.classVersion = classVersion;
    }

    protected abstract EntityManagerFactory getEmf();

    public PersistentObject<Tgt> add(Tgt target) throws ConflictException {
        TransactionalExecutor<Dto> executor = new TransactionalExecutor<>(createEntityManager());
        Dto persisted = executor.execute(manager -> {
            if (existsInternal(manager, target.getName())) {
                throw new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, target.getName());
            }
            Dto dto = wrap(target);
            manager.persist(dto);
            return dto;
        });
        return persisted.toDeployTarget();
    }

    private EntityManager createEntityManager() {
        return getEmf().createEntityManager();
    }

    protected abstract Dto wrap(Tgt target);

    protected boolean existsInternal(EntityManager manager, long id) {
        try {
            findInternal(manager, id);
        } catch (NotFoundException e) {
            return false;
        }
        return true;
    }

    protected boolean existsInternal(EntityManager manager, String name) {
        try {
            findInternalByName(manager, name);
        } catch (NotFoundException e) {
            return false;
        }
        return true;
    }

    public void remove(long id) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            manager.remove(findInternal(manager, id));
            return null;
        });
    }

    public PersistentObject<Tgt> merge(long id, Tgt target) throws ConflictException, NotFoundException {
        Dto dto = new TransactionalExecutor<Dto>(createEntityManager()).execute(manager -> {
            Dto existingDto = findInternal(manager, id);

            PersistentObject<Tgt> persistentTarget = existingDto.toDeployTarget();
            checkForConflicts(manager, persistentTarget.getObject(), target);
            existingDto.setDeployTarget(target);
            manager.merge(existingDto);
            return existingDto;
        });
        return dto.toDeployTarget();
    }

    protected void checkForConflicts(EntityManager manager, Tgt oldTarget, Tgt newTarget) throws ConflictException {
        String oldTargetName = oldTarget.getName();
        String newTargetName = newTarget.getName();
        if (!oldTargetName.equals(newTargetName) && existsInternal(manager, newTargetName)) {
            throw new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, newTargetName);
        }
    }

    public PersistentObject<Tgt> find(long id) throws NotFoundException {
        Dto entity = new Executor<Dto>(createEntityManager()).execute(manager -> findInternal(manager, id));
        return entity.toDeployTarget();
    }

    public PersistentObject<Tgt> findByName(String name) throws NotFoundException {
        Dto entity = new Executor<Dto>(createEntityManager()).execute(manager -> findInternalByName(manager, name));
        return entity.toDeployTarget();
    }

    protected Dto findInternal(EntityManager manager, long id) throws NotFoundException {
        Dto entity = manager.find(classVersion, id);
        if (entity == null) {
            throw new NotFoundException(Messages.DEPLOY_TARGET_NOT_FOUND, id);
        }
        return entity;
    }

    protected Dto findInternalByName(EntityManager manager, String name) throws NotFoundException {
        TypedQuery<Dto> query = createFindByNameQuery(manager, name);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException(Messages.DEPLOY_TARGET_WITH_NAME_NOT_FOUND, name);
        }
    }

    @SuppressWarnings("unchecked")
    public List<PersistentObject<Tgt>> findAll() {
        List<Dto> dtos;
        dtos = new Executor<List<Dto>>(createEntityManager()).execute(manager -> (List<Dto>) manager.createNamedQuery(findAllQueryName)
            .getResultList());
        return unwrap(dtos);
    }

    protected List<PersistentObject<Tgt>> unwrap(List<Dto> dtos) {
        List<PersistentObject<Tgt>> targets = new ArrayList<>();
        for (Dto dto : dtos) {
            targets.add(dto.toDeployTarget());
        }
        return targets;
    }

    private TypedQuery<Dto> createFindByNameQuery(EntityManager manager, String name) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Dto> query = builder.createQuery(this.classVersion);
        Root<Dto> entity = query.from(this.classVersion);

        Predicate namePredicate = builder.equal(entity.get(DEPLOY_TARGET_NAME), name);
        return manager.createQuery(query.select(entity)
            .where(namePredicate));
    }

}
