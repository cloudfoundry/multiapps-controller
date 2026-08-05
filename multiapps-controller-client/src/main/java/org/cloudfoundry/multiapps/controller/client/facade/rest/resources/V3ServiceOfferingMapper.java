package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceOffering;

/**
 * Maps the {@link V3ServiceOffering} wire model (plus the service plans fetched separately for it) to the project's
 * {@link CloudServiceOffering} domain object. Mirrors the OSS {@code RawCloudServiceOffering} adapter field-for-field, so both client
 * implementations yield identical domain objects.
 */
public final class V3ServiceOfferingMapper {

    private V3ServiceOfferingMapper() {
    }

    public static CloudServiceOffering toCloudServiceOffering(V3ServiceOffering serviceOffering, List<CloudServicePlan> servicePlans) {
        return ImmutableCloudServiceOffering.builder()
                                            .metadata(V3ResourceMappers.parseMetadata(serviceOffering.guid(),
                                                                                      serviceOffering.createdAt(),
                                                                                      serviceOffering.updatedAt()))
                                            .name(serviceOffering.name())
                                            .isAvailable(serviceOffering.available())
                                            .isBindable(extractBindable(serviceOffering.brokerCatalog()))
                                            .description(serviceOffering.description())
                                            .isShareable(serviceOffering.shareable())
                                            .extra(extractBrokerCatalogMetadata(serviceOffering.brokerCatalog()))
                                            .docUrl(serviceOffering.documentationUrl())
                                            .brokerId(extractBrokerId(serviceOffering.relationships()))
                                            .uniqueId(extractBrokerCatalogId(serviceOffering.brokerCatalog()))
                                            .servicePlans(servicePlans)
                                            .build();
    }

    private static Boolean extractBindable(V3ServiceOffering.V3BrokerCatalog brokerCatalog) {
        if (brokerCatalog == null || brokerCatalog.features() == null) {
            return null;
        }
        return brokerCatalog.features()
                            .bindable();
    }

    private static Map<String, Object> extractBrokerCatalogMetadata(V3ServiceOffering.V3BrokerCatalog brokerCatalog) {
        return brokerCatalog == null ? null : brokerCatalog.metadata();
    }

    private static String extractBrokerCatalogId(V3ServiceOffering.V3BrokerCatalog brokerCatalog) {
        return brokerCatalog == null ? null : brokerCatalog.id();
    }

    private static String extractBrokerId(V3ServiceOffering.V3Relationships relationships) {
        if (relationships == null || relationships.serviceBroker() == null || relationships.serviceBroker()
                                                                                           .data() == null) {
            return null;
        }
        return relationships.serviceBroker()
                            .data()
                            .guid();
    }

}
