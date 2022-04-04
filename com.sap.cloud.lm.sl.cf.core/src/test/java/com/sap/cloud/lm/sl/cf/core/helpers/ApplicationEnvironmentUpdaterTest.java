package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
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
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class ApplicationEnvironmentUpdaterTest {

    private Input input;
    private Expectation expectation;
    private ApplicationEnvironmentUpdater applicationEnvironmentUpdater;
    private CloudControllerClient client = Mockito.mock(CloudControllerClient.class);

    public ApplicationEnvironmentUpdaterTest(String input, Expectation expectation) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, getClass()), Input.class);
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "application-env-updater-input-00.json", new Expectation(Expectation.Type.RESOURCE, "application-env-updater-result-00.json"),
            }
            ,
            {
                "application-env-updater-input-01.json", new Expectation(Expectation.Type.RESOURCE, "application-env-updater-result-01.json"),
            }
// @formatter:on
        });
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
        TestUtil.test(() -> captor.getValue(), expectation, getClass());
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
