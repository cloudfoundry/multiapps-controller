package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.PersistenceObjectMapper;
import org.cloudfoundry.multiapps.controller.persistence.services.PersistenceService;

@Named("cloudLoggingServiceConfigurationService")
public class CloudLoggingServiceConfigurationService extends PersistenceService<LoggingConfiguration, LoggingConfigurationDto, String> {

    private final LoggingConfigurationMapper mapper = new LoggingConfigurationMapper();

    @Inject
    public CloudLoggingServiceConfigurationService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public void storeCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        add(loggingConfiguration);
    }

    public LoggingConfiguration getCloudLoggingServiceConfiguration(String mtaSpace, String mtaId, String namespace) {
        try (EntityManager manager = createEntityManager()) {
            return findByMtaSpaceIdAndNamespace(manager, mtaSpace, mtaId, namespace).getResultStream()
                                                                                    .findFirst()
                                                                                    .map(mapper::fromDto)
                                                                                    .orElse(null);
        }
    }

    public void deleteCloudLoggingServiceConfiguration(String id) {
        try (EntityManager manager = createEntityManager()) {
            manager.getTransaction()
                   .begin();
            LoggingConfigurationDto dto = manager.find(LoggingConfigurationDto.class, id);
            if (dto != null) {
                manager.remove(dto);
            }
            manager.getTransaction()
                   .commit();
        }
    }

    public void updateCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        try (EntityManager manager = createEntityManager()) {
            manager.getTransaction()
                   .begin();
            findByMtaSpaceIdAndNamespace(manager, loggingConfiguration.getMtaSpace(), loggingConfiguration.getMtaId(),
                                         loggingConfiguration.getNamespace()).getResultStream()
                                                                             .findFirst()
                                                                             .ifPresent(existing -> {
                                                                                 existing.setTargetSpace(
                                                                                     loggingConfiguration.getTargetSpace());
                                                                                 existing.setTargetOrg(loggingConfiguration.getTargetOrg());
                                                                                 existing.setServiceInstanceName(
                                                                                     loggingConfiguration.getServiceInstanceName());
                                                                                 existing.setServiceKeyName(
                                                                                     loggingConfiguration.getServiceKeyName());
                                                                                 existing.setLogLevel(loggingConfiguration.getLogLevel());
                                                                                 existing.setFailSafe(Boolean.TRUE.equals(
                                                                                     loggingConfiguration.isFailSafe()));
                                                                                 existing.setAddedAt(LocalDateTime.now());
                                                                             });
            manager.getTransaction()
                   .commit();
        }
    }

    public List<LoggingConfiguration> getAllCloudLoggingServiceConfigurationsFromSpace(String spaceId) {
        try (EntityManager manager = createEntityManager()) {
            return manager.createQuery("SELECT c FROM LoggingConfigurationDto c WHERE c.mtaSpaceId = :mtaSpaceId",
                                       LoggingConfigurationDto.class)
                          .setParameter(AttributeNames.MTA_SPACE_ID, spaceId)
                          .getResultStream()
                          .map(mapper::fromDto)
                          .toList();
        }
    }

    private TypedQuery<LoggingConfigurationDto> findByMtaSpaceIdAndNamespace(EntityManager manager, String mtaSpace, String mtaId,
                                                                             String namespace) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<LoggingConfigurationDto> query = builder.createQuery(LoggingConfigurationDto.class);
        Root<LoggingConfigurationDto> root = query.from(LoggingConfigurationDto.class);
        Predicate namespacePredicate = namespace == null ? builder.isNull(root.get(AttributeNames.NAMESPACE))
            : builder.equal(root.get(AttributeNames.NAMESPACE), namespace);
        return manager.createQuery(query.select(root)
                                        .where(builder.equal(root.get(AttributeNames.MTA_SPACE), mtaSpace),
                                               builder.equal(root.get(AttributeNames.MTA_ID), mtaId), namespacePredicate));
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

    private static class LoggingConfigurationMapper implements PersistenceObjectMapper<LoggingConfiguration, LoggingConfigurationDto> {

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
