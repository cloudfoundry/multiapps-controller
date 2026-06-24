package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.cloudfoundry.multiapps.controller.persistence.TransactionalExecutor;
import org.cloudfoundry.multiapps.controller.persistence.dto.LoggingConfigurationDto;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;

@Named("cloudLoggingServiceConfigurationService")
public class CloudLoggingServiceConfigurationService {

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public CloudLoggingServiceConfigurationService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void storeCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        executeInTransaction(manager -> {
            LoggingConfigurationDto dto = toDto(loggingConfiguration);
            dto.setAddedAt(LocalDateTime.now());
            manager.persist(dto);
            return null;
        });
    }

    public LoggingConfiguration getCloudLoggingServiceConfiguration(String mtaSpace, String mtaId, String namespace) {
        return executeInEntityManager(manager -> {
            TypedQuery<LoggingConfigurationDto> query;
            if (namespace == null) {
                query = manager.createNamedQuery(LoggingConfigurationDto.NamedQueries.FIND_BY_MTA_SPACE_ID_NULL_NAMESPACE,
                                                 LoggingConfigurationDto.class);
            } else {
                query = manager.createNamedQuery(LoggingConfigurationDto.NamedQueries.FIND_BY_MTA_SPACE_ID_NAMESPACE,
                                                 LoggingConfigurationDto.class);
                query.setParameter("namespace", namespace);
            }
            query.setParameter("mtaSpace", mtaSpace);
            query.setParameter("mtaId", mtaId);
            try {
                return fromDto(query.getSingleResult());
            } catch (NoResultException e) {
                return null;
            }
        });
    }

    public void deleteCloudLoggingServiceConfiguration(String id) {
        executeInTransaction(manager -> {
            LoggingConfigurationDto dto = manager.find(LoggingConfigurationDto.class, id);
            if (dto != null) {
                manager.remove(dto);
            }
            return null;
        });
    }

    public void updateCloudLoggingServiceConfiguration(LoggingConfiguration loggingConfiguration) {
        executeInTransaction(manager -> {
            TypedQuery<LoggingConfigurationDto> query;
            String namespace = loggingConfiguration.getNamespace();
            if (namespace == null) {
                query = manager.createNamedQuery(LoggingConfigurationDto.NamedQueries.FIND_BY_MTA_SPACE_ID_NULL_NAMESPACE,
                                                 LoggingConfigurationDto.class);
            } else {
                query = manager.createNamedQuery(LoggingConfigurationDto.NamedQueries.FIND_BY_MTA_SPACE_ID_NAMESPACE,
                                                 LoggingConfigurationDto.class);
                query.setParameter("namespace", namespace);
            }
            query.setParameter("mtaSpace", loggingConfiguration.getMtaSpace());
            query.setParameter("mtaId", loggingConfiguration.getMtaId());
            LoggingConfigurationDto existing;
            try {
                existing = query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
            existing.setTargetSpace(loggingConfiguration.getTargetSpace());
            existing.setTargetOrg(loggingConfiguration.getTargetOrg());
            existing.setServiceInstanceName(loggingConfiguration.getServiceInstanceName());
            existing.setServiceKeyName(loggingConfiguration.getServiceKeyName());
            existing.setLogLevel(loggingConfiguration.getLogLevel());
            existing.setFailSafe(Boolean.TRUE.equals(loggingConfiguration.isFailSafe()));
            existing.setAddedAt(LocalDateTime.now());
            return null;
        });
    }

    public List<LoggingConfiguration> getAllCloudLoggingServiceConfigurationsFromSpace(String spaceId) {
        return executeInEntityManager(manager -> manager.createNamedQuery(LoggingConfigurationDto.NamedQueries.FIND_ALL_BY_MTA_SPACE_ID,
                                                                          LoggingConfigurationDto.class)
                                                        .setParameter("mtaSpaceId", spaceId)
                                                        .getResultList()
                                                        .stream()
                                                        .map(CloudLoggingServiceConfigurationService::fromDto)
                                                        .toList());
    }

    private <R> R executeInTransaction(Function<EntityManager, R> function) {
        return new TransactionalExecutor<R>(entityManagerFactory.createEntityManager()).execute(function);
    }

    private <R> R executeInEntityManager(Function<EntityManager, R> function) {
        try (EntityManager manager = entityManagerFactory.createEntityManager()) {
            return function.apply(manager);
        }
    }

    private static LoggingConfigurationDto toDto(LoggingConfiguration config) {
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
        return dto;
    }

    private static LoggingConfiguration fromDto(LoggingConfigurationDto dto) {
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
