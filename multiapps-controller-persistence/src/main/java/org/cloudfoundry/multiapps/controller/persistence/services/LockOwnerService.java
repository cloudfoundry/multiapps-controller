package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.LockOwnerDto;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.LockOwnersQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.LockOwnersQueryImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;

@Named
public class LockOwnerService extends PersistenceService<LockOwnerEntry, LockOwnerDto, Long> {

    @Inject
    public LockOwnerService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public LockOwnersQuery createQuery() {
        return new LockOwnersQueryImpl(createEntityManager());
    }

    @Override
    protected PersistenceObjectMapper<LockOwnerEntry, LockOwnerDto> getPersistenceObjectMapper() {
        return new LockOwnersMapper();
    }

    @Override
    protected void onEntityConflict(LockOwnerDto dto, Throwable t) {
        throw new ConflictException(t, Messages.LOCK_OWNER_ALREADY_EXISTS, dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long primaryKey) {
        throw new NotFoundException(Messages.LOCK_OWNER_NOT_FOUND, primaryKey);
    }

    public static class LockOwnersMapper implements PersistenceObjectMapper<LockOwnerEntry, LockOwnerDto> {

        @Override
        public LockOwnerEntry fromDto(LockOwnerDto dto) {
            Long id = dto.getPrimaryKey();
            String lockOwner = dto.getLockOwner();
            LocalDateTime timestamp = dto.getTimestamp();
            return ImmutableLockOwnerEntry.builder()
                                          .id(id)
                                          .lockOwner(lockOwner)
                                          .timestamp(timestamp)
                                          .build();
        }

        @Override
        public LockOwnerDto toDto(LockOwnerEntry entry) {
            long id = entry.getId();
            String lockOwner = entry.getLockOwner();
            LocalDateTime timestamp = entry.getTimestamp();
            return new LockOwnerDto(id, lockOwner, timestamp);
        }

    }
}
