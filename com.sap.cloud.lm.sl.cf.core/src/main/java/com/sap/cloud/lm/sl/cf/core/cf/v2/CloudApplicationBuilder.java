package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;

public interface CloudApplicationBuilder {

    public CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota, int memory,
        int instances, List<String> uris, List<String> idleUris, List<String> services, List<ServiceKeyToInject> serviceKeysToInject,
        Map<Object, Object> env, List<CloudTask> tasks, DockerInfo dockerInfo);
}
