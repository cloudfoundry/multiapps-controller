package org.cloudfoundry.multiapps.controller.core.cf.clients.v3;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.SLException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

class CustomCreateServiceKeyRequestV3 {
    @JsonProperty("type")
    private final String type = "key";
    @JsonProperty("name")
    private String name;
    @JsonSerialize(using = CustomServiceRelationshipSerializer.class)
    @JsonProperty("relationships")
    private UUID serviceInstanceId;
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    @JsonProperty("metadata")
    private Metadata mtaMetadata;

    public CustomCreateServiceKeyRequestV3(CloudServiceKey key, UUID serviceInstanceId) {
        this.name = key.getName();
        this.parameters = key.getCredentials();
        this.mtaMetadata = key.getV3Metadata();
        this.serviceInstanceId = serviceInstanceId;
    }

    static class CustomServiceRelationshipSerializer extends StdSerializer<UUID> {
        private static final long serialVersionUID = 1L;

        public CustomServiceRelationshipSerializer() {
            super(UUID.class);
        }

        @Override
        public void serialize(UUID value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (value != null) {
                generator.writeStartObject();
                generator.writeObjectFieldStart("service_instance");
                generator.writeObjectFieldStart("data");
                generator.writeStringField("guid", value.toString());
                generator.writeEndObject();
                generator.writeEndObject();
                generator.writeEndObject();
            } else {
                throw new SLException("Cannot create a service key with null service instance guid");
            }
        }

    }
}