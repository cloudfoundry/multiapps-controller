package com.sap.cloud.lm.sl.cf.core.dao.v2;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.v2.DeployTargetDto;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;

@Component("deployTargetDaoV2")
public class DeployTargetDao extends com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, DeployTargetDto> {

    private static final String FIND_ALL_QUERY_NAME = "find_all_v2";

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
