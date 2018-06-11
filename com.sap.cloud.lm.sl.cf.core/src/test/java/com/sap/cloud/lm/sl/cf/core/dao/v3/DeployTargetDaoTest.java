package com.sap.cloud.lm.sl.cf.core.dao.v3;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;

import java.util.Arrays;

import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.handlers.v3.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v3.Target;

public class DeployTargetDaoTest extends com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDaoTest {

    public DeployTargetDaoTest(Input input, String exceptionMessage) {
        super(input, exceptionMessage);
        resource = "/platform/platform-v3.json";
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao getDeployTargetDao() {
        return new DeployTargetDao(EMF);
    }

    @Override
    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    @Override
    protected Target loadDeployTarget(String deployTargetJsonPath) throws Exception {
        String json = getResourceAsString(deployTargetJsonPath, getClass());
        return getConfigurationParser().parseTargetJson(json);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Add new deploy target:
            {
                 new AddInput("/platform/platform-v3-1.json"), "Deploy target with name \"DEPLOY-TARGET-TEST\" already exists"
            },
            // (1) Remove deploy target:
            {
                 new RemoveInput(0), "Deploy target with id \"0\" does not exist"
            },
            // (2) Merge deploy targets
            {
                new MergeInput("/platform/platform-v3-2.json"), null
            },
// @formatter:on
        });
    }

}
