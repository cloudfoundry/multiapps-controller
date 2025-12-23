package org.cloudfoundry.multiapps.controller.persistence.services;

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

    private final ApplicationShutdownMapper applicationShutdownMapper;

    public ApplicationShutdownService(EntityManagerFactory entityManagerFactory, ApplicationShutdownMapper applicationShutdownMapper) {
        super(entityManagerFactory);
        this.applicationShutdownMapper = applicationShutdownMapper;
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

    public ApplicationShutdown getApplicationShutdownInstanceByInstanceId(String instanceId, String applicationId) {
        return createQuery().id(instanceId)
                            .applicationId(applicationId)
                            .singleResult();
    }

    public ApplicationShutdown getApplicationsByApplicationIndex(int applicationInstanceIndex) {
        return createQuery().applicationInstanceIndex(applicationInstanceIndex)
                            .singleResult();
    }

    public int deleteApplicationsByIndex(int applicationInstanceIndex) {
        return createQuery().applicationInstanceIndex(applicationInstanceIndex)
                            .delete();
    }

    public ApplicationShutdown updateApplicationShutdownStatus(ApplicationShutdown oldApplicationShutdown, String status) {
        ApplicationShutdown newApplicationShutdown = ImmutableApplicationShutdown.copyOf(oldApplicationShutdown)
                                                                                 .withStatus(status);
        return update(oldApplicationShutdown, newApplicationShutdown);
    }
}
