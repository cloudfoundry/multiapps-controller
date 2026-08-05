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

/**
 * CF v3 <em>service offerings</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape, filtering and
 * per-offering plan fan-out of the OSS {@code CloudControllerRestClientImpl#getServiceOfferings} on top of the shared
 * {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>list offerings &rarr; {@code GET /v3/service_offerings?space_guids=<target>} (paginated);</li>
 * <li>resolve each offering's plans &rarr; {@code GET /v3/service_plans?service_offering_guids=<guid>} (paginated).</li>
 * </ul>
 *
 * <p>
 * This mirrors the OSS {@code getServiceResources()} + {@code zipWithAuxiliaryServiceOfferingContent(...)} flow: the offerings are scoped
 * to the target space, and each offering is enriched with the plans belonging to it before being mapped to the domain object.
 * </p>
 */
public class ServiceOfferingsV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int PER_PAGE = 5000;

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>> OFFERING_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3ServicePlan>> PLAN_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServiceOfferingsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public List<CloudServiceOffering> getServiceOfferings() {
        return getServiceOfferingResources().stream()
                                            .map(this::toCloudServiceOfferingWithPlans)
                                            .toList();
    }

    // Mirrors the OSS getServiceResources(): list the target space's offerings (space_guids filter), walking all pages.
    private List<V3ServiceOffering> getServiceOfferingResources() {
        StringBuilder query = new StringBuilder("/v3/service_offerings?per_page=" + PER_PAGE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        return cc.list(query.toString(), OFFERING_LIST_TYPE);
    }

    // Mirrors the OSS zipWithAuxiliaryServiceOfferingContent(...): fetch the offering's plans, then map to the domain object.
    private CloudServiceOffering toCloudServiceOfferingWithPlans(V3ServiceOffering serviceOffering) {
        List<CloudServicePlan> servicePlans = getServicePlans(serviceOffering.guid());
        return V3ServiceOfferingMapper.toCloudServiceOffering(serviceOffering, servicePlans);
    }

    // Mirrors the OSS getServicePlanResourcesByServiceOfferingGuid(...): list the plans belonging to the given offering, walking all pages.
    private List<CloudServicePlan> getServicePlans(String serviceOfferingGuid) {
        String uri = "/v3/service_plans?service_offering_guids=" + serviceOfferingGuid + "&per_page=" + PER_PAGE;
        return cc.list(uri, PLAN_LIST_TYPE)
                 .stream()
                 .map(V3ServicePlanMapper::toCloudServicePlan)
                 .toList();
    }

}
