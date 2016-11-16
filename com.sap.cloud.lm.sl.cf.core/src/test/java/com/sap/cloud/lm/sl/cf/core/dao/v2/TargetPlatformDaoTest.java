package com.sap.cloud.lm.sl.cf.core.dao.v2;

import java.util.Arrays;

import javax.persistence.Persistence;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser;

public class TargetPlatformDaoTest extends com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDaoTest {

    public TargetPlatformDaoTest(Input input, String exceptionMessage) {
        super(input, exceptionMessage);
        resource = "/platform/platform-v2.json";
    }

    @Override
    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    @Override
    protected TargetPlatformDao getTargetPlatformDao() {
        com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao dao = new com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao();
        dao.emf = Persistence.createEntityManagerFactory("TargetPlatformManagement");
        return dao;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Add new target platform:
            {
                new AddInput("/platform/platform-v2-1.json"), "Target platform with name \"TARGET-PLATFORM-TEST\" already exists"
            },
            // (1) Remove target platform:
            {
                new RemoveInput("TARGET-PLATFORM-TEST"), "Target platform with name \"TARGET-PLATFORM-TEST\" does not exist"
            },
            // (2) Merge target platforms
            {
                new MergeInput("TARGET-PLATFORM-TEST", "/platform/platform-v2-2.json"), null
            },
// @formatter:on
        });
    }

}
