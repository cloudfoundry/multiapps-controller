package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataValidator;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class DeployedMtaEnvDetectorTest {

    private final Tester tester = Tester.forClass(getClass());

    @Mock
    private CloudControllerClient client;

    private DeployedMtaEnvDetector deployedMtaEnvDetector = new DeployedMtaEnvDetector(new EnvMtaMetadataParser(new EnvMtaMetadataValidator()));

    public static Stream<Arguments> testGetAllDeployedMtas() {
        return Stream.of(
// @formatter:off
            // (1) No MTA applications:
            Arguments.of("env/apps-01.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-01.json")),
            // (2) No applications:
            Arguments.of("env/apps-02.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-02.json")),
            // (3) Applications without MTA_PROVIDED_DEPENDENCY_NAMES:
            Arguments.of("env/apps-03.json", new Expectation(Expectation.Type.EXCEPTION, "MTA metadata for application \"mta-application-1\" is incomplete. This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application.")),
            // (4) Applications without MTA_MODULE_METADATA:
            Arguments.of("env/apps-04.json", new Expectation(Expectation.Type.EXCEPTION, "MTA metadata for application \"mta-application-1\" is incomplete. This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application.")),
            // (5) Applications without MTA_SERVICES:
            Arguments.of("env/apps-05.json", new Expectation(Expectation.Type.EXCEPTION, "MTA metadata for application \"mta-application-1\" is incomplete. This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application.")),
            // (6) Applications without MTA_METADATA:
            Arguments.of("env/apps-06.json", new Expectation(Expectation.Type.EXCEPTION, "MTA metadata for application \"mta-application-1\" is incomplete. This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application.")),
            // (7) Two MTA applications:
            Arguments.of("env/apps-07.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-07.json")),
            // (8) Applications from different versions of the same MTA:
            Arguments.of("env/apps-08.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-08.json")),
            // (9) Applications from different versions of the same MTA (same modules):
            Arguments.of("env/apps-09.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-09.json")),
            // (10) Applications with provided dependencies stored using the old format:
            Arguments.of("env/apps-10.json", new Expectation(Expectation.Type.JSON, "env/deployed-mtas-10.json")),
            // (11) Application with invalid MTA_MODULE_METADATA:
            Arguments.of("env/apps-11.json", new Expectation(Expectation.Type.EXCEPTION, "The environment of application \"mta-application\" contains an invalid value for \"mta-application\". This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application.")),
            // (12) Application with invalid MTA_METADATA:
            Arguments.of("env/apps-12.json", new Expectation(Expectation.Type.EXCEPTION, "The environment of application \"mta-application\" contains an invalid value for \"mta-application\". This indicates that MTA reserved variables in the applications environment were modified manually. Either revert the changes or delete the application."))
// @formatter:on
        );
    }

    @BeforeEach
    private void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest
    @MethodSource
    public void testGetAllDeployedMtas(String appsResourceLocation, Expectation expectation) throws IOException {
        List<CloudApplication> apps = parseApps(appsResourceLocation);
        prepareClient(apps);
        tester.test(() -> deployedMtaEnvDetector.detectDeployedMtas(client), expectation);
    }

    private List<CloudApplication> parseApps(String appsResourceLocation) throws IOException {
        String appsJson = TestUtil.getResourceAsString(appsResourceLocation, getClass());
        List<TestCloudApplication> testApps = JsonUtil.fromJson(appsJson, new TypeReference<List<TestCloudApplication>>() {
        });
        return toCloudApplications(testApps);
    }

    private List<CloudApplication> toCloudApplications(List<TestCloudApplication> simpleApplications) {
        return simpleApplications.stream()
                                 .map(TestCloudApplication::toCloudApplication)
                                 .collect(Collectors.toList());
    }

    private void prepareClient(List<CloudApplication> apps) {
        Mockito.doReturn(apps)
               .when(client)
               .getApplications();
    }

    private static final MapToEnvironmentConverter ENV_CONVERTER = new MapToEnvironmentConverter(false);

    private static class TestCloudApplication {

        private String name;
        private Map<String, Object> env;

        private CloudApplication toCloudApplication() {
            return ImmutableCloudApplication.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(NameUtil.getUUID(name))
                                                                            .build())
                                            .name(name)
                                            .env(ENV_CONVERTER.asEnv(env))
                                            .build();
        }

    }

}
