package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Arrays;
import java.util.Objects;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudPackage.class)
@JsonDeserialize(as = ImmutableCloudPackage.class)
public abstract class CloudPackage extends CloudEntity implements Derivable<CloudPackage> {

    @Nullable
    public abstract Type getType();

    @Nullable
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(name = "bits", value = BitsData.class),
        @JsonSubTypes.Type(name = "docker", value = DockerData.class) })
    public abstract PackageData getData();

    @Nullable
    public abstract Status getStatus();

    @Override
    public CloudPackage derive() {
        return this;
    }

    public enum Type {
        BITS, DOCKER;

        @JsonCreator
        public static Type from(String s) {
            Objects.requireNonNull(s);
            return Arrays.stream(Type.values())
                         .filter(type -> s.toLowerCase()
                                          .equals(type.toString()))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(String.format(Messages.UNKNOWN_PACKAGE_TYPE, s)));
        }

        @JsonValue
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public interface PackageData {
    }

}
