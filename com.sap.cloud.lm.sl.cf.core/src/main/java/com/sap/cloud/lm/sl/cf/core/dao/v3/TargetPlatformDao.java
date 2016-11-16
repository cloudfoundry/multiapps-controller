package com.sap.cloud.lm.sl.cf.core.dao.v3;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.v3.TargetPlatformDto;
import com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatform;

@Component("targetPlatformDaoV3")
public class TargetPlatformDao extends com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao {

    private static final String FIND_ALL_QUERY_NAME = "find_all_v3";

    @Autowired
    @Qualifier("targetPlatformEntityManagerFactory")
    EntityManagerFactory emf;

    public TargetPlatformDao() {
        super(TargetPlatformDto.class, FIND_ALL_QUERY_NAME);
    }

    @Override
    protected EntityManagerFactory getEmf() {
        return emf;
    }

    @Override
    protected TargetPlatformDto wrap(com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform platform) {
        return new TargetPlatformDto((TargetPlatform) platform);
    }

}
