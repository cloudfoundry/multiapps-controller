package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServicePlan;

public final class V3ServicePlanMapper {
    
    private static final String PUBLIC_VISIBILITY = "public";

    private V3ServicePlanMapper() {
    }

    public static CloudServicePlan toCloudServicePlan(V3ServicePlan servicePlan) {
        return ImmutableCloudServicePlan.builder()
                                        .metadata(V3ResourceMappers.parseMetadata(servicePlan.guid(), servicePlan.createdAt(),
                                                                                  servicePlan.updatedAt()))
                                        .v3Metadata(V3ResourceMappers.toV3Metadata(servicePlan.metadata()))
                                        .name(servicePlan.name())
                                        .description(servicePlan.description())
                                        .extra(extractBrokerCatalogMetadata(servicePlan.brokerCatalog()))
                                        .uniqueId(extractBrokerCatalogId(servicePlan.brokerCatalog()))
                                        .serviceOfferingId(extractServiceOfferingId(servicePlan.relationships()))
                                        .isPublic(isPublic(servicePlan.visibilityType()))
                                        .isFree(servicePlan.free())
                                        .build();
    }

    private static java.util.Map<String, Object> extractBrokerCatalogMetadata(V3ServicePlan.V3BrokerCatalog brokerCatalog) {
        return brokerCatalog == null ? null : brokerCatalog.metadata();
    }

    private static String extractBrokerCatalogId(V3ServicePlan.V3BrokerCatalog brokerCatalog) {
        return brokerCatalog == null ? null : brokerCatalog.id();
    }

    private static String extractServiceOfferingId(V3ServicePlan.V3Relationships relationships) {
        if (relationships == null || relationships.serviceOffering() == null || relationships.serviceOffering()
                                                                                             .data() == null) {
            return null;
        }

        return relationships.serviceOffering()
                            .data()
                            .guid();
    }

    private static Boolean isPublic(String visibilityType) {
        return visibilityType == null ? null : PUBLIC_VISIBILITY.equals(visibilityType);
    }

}
