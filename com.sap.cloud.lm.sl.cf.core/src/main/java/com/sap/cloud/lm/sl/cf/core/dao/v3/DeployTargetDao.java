package com.sap.cloud.lm.sl.cf.core.dao.v3;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.v3.DeployTargetDto;
import com.sap.cloud.lm.sl.mta.model.v3.Target;

@Component("deployTargetDaoV3")
public class DeployTargetDao extends com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, DeployTargetDto> {

    private static final String FIND_ALL_QUERY_NAME = "find_all_v3";

    @Autowired
    @Qualifier("deployTargetEntityManagerFactory")
    EntityManagerFactory emf;

    public DeployTargetDao() {
        super(DeployTargetDto.class, FIND_ALL_QUERY_NAME);
    }

    @Override
    protected EntityManagerFactory getEmf() {
        return emf;
    }

    @Override
    protected DeployTargetDto wrap(Target target) {
        return new DeployTargetDto(target);
    }

}
