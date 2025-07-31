package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;

@Value.Immutable
public abstract class RawInstancesInfo extends RawCloudEntity<InstancesInfo> {

    @Value.Parameter
    public abstract GetApplicationProcessStatisticsResponse getProcessStatisticsResponse();

    @Override
    public InstancesInfo derive() {
        var processStats = getProcessStatisticsResponse();
        return ImmutableInstancesInfo.builder()
                                     .instances(parseProcessStatistics(processStats.getResources()))
                                     .build();
    }

    private static List<InstanceInfo> parseProcessStatistics(List<ProcessStatisticsResource> stats) {
        if (stats == null) {
            return Collections.emptyList();
        }
        return stats.stream()
                    .map(RawInstancesInfo::parseProcessStatistic)
                    .collect(Collectors.toList());
    }

    private static InstanceInfo parseProcessStatistic(ProcessStatisticsResource statsResource) {
        return ImmutableInstanceInfo.builder()
                                    .index(statsResource.getIndex())
                                    .state(InstanceState.valueOfWithDefault(statsResource.getState()))
                                    .build();
    }

}
