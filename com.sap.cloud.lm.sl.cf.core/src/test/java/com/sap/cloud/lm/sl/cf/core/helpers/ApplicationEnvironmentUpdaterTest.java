package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ApplicationEnvironmentUpdaterTest {

    private Input input;
    private String expectedResult;
    private ApplicationEnvironmentUpdater applicationEnvironmentUpdater;
    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "application-env-updater-input-00.json", "R:application-env-updater-result-00.json"
            }
            ,
            {
                "application-env-updater-input-01.json", "R:application-env-updater-result-01.json"
            }
// @formatter:on
        });
    }

    public ApplicationEnvironmentUpdaterTest(String input, String expectedResult) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, getClass()), Input.class);
        this.expectedResult = expectedResult;
    }

    @Before
    public void prepare() {
        applicationEnvironmentUpdater = new ApplicationEnvironmentUpdater(input.app.toCloudApplication(), client).withPrettyPrinting(false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUpdateEnv() {
        applicationEnvironmentUpdater.updateApplicationEnvironment(input.envPropertyKey, input.newKey, input.newValue);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client)
            .updateApplicationEnv(Mockito.eq(input.app.name), (Map<String, String>) captor.capture());
        TestUtil.test(() -> captor.getValue(), expectedResult, getClass());
    }

    private static class Input {
        SimpleApp app;
        String envPropertyKey;
        String newKey;
        String newValue;
    }

    private static class SimpleApp {
        String name;
        Map<Object, Object> env;

        CloudApplication toCloudApplication() {
            CloudApplication app = new CloudApplication(Meta.defaultMeta(), name);
            app.setEnv(env);
            return app;
        }
    }
}
