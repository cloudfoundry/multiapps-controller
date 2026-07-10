package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.LoggingConfigurationQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.LoggingConfigurationQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.PersistenceObjectMapper;
import org.cloudfoundry.multiapps.controller.persistence.services.PersistenceService;

@Named("cloudLoggingServiceConfigurationService")
public class CloudLoggingServiceConfigurationService extends PersistenceService<LoggingConfiguration, LoggingConfigurationDto, String> {

    private final LoggingConfigurationMapper mapper = new LoggingConfigurationMapper();

    @Inject
    public CloudLoggingServiceConfigurationService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public LoggingConfigurationQuery createQuery() {
        return new LoggingConfigurationQueryImpl(createEntityManager(), mapper);
    }

    public LoggingConfiguration getLoggingConfiguration(String mtaSpace, String mtaId, String namespace) {
        return createQuery().mtaSpace(mtaSpace)
                            .mtaId(mtaId)
                            .namespace(namespace)
                            .list()
                            .stream()
                            .findFirst()
                            .orElse(null);
    }

    public List<LoggingConfiguration> getLoggingConfigurationsBySpace(String mtaSpaceId) {
        return createQuery().mtaSpaceId(mtaSpaceId)
                            .list();
    }

    public void deleteLoggingConfiguration(String id) {
        createQuery().id(id)
                     .delete();
    }

    @Override
    protected PersistenceObjectMapper<LoggingConfiguration, LoggingConfigurationDto> getPersistenceObjectMapper() {
        return mapper;
    }

    @Override
    protected void onEntityConflict(LoggingConfigurationDto dto, Throwable t) {
        throw new ConflictException(t, Messages.CLOUD_LOGGING_CONFIGURATION_ALREADY_EXISTS, dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(String primaryKey) {
        throw new NotFoundException(Messages.CLOUD_LOGGING_CONFIGURATION_NOT_FOUND, primaryKey);
    }

    public static class LoggingConfigurationMapper implements PersistenceObjectMapper<LoggingConfiguration, LoggingConfigurationDto> {

        @Override
        public LoggingConfigurationDto toDto(LoggingConfiguration config) {
            LoggingConfigurationDto dto = new LoggingConfigurationDto();
            dto.setPrimaryKey(config.getId());
            dto.setTargetSpace(config.getTargetSpace());
            dto.setTargetOrg(config.getTargetOrg());
            dto.setMtaId(config.getMtaId());
            dto.setMtaOrg(config.getMtaOrg());
            dto.setMtaSpace(config.getMtaSpace());
            dto.setMtaSpaceId(config.getMtaSpaceId());
            dto.setNamespace(config.getNamespace());
            dto.setServiceInstanceName(config.getServiceInstanceName());
            dto.setServiceKeyName(config.getServiceKeyName());
            dto.setLogLevel(config.getLogLevel());
            dto.setFailSafe(Boolean.TRUE.equals(config.isFailSafe()));
            dto.setAddedAt(LocalDateTime.now());
            return dto;
        }

        @Override
        public LoggingConfiguration fromDto(LoggingConfigurationDto dto) {
            return ImmutableLoggingConfiguration.builder()
                                                .id(dto.getPrimaryKey())
                                                .targetSpace(dto.getTargetSpace())
                                                .targetOrg(dto.getTargetOrg())
                                                .mtaId(dto.getMtaId())
                                                .mtaOrg(dto.getMtaOrg())
                                                .mtaSpace(dto.getMtaSpace())
                                                .mtaSpaceId(dto.getMtaSpaceId())
                                                .namespace(dto.getNamespace())
                                                .serviceInstanceName(dto.getServiceInstanceName())
                                                .serviceKeyName(dto.getServiceKeyName())
                                                .logLevel(dto.getLogLevel())
                                                .isFailSafe(dto.isFailSafe())
                                                .build();
        }
    }
}
