package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3JobMapper;

public class JobsV3Operations {

    private final CloudControllerV3Client cc;

    public JobsV3Operations(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudAsyncJob getAsyncJob(String jobId) {
        V3Job job = cc.get(CloudControllerV3Endpoints.JOBS + "/" + jobId, V3Job.class);

        return V3JobMapper.toCloudAsyncJob(job);
    }

}
