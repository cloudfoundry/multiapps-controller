package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3JobMapper;

/**
 * CF v3 <em>Jobs</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape and domain mapping of the OSS
 * {@code CloudControllerRestClientImpl#getAsyncJob} on top of the shared {@link CloudControllerV3Client} machinery.
 */
public class JobsV3Operations {

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public JobsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    /**
     * {@code GET /v3/jobs/{guid}} &rarr; {@link CloudAsyncJob}. Non-paginated, synchronous fetch. A missing job yields a
     * {@code CloudOperationException(NOT_FOUND)}, matching the OSS {@code fetch(...)} contract (which does not swallow 404s here).
     */
    public CloudAsyncJob getAsyncJob(String jobId) {
        V3Job job = cc.get("/v3/jobs/" + jobId, V3Job.class);
        return V3JobMapper.toCloudAsyncJob(job);
    }

}
