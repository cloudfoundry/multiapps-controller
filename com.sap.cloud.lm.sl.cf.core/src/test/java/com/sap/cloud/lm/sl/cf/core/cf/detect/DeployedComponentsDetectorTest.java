package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class DeployedComponentsDetectorTest {

    @ParameterizedTest
    @MethodSource
    public void testDetectAllApplications(String appsResourceLocation, String expected) throws IOException {
        List<CloudApplication> apps = parseApps(appsResourceLocation);
        TestUtil.test(() -> new DeployedComponentsDetector().detectAllDeployedComponents(apps), expected, getClass());
    }

    private List<CloudApplication> parseApps(String appsResourceLocation) throws IOException {
        String appsJson = TestUtil.getResourceAsString(appsResourceLocation, getClass());
        List<TestCloudApplication> testApps = JsonUtil.fromJson(appsJson, new TypeToken<List<TestCloudApplication>>() {
        }.getType());
        return toCloudApplications(testApps);
    }

    private List<CloudApplication> toCloudApplications(List<TestCloudApplication> simpleApplications) {
        return simpleApplications.stream()
            .map(TestCloudApplication::toCloudApplication)
            .collect(Collectors.toList());
    }

    public static Stream<Arguments> testDetectAllApplications() {
        return Stream.of(
// @formatter:off
            // (1) No MTA applications:
            Arguments.of("apps-01.json", "R:deployed-components-01.json"),
            // (2) No applications:
            Arguments.of("apps-02.json", "R:deployed-components-02.json"),
            // (3) Applications without MTA_PROVIDED_DEPENDENCY_NAMES:
            Arguments.of("apps-03.json", "E:Cannot parse MTA metadata for application \"mta-application-1\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application."),
            // (4) Applications without MTA_MODULE_METADATA:
            Arguments.of("apps-04.json", "E:Cannot parse MTA metadata for application \"mta-application-1\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application."),
            // (5) Applications without MTA_SERVICES:
            Arguments.of("apps-05.json", "E:Cannot parse MTA metadata for application \"mta-application-1\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application."),
            // (6) Applications without MTA_METADATA:
            Arguments.of("apps-06.json", "E:Cannot parse MTA metadata for application \"mta-application-1\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application."),
            // (7) Two MTA applications:
            Arguments.of("apps-07.json", "R:deployed-components-07.json"),
            // (8) Applications from different versions of the same MTA:
            Arguments.of("apps-08.json", "R:deployed-components-08.json"),
            // (9) Applications from different versions of the same MTA (same modules):
            Arguments.of("apps-09.json", "R:deployed-components-09.json"),
            // (10) Applications with provided dependencies stored using the old format:
            Arguments.of("apps-10.json", "R:deployed-components-10.json"),
            // (11) Application with invalid MTA_MODULE_METADATA:
            Arguments.of("apps-11.json", "E:Cannot parse MTA metadata for application \"mta-application\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application."),
            // (12) Application with invalid MTA_METADATA:
            Arguments.of("apps-12.json", "E:Cannot parse MTA metadata for application \"mta-application\". This indicates that MTA reserved variables in the application's environment were modified manually. Either revert the changes or delete the application.")
// @formatter:on
        );
    }

    private static class TestCloudApplication {

        private Map<Object, Object> env;
        private String name;

        private CloudApplication toCloudApplication() {
            CloudApplication app = new CloudApplication(new Meta(NameUtil.getUUID(name), null, null), name);
            app.setEnv(env);
            return app;
        }

    }

}
