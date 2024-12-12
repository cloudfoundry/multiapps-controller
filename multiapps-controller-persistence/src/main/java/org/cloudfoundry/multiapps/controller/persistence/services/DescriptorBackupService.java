package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptorDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorBackupQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.DescriptorBackupQueryImpl;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;

@Named
public class DescriptorBackupService extends PersistenceService<BackupDescriptor, BackupDescriptorDto, Long> {

    @Inject
    protected DescriptorBackupMapper descriptorBackupMapper;

    public DescriptorBackupService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public DescriptorBackupQuery createQuery() {
        return new DescriptorBackupQueryImpl(createEntityManager(), descriptorBackupMapper);
    }

    @Override
    protected PersistenceObjectMapper<BackupDescriptor, BackupDescriptorDto> getPersistenceObjectMapper() {
        return descriptorBackupMapper;
    }

    @Override
    protected void onEntityConflict(BackupDescriptorDto dto, Throwable t) {
        throw new ConflictException(t,
                                    Messages.BACKUP_DESCRIPTOR_FOR_MTA_ID_0_AND_ID_1_ALREADY_EXIST,
                                    dto.getMtaId(),
                                    dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.BACKUP_DESCRIPTOR_WITH_ID_NOT_EXIST, id);
    }

    @Named
    public static class DescriptorBackupMapper implements PersistenceObjectMapper<BackupDescriptor, BackupDescriptorDto> {

        @Override
        public BackupDescriptor fromDto(BackupDescriptorDto dto) {
            return ImmutableBackupDescriptor.builder()
                                            .id(dto.getPrimaryKey())
                                            .descriptor(getDescriptor(dto.getdescriptor()))
                                            .mtaId(dto.getMtaId())
                                            .mtaVersion(dto.getMtaVersion())
                                            .spaceId(dto.getSpaceId())
                                            .namespace(dto.getNamespace())
                                            .checksum(dto.getChecksum())
                                            .timestamp(dto.getTimestamp())
                                            .build();
        }

        private DeploymentDescriptor getDescriptor(byte[] descriptor) {
            return JsonUtil.fromJsonBinary(descriptor, DeploymentDescriptor.class);
        }

        private byte[] serializeDescriptor(DeploymentDescriptor descriptor) {
            return JsonUtil.toJsonBinary(descriptor);
        }

        @Override
        public BackupDescriptorDto toDto(BackupDescriptor backupDescriptor) {
            long id = backupDescriptor.getId();
            DeploymentDescriptor descriptor = backupDescriptor.getDescriptor();
            String mtaId = backupDescriptor.getMtaId();
            String mtaVersion = backupDescriptor.getMtaVersion()
                                                .toString();
            String spaceId = backupDescriptor.getSpaceId();
            String namespace = backupDescriptor.getNamespace();
            String checksum = backupDescriptor.getChecksum();
            LocalDateTime timestamp = backupDescriptor.getTimestamp();
            return new BackupDescriptorDto(id, serializeDescriptor(descriptor), mtaId, mtaVersion, spaceId, namespace, checksum, timestamp);
        }

    }

}
