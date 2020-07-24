package org.cloudfoundry.multiapps.controller.core.persistence.dto;

public interface DtoWithPrimaryKey<P> {

    P getPrimaryKey();

    void setPrimaryKey(P primaryKey);
}
