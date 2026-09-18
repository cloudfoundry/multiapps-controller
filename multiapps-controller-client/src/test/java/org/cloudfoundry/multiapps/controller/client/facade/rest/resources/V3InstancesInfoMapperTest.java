package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3InstancesInfoMapperTest {

    @Test
    void testToInstancesInfoWithNullResourcesReturnsEmptyList() {
        InstancesInfo result = V3InstancesInfoMapper.toInstancesInfo(new V3Process.V3ProcessStats(null));

        Assertions.assertTrue(result.getInstances()
                                    .isEmpty());
    }

    @Test
    void testToInstancesInfoMapsEachResource() {
        V3Process.V3ProcessStats stats = new V3Process.V3ProcessStats(List.of(new V3Process.V3ProcessStatsResource(0, "RUNNING", "true"),
                                                                              new V3Process.V3ProcessStatsResource(1, "CRASHED", "false")));

        InstancesInfo result = V3InstancesInfoMapper.toInstancesInfo(stats);

        Assertions.assertEquals(2, result.getInstances()
                                         .size());
        Assertions.assertEquals(0, result.getInstances()
                                         .getFirst()
                                         .getIndex());
        Assertions.assertEquals(InstanceState.RUNNING, result.getInstances()
                                                             .getFirst()
                                                             .getState());
        Assertions.assertTrue(result.getInstances()
                                    .getFirst()
                                    .isRoutable());
        Assertions.assertEquals(InstanceState.CRASHED, result.getInstances()
                                                             .get(1)
                                                             .getState());
        Assertions.assertFalse(result.getInstances()
                                     .get(1)
                                     .isRoutable());
    }

    @Test
    void testToInstancesInfoDefaultsUnknownStateAndNonBooleanRoutable() {
        V3Process.V3ProcessStats stats = new V3Process.V3ProcessStats(List.of(new V3Process.V3ProcessStatsResource(0, "bogus", null)));

        InstancesInfo result = V3InstancesInfoMapper.toInstancesInfo(stats);

        Assertions.assertEquals(InstanceState.UNKNOWN, result.getInstances()
                                                             .getFirst()
                                                             .getState());
        Assertions.assertFalse(result.getInstances()
                                     .getFirst()
                                     .isRoutable());
    }

}
