package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service plan resource ({@code GET /v3/service_plans}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "description": "...", "free": true|false,
 *   "visibility_type": "public|admin|organization|space",
 *   "available": true|false,
 *   "costs": [
 *     {
 *       "currency": "USD",
 *       "amount": 199.99,
 *       "unit": "..."
 *     },
 *   "maintenance_info": {
 *     "version": "1.0.0+dev4",
 *     "description": "Database version 7.8.0"
 *   },
 *   "schemas": {
 *     "service_instance": {
 *       "create": {
 *         "parameters": {
 *           "$schema": "...",
 *           "type": "object",
 *           "properties": {
 *             "billing-account": {
 *               "description": "Billing account number used to charge use of shared fake server.",
 *               "type": "string"
 *             }
 *           }
 *         }
 *       },
 *       "update": {
 *         "parameters": {}
 *       }
 *     },
 *     "service_binding": {
 *       "create": {
 *         "parameters": {}
 *       }
 *     }
 *   }
 *   "created_at": "...", "updated_at": "...",
 *   "broker_catalog": { "id": "...", "metadata": { ... } },
 *   "relationships": { "service_offering": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} }
 *   "links": { "self": { "href": "..." }, "scale": { "href": "..." }, ... }}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServicePlan(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                            @JsonProperty(V3Fields.DESCRIPTION) String description, @JsonProperty(V3Fields.FREE) Boolean free,
                            @JsonProperty(V3Fields.VISIBILITY_TYPE) String visibilityType,
                            @JsonProperty(V3Fields.AVAILABLE) Boolean available,
                            @JsonProperty(V3Fields.COSTS) List<V3ServicePlanCost> costs,
                            @JsonProperty(V3Fields.MAINTENANCE_INFO) V3ServicePlanMaintenanceInfo maintenanceInfo,
                            @JsonProperty(V3Fields.SCHEMAS) V3ServicePlanSchemas schemas,
                            @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                            @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                            @JsonProperty(V3Fields.BROKER_CATALOG) V3BrokerCatalog brokerCatalog,
                            @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships,
                            @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                            @JsonProperty(V3Fields.LINKS) Map<String, V3Link> links) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3BrokerCatalog(@JsonProperty(V3Fields.ID) String id, @JsonProperty(V3Fields.METADATA) Map<String, Object> metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.SERVICE_OFFERING) V3ToOneRelationship serviceOffering,
                                  @JsonProperty(V3Fields.SPACE) V3ToOneRelationship space) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ServicePlanCost(@JsonProperty(V3Fields.AMOUNT) float amount, @JsonProperty(V3Fields.CURRENCY) String currency,
                                    @JsonProperty(V3Fields.UNIT) String unit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ServicePlanMaintenanceInfo(@JsonProperty(V3Fields.VERSION) String version,
                                               @JsonProperty(V3Fields.DESCRIPTION) String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ServicePlanSchemas(@JsonProperty(V3Fields.SERVICE_INSTANCE) V3SchemaParameters serviceInstance,
                                       @JsonProperty(V3Fields.SERVICE_BINDING) V3SchemaParameters serviceBinding) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3SchemaParameters(@JsonProperty(V3Fields.CREATE) V3SchemaParametersDefinition create,
                                     @JsonProperty(V3Fields.UPDATE) V3SchemaParametersDefinition update) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3SchemaParametersDefinition(@JsonProperty(V3Fields.PARAMETERS) Map<String, Object> parameters) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Link(@JsonProperty(V3Fields.HREF) String href) {
    }

}
