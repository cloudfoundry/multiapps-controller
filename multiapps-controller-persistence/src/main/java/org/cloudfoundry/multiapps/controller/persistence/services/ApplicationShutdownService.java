package org.cloudfoundry.multiapps.controller.persistence.services;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdownDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.query.ApplicationShutdownQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.ApplicationShutdownQueryImpl;

@Named
public class ApplicationShutdownService extends PersistenceService<ApplicationShutdown, ApplicationShutdownDto, String> {

    @Inject
    protected ApplicationShutdownMapper applicationShutdownMapper;

    protected ApplicationShutdownService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected PersistenceObjectMapper<ApplicationShutdown, ApplicationShutdownDto> getPersistenceObjectMapper() {
        return applicationShutdownMapper;
    }

    public ApplicationShutdownQuery createQuery() {
        return new ApplicationShutdownQueryImpl(createEntityManager(), applicationShutdownMapper);
    }

    @Override
    protected void onEntityConflict(ApplicationShutdownDto dto, Throwable t) {
        throw new ConflictException(t, Messages.APPLICATION_SHUTDOWN_WITH_APPLICATION_INSTANCE_ID_ALREADY_EXIST, dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(String primaryKey) {
        throw new NotFoundException(Messages.APPLICATION_SHUTDOWN_WITH_APPLICATION_INSTANCE_ID_DOES_NOT_EXIST, primaryKey);
    }

    @Named
    public static class ApplicationShutdownMapper implements PersistenceObjectMapper<ApplicationShutdown, ApplicationShutdownDto> {

        @Override
        public ApplicationShutdown fromDto(ApplicationShutdownDto dto) {
            return ImmutableApplicationShutdown.builder()
                                               .applicationId(dto.getАpplicationId())
                                               .applicationInstanceId(dto.getPrimaryKey())
                                               .applicationInstanceIndex(dto.getАpplicationIndex())
                                               .build();
        }

        @Override
        public ApplicationShutdownDto toDto(ApplicationShutdown object) {
            return new ApplicationShutdownDto(object.getApplicationId(), object.getApplicationInstanceId(),
                                              object.getApplicationInstanceIndex());
        }
    }
}
