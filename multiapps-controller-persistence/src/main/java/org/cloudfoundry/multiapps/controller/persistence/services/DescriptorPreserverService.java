package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutablePreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptorDto;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorPreserverQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.DescriptorPreserverQueryImpl;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

@Named
public class DescriptorPreserverService extends PersistenceService<PreservedDescriptor, PreservedDescriptorDto, Long> {

    @Inject
    protected DescriptorPreserverMapper descriptorPreserverMapper;

    public DescriptorPreserverService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public DescriptorPreserverQuery createQuery() {
        return new DescriptorPreserverQueryImpl(createEntityManager(), descriptorPreserverMapper);
    }

    @Override
    protected PersistenceObjectMapper<PreservedDescriptor, PreservedDescriptorDto> getPersistenceObjectMapper() {
        return descriptorPreserverMapper;
    }

    @Override
    protected void onEntityConflict(PreservedDescriptorDto dto, Throwable t) {
        throw new ConflictException(t,
                                    Messages.PRESERVED_DESCRIPTOR_FOR_MTA_ID_0_AND_ID_1_ALREADY_EXIST,
                                    dto.getMtaId(),
                                    dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.PRESERVED_DESCRIPTOR_WITH_ID_NOT_EXIST, id);
    }

    @Named
    public static class DescriptorPreserverMapper implements PersistenceObjectMapper<PreservedDescriptor, PreservedDescriptorDto> {

        @Override
        public PreservedDescriptor fromDto(PreservedDescriptorDto dto) {
            return ImmutablePreservedDescriptor.builder()
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
        public PreservedDescriptorDto toDto(PreservedDescriptor preservedDescriptor) {
            long id = preservedDescriptor.getId();
            DeploymentDescriptor descriptor = preservedDescriptor.getDescriptor();
            String mtaId = preservedDescriptor.getMtaId();
            String mtaVersion = preservedDescriptor.getMtaVersion()
                                                   .toString();
            String spaceId = preservedDescriptor.getSpaceId();
            String namespace = preservedDescriptor.getNamespace();
            String checksum = preservedDescriptor.getChecksum();
            LocalDateTime timestamp = preservedDescriptor.getTimestamp();
            return new PreservedDescriptorDto(id,
                                              serializeDescriptor(descriptor),
                                              mtaId,
                                              mtaVersion,
                                              spaceId,
                                              namespace,
                                              checksum,
                                              timestamp);
        }

    }

}
