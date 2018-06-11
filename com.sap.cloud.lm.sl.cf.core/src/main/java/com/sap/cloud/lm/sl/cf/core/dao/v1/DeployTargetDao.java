package com.sap.cloud.lm.sl.cf.core.dao.v1;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@Component("deployTargetDaoV1")
public class DeployTargetDao
    extends com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, com.sap.cloud.lm.sl.cf.core.dto.persistence.v1.DeployTargetDto> {

    private static final String FIND_ALL_QUERY_NAME = "find_all_v1";

    @Inject
    public DeployTargetDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory, DeployTargetDto.class, FIND_ALL_QUERY_NAME);
    }

    @Override
    protected DeployTargetDto wrap(Target target) {
        return new DeployTargetDto(target);
    }

}
