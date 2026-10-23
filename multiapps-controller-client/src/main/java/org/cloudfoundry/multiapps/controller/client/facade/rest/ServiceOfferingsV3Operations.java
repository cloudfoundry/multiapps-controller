package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOfferingMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlanMapper;
import org.springframework.core.ParameterizedTypeReference;

public class ServiceOfferingsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>> OFFERING_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3ServicePlan>> PLAN_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public ServiceOfferingsV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public List<CloudServiceOffering> getServiceOfferings() {
        return getServiceOfferingResources().stream()
                                            .map(this::toCloudServiceOfferingWithPlans)
                                            .toList();
    }

    private List<V3ServiceOffering> getServiceOfferingResources() {
        String uri = V3Uri.path(CloudControllerV3Endpoints.SERVICE_OFFERINGS)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .query(V3QueryParameters.SPACE_GUIDS, OperationsUtil.getGuid(targetSpace))
                          .build();

        return client.list(uri, OFFERING_PAGE);
    }

    private CloudServiceOffering toCloudServiceOfferingWithPlans(V3ServiceOffering serviceOffering) {
        List<CloudServicePlan> servicePlans = getServicePlans(serviceOffering.guid());

        return V3ServiceOfferingMapper.toCloudServiceOffering(serviceOffering, servicePlans);
    }

    private List<CloudServicePlan> getServicePlans(String serviceOfferingGuid) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.SERVICE_PLANS)
                          .query(V3QueryParameters.SERVICE_OFFERING_GUIDS, serviceOfferingGuid)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .build();

        return client.list(uri, PLAN_PAGE)
                     .stream()
                     .map(V3ServicePlanMapper::toCloudServicePlan)
                     .toList();
    }

}
