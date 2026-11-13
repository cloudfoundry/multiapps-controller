package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service offering resource ({@code GET /v3/service_offerings}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "description": "...", "available": true, "shareable": true,
 *   "tags": [...], "documentation_url": "...", "created_at": "...", "updated_at": "...",
 *   "broker_catalog": { "id": "...", "metadata": {...}, "features": { "bindable": true, ... } },
 *   "relationships": { "service_broker": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceOffering(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                                @JsonProperty(V3Fields.DESCRIPTION) String description, @JsonProperty(V3Fields.AVAILABLE) Boolean available,
                                @JsonProperty(V3Fields.SHAREABLE) Boolean shareable,
                                @JsonProperty(V3Fields.DOCUMENTATION_URL) String documentationUrl,
                                @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                                @JsonProperty(V3Fields.BROKER_CATALOG) V3BrokerCatalog brokerCatalog,
                                @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships,
                                @JsonProperty(V3Fields.METADATA) V3Metadata metadata, @JsonProperty(V3Fields.TAGS) List<String> tags) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3BrokerCatalog(@JsonProperty(V3Fields.ID) String id, @JsonProperty(V3Fields.METADATA) Map<String, Object> metadata,
                                  @JsonProperty(V3Fields.FEATURES) V3Features features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Features(@JsonProperty(V3Fields.BINDABLE) Boolean bindable,
                             @JsonProperty(V3Fields.ALLOW_CONTEXT_UPDATES) Boolean allowContextUpdates,
                             @JsonProperty(V3Fields.BINDINGS_RETRIEVABLE) Boolean bindingsRetrievable,
                             @JsonProperty(V3Fields.INSTANCES_RETRIEVABLE) Boolean instancesRetrievable,
                             @JsonProperty(V3Fields.PLAN_UPDATABLE) Boolean planUpdatable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.SERVICE_BROKER) V3ToOneRelationship serviceBroker) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
