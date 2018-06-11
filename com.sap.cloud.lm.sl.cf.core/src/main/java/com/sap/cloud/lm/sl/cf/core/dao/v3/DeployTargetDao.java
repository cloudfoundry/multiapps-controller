package com.sap.cloud.lm.sl.cf.core.dao.v3;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.v3.DeployTargetDto;
import com.sap.cloud.lm.sl.mta.model.v3.Target;

@Component("deployTargetDaoV3")
public class DeployTargetDao extends com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, DeployTargetDto> {

    private static final String FIND_ALL_QUERY_NAME = "find_all_v3";

    @Inject
    public DeployTargetDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory, DeployTargetDto.class, FIND_ALL_QUERY_NAME);
    }

    @Override
    protected DeployTargetDto wrap(Target target) {
        return new DeployTargetDto(target);
    }

}
