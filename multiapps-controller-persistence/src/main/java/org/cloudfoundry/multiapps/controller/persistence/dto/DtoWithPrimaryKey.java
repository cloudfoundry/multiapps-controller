package org.cloudfoundry.multiapps.controller.persistence.dto;

public interface DtoWithPrimaryKey<P> {

    P getPrimaryKey();

    void setPrimaryKey(P primaryKey);
}
