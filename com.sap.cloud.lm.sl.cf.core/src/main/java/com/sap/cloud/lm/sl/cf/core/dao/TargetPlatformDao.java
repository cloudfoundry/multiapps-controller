package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sap.cloud.lm.sl.cf.core.dto.serialization.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;

public abstract class TargetPlatformDao {

    protected final Class<? extends TargetPlatformDto> classVersion;
    protected final String findAllQueryName;

    protected TargetPlatformDao(Class<? extends TargetPlatformDto> classVersion, String findAllQueryName) {
        this.findAllQueryName = findAllQueryName;
        this.classVersion = classVersion;
    }

    protected abstract EntityManagerFactory getEmf();

    public void add(TargetPlatform platform) throws ConflictException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            if (existsInternal(manager, platform.getName())) {
                throw new ConflictException(Messages.TARGET_PLATFORM_ALREADY_EXISTS, platform.getName());
            }
            manager.persist(wrap(platform));
            return null;
        });
    }

    private EntityManager createEntityManager() {
        return getEmf().createEntityManager();
    }

    protected abstract TargetPlatformDto wrap(TargetPlatform platform);

    protected boolean existsInternal(EntityManager manager, String name) {
        try {
            findInternal(manager, name);
        } catch (NotFoundException e) {
            return false;
        }
        return true;
    }

    public void remove(String name) throws NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            manager.remove(findInternal(manager, name));
            return null;
        });
    }

    public void merge(String name, TargetPlatform platform) throws ConflictException, NotFoundException {
        new TransactionalExecutor<Void>(createEntityManager()).execute((manager) -> {
            TargetPlatformDto existingDto = findInternal(manager, name);

            checkForConflicts(manager, name, platform.getName());
            manager.remove(existingDto);

            manager.persist(wrap(platform));
            return null;
        });
    }

    protected void checkForConflicts(EntityManager manager, String oldPlatformName, String newPlatformName) throws ConflictException {
        if (!oldPlatformName.equals(newPlatformName) && existsInternal(manager, newPlatformName)) {
            throw new ConflictException(Messages.TARGET_PLATFORM_ALREADY_EXISTS, newPlatformName);
        }
    }

    public TargetPlatform find(String name) throws NotFoundException {
        return new Executor<TargetPlatform>(createEntityManager()).execute((manager) -> {
            return findInternal(manager, name).toTargetPlatform();
        });
    }

    protected TargetPlatformDto findInternal(EntityManager manager, String name) throws NotFoundException {
        TargetPlatformDto dto = manager.find(classVersion, name);
        if (dto == null) {
            throw new NotFoundException(Messages.TARGET_PLATFORM_NOT_FOUND, name);
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    public List<TargetPlatform> findAll() {
        return new Executor<List<TargetPlatform>>(createEntityManager()).execute((manager) -> {
            List<TargetPlatformDto> dtos = manager.createNamedQuery(findAllQueryName).getResultList();
            return unwrap(dtos);
        });
    }

    protected List<TargetPlatform> unwrap(List<TargetPlatformDto> dtos) {
        List<TargetPlatform> platforms = new ArrayList<>();
        for (TargetPlatformDto dto : dtos) {
            platforms.add(dto.toTargetPlatform());
        }
        return platforms;
    }

}
