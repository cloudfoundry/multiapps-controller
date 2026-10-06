package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application.V3Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application.V3LifecycleData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ApplicationMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudApplicationMapsAllFields() {
        V3Lifecycle lifecycle = new V3Lifecycle("buildpack", new V3LifecycleData(List.of("java_buildpack"), "cflinuxfs4"));
        V3Application app = application("STARTED", lifecycle);

        CloudApplication result = V3ApplicationMapper.toCloudApplication(app, null);

        Assertions.assertEquals("my-app", result.getName());
        Assertions.assertEquals(CloudApplication.State.STARTED, result.getState());
        Assertions.assertEquals(LifecycleType.BUILDPACK, result.getLifecycle()
                                                               .getType());
        Assertions.assertEquals(Map.of("buildpacks", List.of("java_buildpack"), "stack", "cflinuxfs4"), result.getLifecycle()
                                                                                                              .getData());
    }

    @Test
    void testToCloudApplicationNullStateReturnsNullState() {
        V3Application app = application(null, null);

        Assertions.assertNull(V3ApplicationMapper.toCloudApplication(app, null)
                                                 .getState());
    }

    @Test
    void testToCloudApplicationNullLifecycleReturnsNullLifecycle() {
        V3Application app = application("STOPPED", null);

        Assertions.assertNull(V3ApplicationMapper.toCloudApplication(app, null)
                                                 .getLifecycle());
    }

    @Test
    void testToCloudApplicationLifecycleWithNullTypeReturnsNullLifecycle() {
        V3Lifecycle lifecycle = new V3Lifecycle(null, new V3LifecycleData(List.of(), "cflinuxfs4"));
        V3Application app = application("STARTED", lifecycle);

        Assertions.assertNull(V3ApplicationMapper.toCloudApplication(app, null)
                                                 .getLifecycle());
    }

    @Test
    void testToCloudApplicationLifecycleTypeIsUpperCased() {
        V3Lifecycle lifecycle = new V3Lifecycle("docker", null);
        V3Application app = application("STARTED", lifecycle);

        Assertions.assertEquals(LifecycleType.DOCKER, V3ApplicationMapper.toCloudApplication(app, null)
                                                                         .getLifecycle()
                                                                         .getType());
    }

    @Test
    void testToCloudApplicationNullLifecycleDataReturnsEmptyDataMap() {
        V3Lifecycle lifecycle = new V3Lifecycle("buildpack", null);
        V3Application app = application("STARTED", lifecycle);

        Assertions.assertEquals(Map.of(), V3ApplicationMapper.toCloudApplication(app, null)
                                                             .getLifecycle()
                                                             .getData());
    }

    @Test
    void testToCloudApplicationLifecycleDataWithNullFieldsReturnsDefaultedMap() {
        V3Lifecycle lifecycle = new V3Lifecycle("buildpack", new V3LifecycleData(null, null));
        V3Application app = application("STARTED", lifecycle);

        Assertions.assertEquals(Map.of("buildpacks", List.of(), "stack", ""), V3ApplicationMapper.toCloudApplication(app, null)
                                                                                                 .getLifecycle()
                                                                                                 .getData());
    }

    private V3Application application(String state, V3Lifecycle lifecycle) {
        return new V3Application(GUID_STRING, "my-app", state, null, null, lifecycle, null, null);
    }

}
