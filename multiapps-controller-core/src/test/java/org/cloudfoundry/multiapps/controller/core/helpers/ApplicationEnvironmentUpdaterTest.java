package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

class ApplicationEnvironmentUpdaterTest {

    private final Tester tester = Tester.forClass(getClass());

    private final CloudControllerClient client = Mockito.mock(CloudControllerClient.class);

    public static Stream<Arguments> testUpdateEnv() {
        return Stream.of(
// @formatter:off
            Arguments.of("application-env-updater-input-00.json", new Expectation(Expectation.Type.JSON, "application-env-updater-result-00.json")),
            Arguments.of("application-env-updater-input-01.json", new Expectation(Expectation.Type.JSON, "application-env-updater-result-01.json"))
// @formatter:on
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ParameterizedTest
    @MethodSource
    void testUpdateEnv(String filename, Expectation expectation) {
        Input input = JsonUtil.fromJson(TestUtil.getResourceAsString(filename, getClass()), Input.class);
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .metadata(CloudMetadata.defaultMetadata())
                                                        .name(input.app.name)
                                                        .state(CloudApplication.State.STOPPED)
                                                        .lifecycle(ImmutableLifecycle.builder()
                                                                                     .type(LifecycleType.DOCKER)
                                                                                     .build())
                                                        .build();
        ApplicationEnvironmentUpdater applicationEnvironmentUpdater = new ApplicationEnvironmentUpdater(app, ENV_CONVERTER.asEnv(input.app.env),
                                                                                                        client).withPrettyPrinting(false);
        applicationEnvironmentUpdater.updateApplicationEnvironment(input.envPropertyKey, input.newKey, input.newValue);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(client)
               .updateApplicationEnv(Mockito.eq(input.app.name), (Map<String, String>) captor.capture());
        tester.test(captor::getValue, expectation);
    }

    private static class Input {
        SimpleApp app;
        String envPropertyKey;
        String newKey;
        String newValue;
    }

    private static final MapToEnvironmentConverter ENV_CONVERTER = new MapToEnvironmentConverter(false);

    private static class SimpleApp {
        String name;
        Map<String, Object> env;
    }
}
