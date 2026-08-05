package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service offering resource ({@code GET /v3/service_offerings}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "description": "...", "available": true, "shareable": true,
 *   "documentation_url": "...", "created_at": "...", "updated_at": "...",
 *   "broker_catalog": { "id": "...", "metadata": {...}, "features": { "bindable": true, ... } },
 *   "relationships": { "service_broker": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceOffering(@JsonProperty("guid") String guid, @JsonProperty("name") String name,
                                @JsonProperty("description") String description, @JsonProperty("available") Boolean available,
                                @JsonProperty("shareable") Boolean shareable,
                                @JsonProperty("documentation_url") String documentationUrl,
                                @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                                @JsonProperty("broker_catalog") V3BrokerCatalog brokerCatalog,
                                @JsonProperty("relationships") V3Relationships relationships,
                                @JsonProperty("metadata") V3Metadata metadata) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3BrokerCatalog(@JsonProperty("id") String id, @JsonProperty("metadata") Map<String, Object> metadata,
                                  @JsonProperty("features") V3Features features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Features(@JsonProperty("bindable") Boolean bindable,
                             @JsonProperty("allow_context_updates") Boolean allowContextUpdates,
                             @JsonProperty("bindings_retrievable") Boolean bindingsRetrievable,
                             @JsonProperty("instances_retrievable") Boolean instancesRetrievable,
                             @JsonProperty("plan_updateable") Boolean planUpdateable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("service_broker") V3ToOneRelationship serviceBroker) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

}
