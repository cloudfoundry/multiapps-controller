package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.controller.persistence.dto.DtoWithPrimaryKey;

public interface PersistenceObjectMapper<T, D extends DtoWithPrimaryKey<?>> {

    T fromDto(D dto);

    D toDto(T object);

}