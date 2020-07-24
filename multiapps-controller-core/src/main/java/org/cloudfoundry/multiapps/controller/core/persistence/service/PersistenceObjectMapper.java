package org.cloudfoundry.multiapps.controller.core.persistence.service;

import org.cloudfoundry.multiapps.controller.core.persistence.dto.DtoWithPrimaryKey;

public interface PersistenceObjectMapper<T, D extends DtoWithPrimaryKey<?>> {

    T fromDto(D dto);

    D toDto(T object);

}