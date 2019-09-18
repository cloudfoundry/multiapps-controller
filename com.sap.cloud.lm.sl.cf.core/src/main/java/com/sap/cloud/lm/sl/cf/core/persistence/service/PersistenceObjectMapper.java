package com.sap.cloud.lm.sl.cf.core.persistence.service;

import com.sap.cloud.lm.sl.cf.core.persistence.dto.DtoWithPrimaryKey;

public interface PersistenceObjectMapper<T, D extends DtoWithPrimaryKey<?>> {

    T fromDto(D dto);

    D toDto(T object);

}