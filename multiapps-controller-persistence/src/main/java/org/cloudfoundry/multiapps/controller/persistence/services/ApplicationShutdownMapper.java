package org.cloudfoundry.multiapps.controller.persistence.services;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdownDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;

@Named
public class ApplicationShutdownMapper implements PersistenceObjectMapper<ApplicationShutdown, ApplicationShutdownDto> {

    @Override
    public ApplicationShutdown fromDto(ApplicationShutdownDto dto) {
        return ImmutableApplicationShutdown.builder()
                                           .id(dto.getPrimaryKey())
                                           .applicationId(dto.getАpplicationId())
                                           .applicationInstanceIndex(dto.getАpplicationIndex())
                                           .status(dto.getShutdownStatus())
                                           .staredAt(dto.getStartedAt())
                                           .build();
    }

    @Override
    public ApplicationShutdownDto toDto(ApplicationShutdown object) {
        return new ApplicationShutdownDto(object.getId(), object.getApplicationId(), object.getApplicationInstanceIndex(),
                                          object.getStatus(), object.getStaredAt());
    }
}
