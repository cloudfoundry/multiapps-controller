package com.sap.cloud.lm.sl.cf.core.persistence.dto;

public interface DtoWithPrimaryKey<P> {

    P getPrimaryKey();

    void setPrimaryKey(P primaryKey);
}
