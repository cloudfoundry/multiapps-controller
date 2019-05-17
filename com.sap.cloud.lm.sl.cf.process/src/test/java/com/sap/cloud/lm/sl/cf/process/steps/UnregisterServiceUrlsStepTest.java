package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UnregisterServiceUrlsStepTest extends SyncFlowableStepTest<UnregisterServiceUrlsStep> {

    private final String inputLocation;
    private final String[] expectedUnregisteredServiceUrls;

    private StepInput input;

    private class UnregisterServiceUrlsStepMock extends UnregisterServiceUrlsStep {
        @Override
        protected List<String> getRegisteredServiceUrlNames(ExecutionWrapper execution) {
            if (CollectionUtils.isEmpty(input.serviceUrlsToRegister)) {
                return Collections.emptyList();
            }
            return input.serviceUrlsToRegister.stream()
                .map(ServiceUrl::getServiceName)
                .collect(Collectors.toList());
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Unregister services with different names:
            {
                "unregister-service-urls-step-input-01.json", new String[] { "test-service-1", "test-service-2", },
            },
            // (1) Unregister services with the same name:
            {
                "unregister-service-urls-step-input-02.json", new String[] { "test-service-1", "test-service-1", },
            },
            // (2) Try to unregister service with register-service-url = false:
            {
                "unregister-service-urls-step-input-03.json", new String[] {},
            },
            // (3) A module that provides a service URL was renamed (the service URL was updated):
            {
                "unregister-service-urls-step-input-04.json", new String[] {},
            },
// @formatter:on
        });
    }

    public UnregisterServiceUrlsStepTest(String inputLocation, String[] expectedUnregisteredServiceUrls) {
        this.inputLocation = inputLocation;
        this.expectedUnregisteredServiceUrls = expectedUnregisteredServiceUrls;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    private void loadParameters() throws Exception {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, UnregisterServiceUrlsStep.class), StepInput.class);
    }

    private void prepareContext() {
        StepsUtil.setAppsToUndeploy(context, toCloudApplications(input.apps));
    }

    private List<CloudApplication> toCloudApplications(List<SimpleApplication> apps) {
        return apps.stream()
            .map((app) -> app.toCloudApplication())
            .collect(Collectors.toList());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        String[] deletedServiceUrls = captureStepOutput();

        assertArrayEquals(expectedUnregisteredServiceUrls, deletedServiceUrls);
    }

    private String[] captureStepOutput() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client, Mockito.times(expectedUnregisteredServiceUrls.length))
            .unregisterServiceURL(captor.capture());
        return captor.getAllValues()
            .toArray(new String[0]);
    }

    private static class StepInput {
        List<ServiceUrl> serviceUrlsToRegister;
        List<SimpleApplication> apps;
    }

    private static final MapToEnvironmentConverter ENV_CONVERTER = new MapToEnvironmentConverter(false);

    private static class SimpleApplication {

        String name;
        Map<String, Object> env;

        CloudApplication toCloudApplication() {
            CloudApplication app = new CloudApplication(new Meta(NameUtil.getUUID(name), null, null), name);
            app.setEnv(MapUtil.upcast(ENV_CONVERTER.asEnv(env)));
            return app;
        }

    }

    @Override
    protected UnregisterServiceUrlsStep createStep() {
        return new UnregisterServiceUrlsStepMock();
    }

}
