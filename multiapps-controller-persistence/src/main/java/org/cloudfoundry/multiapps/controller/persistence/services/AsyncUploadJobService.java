package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.AsyncUploadJobDto;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.AsyncUploadJobsQueryImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

@Named
public class AsyncUploadJobService extends PersistenceService<AsyncUploadJobEntry, AsyncUploadJobDto, String> {

    @Inject
    public AsyncUploadJobService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public AsyncUploadJobsQuery createQuery() {
        return new AsyncUploadJobsQueryImpl(createEntityManager());
    }

    @Override
    protected PersistenceObjectMapper<AsyncUploadJobEntry, AsyncUploadJobDto> getPersistenceObjectMapper() {
        return new AsyncUploadJobMapper();
    }

    @Override
    protected void onEntityConflict(AsyncUploadJobDto dto, Throwable t) {
        throw new ConflictException(t, Messages.ASYNC_UPLOAD_JOB_ALREADY_EXISTS, dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(String primaryKey) {
        throw new NotFoundException(Messages.ASYNC_UPLOAD_JOB_NOT_FOUND, primaryKey);
    }

    public static class AsyncUploadJobMapper implements PersistenceObjectMapper<AsyncUploadJobEntry, AsyncUploadJobDto> {

        @Override
        public AsyncUploadJobEntry fromDto(AsyncUploadJobDto dto) {
            return ImmutableAsyncUploadJobEntry.builder()
                                               .id(dto.getPrimaryKey())
                                               .user(dto.getMtaUser())
                                               .state(AsyncUploadJobEntry.State.valueOf(dto.getState()))
                                               .startedAt(dto.getStartedAt())
                                               .finishedAt(dto.getFinishedAt())
                                               .namespace(dto.getNamespace())
                                               .spaceGuid(dto.getSpaceGuid())
                                               .url(dto.getUrl())
                                               .mtaId(dto.getMtaId())
                                               .fileId(dto.getFileId())
                                               .error(dto.getError())
                                               .instanceIndex(dto.getInstanceIndex())
                                               .build();
        }

        @Override
        public AsyncUploadJobDto toDto(AsyncUploadJobEntry entry) {
            return new AsyncUploadJobDto(entry.getId(),
                                         entry.getUser(),
                                         entry.getState()
                                              .toString(),
                                         entry.getUrl(),
                                         entry.getStartedAt(),
                                         entry.getFinishedAt(),
                                         entry.getNamespace(),
                                         entry.getSpaceGuid(),
                                         entry.getMtaId(),
                                         entry.getFileId(),
                                         entry.getError(),
                                         entry.getInstanceIndex());
        }
    }

}
