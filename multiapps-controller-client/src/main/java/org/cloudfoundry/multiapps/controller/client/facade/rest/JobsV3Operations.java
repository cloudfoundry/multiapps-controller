package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3JobMapper;
import org.springframework.http.HttpStatus;

public class JobsV3Operations {

    private final CloudControllerV3Client client;

    public JobsV3Operations(CloudControllerV3Client client) {
        this.client = client;
    }

    public CloudAsyncJob getAsyncJob(String jobId) {
        V3Job job = client.get(CloudControllerV3Endpoints.JOBS + "/" + jobId, V3Job.class);

        if (job == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.JOB_WITH_GUID_0_NOT_FOUND, jobId));
        }

        return V3JobMapper.toCloudAsyncJob(job);
    }

}
