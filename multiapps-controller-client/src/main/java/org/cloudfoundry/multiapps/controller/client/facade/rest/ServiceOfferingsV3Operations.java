package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOfferingMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlanMapper;
import org.springframework.core.ParameterizedTypeReference;

public class ServiceOfferingsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>> OFFERING_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3ServicePlan>> PLAN_LIST_TYPE = new ParameterizedTypeReference<>() {
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
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.SERVICE_OFFERINGS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (targetSpace != null && targetSpace.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(targetSpace.getGuid());
        }

        return client.list(query.toString(), OFFERING_LIST_TYPE);
    }

    private CloudServiceOffering toCloudServiceOfferingWithPlans(V3ServiceOffering serviceOffering) {
        List<CloudServicePlan> servicePlans = getServicePlans(serviceOffering.guid());

        return V3ServiceOfferingMapper.toCloudServiceOffering(serviceOffering, servicePlans);
    }

    private List<CloudServicePlan> getServicePlans(String serviceOfferingGuid) {
        String uri =
            CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_SERVICE_OFFERING_GUIDS + serviceOfferingGuid
                + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, PLAN_LIST_TYPE)
                     .stream()
                     .map(V3ServicePlanMapper::toCloudServicePlan)
                     .toList();
    }

}
