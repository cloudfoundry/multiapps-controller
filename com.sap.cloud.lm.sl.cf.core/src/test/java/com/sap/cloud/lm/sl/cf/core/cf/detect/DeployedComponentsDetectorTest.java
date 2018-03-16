package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeployedComponentsDetectorTest {

    private static class SimpleApplication {

        private Map<Object, Object> env;
        private String name;

        CloudApplication toCloudApplication() {
            CloudApplication application = new CloudApplication(new Meta(NameUtil.getUUID(name), null, null), name);
            application.setEnv(env);
            return application;
        }

    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No MTA applications:
            {
                "apps-01.json", "R:deployed-components-01.json",
            },
            // (1) No applications:
            {
                "apps-02.json", "R:deployed-components-02.json",
            },
            // (2) Applications with no provided dependencies, services and module metadata:
            {
                "apps-03.json", "R:deployed-components-03.json",
            },
            // (3) Applications with no provided dependencies and services:
            {
                "apps-04.json", "R:deployed-components-04.json",
            },
            // (4) Applications with no services, but with provided dependencies stored using the old format:
            {
                "apps-05.json", "R:deployed-components-04.json",
            },
            // (5) Applications with no services, but with provided dependencies:
            {
                "apps-06.json", "R:deployed-components-06.json",
            },
            // (6) Applications with services:
            {
                "apps-07.json", "R:deployed-components-07.json",
            },
            // (7) Applications from different versions of the same MTA:
            {
                "apps-08.json", "R:deployed-components-08.json",
            },
            // (8) Applications from different versions of the same MTA (same modules):
            {
                "apps-09.json", "R:deployed-components-09.json",
            },
            // (9) Applications with deploy attributes:
            {
                "apps-10.json", "R:deployed-components-10.json",
            },
// @formatter:on
        });
    }

    List<CloudApplication> apps;

    private String appsLocation;
    private String expected;

    public DeployedComponentsDetectorTest(String appsLocation, String expected) {
        this.appsLocation = appsLocation;
        this.expected = expected;
    }

    @Before
    public void loadParameters() throws Exception {
        String appsJson = TestUtil.getResourceAsString(appsLocation, getClass());
        List<SimpleApplication> simpleApps = JsonUtil.fromJson(appsJson, new TypeToken<List<SimpleApplication>>() {
        }.getType());
        this.apps = toCloudApplications(simpleApps);
    }

    private List<CloudApplication> toCloudApplications(List<SimpleApplication> simpleApplications) {
        return simpleApplications.stream()
            .map((application) -> application.toCloudApplication())
            .collect(Collectors.toList());
    }

    @Test
    public void testDetectAllApplications() {
        TestUtil.test(() -> new DeployedComponentsDetector().detectAllDeployedComponents(this.apps), expected, getClass());
    }

}
