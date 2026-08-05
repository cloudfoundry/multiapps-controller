package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;

/**
 * Maps the {@link V3Process.V3ProcessStats} wire model to the project's {@link InstancesInfo} domain object. Mirrors the OSS
 * {@code RawInstancesInfo} adapter field-for-field, so both client implementations yield identical domain objects.
 * <p>
 * The instance state string comes from CF ({@code GET /v3/apps/{guid}/processes/web/stats}) and is resolved via
 * {@link InstanceState#valueOfWithDefault(String)} (which falls back to {@link InstanceState#UNKNOWN} for unrecognised values), so no OSS
 * leaf type is involved.
 */
public final class V3InstancesInfoMapper {

    private V3InstancesInfoMapper() {
    }

    public static InstancesInfo toInstancesInfo(V3Process.V3ProcessStats processStats) {
        return ImmutableInstancesInfo.builder()
                                     .instances(parseProcessStatistics(processStats.resources()))
                                     .build();
    }

    private static List<InstanceInfo> parseProcessStatistics(List<V3Process.V3ProcessStatsResource> stats) {
        if (stats == null) {
            return Collections.emptyList();
        }
        return stats.stream()
                    .map(V3InstancesInfoMapper::parseProcessStatistic)
                    .collect(Collectors.toList());
    }

    private static InstanceInfo parseProcessStatistic(V3Process.V3ProcessStatsResource statsResource) {
        return ImmutableInstanceInfo.builder()
                                    .index(statsResource.index())
                                    .state(InstanceState.valueOfWithDefault(statsResource.state()))
                                    .isRoutable(Boolean.parseBoolean(statsResource.routable()))
                                    .build();
    }

}
