package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Metadata {

    private final Map<String, String> labels;
    private final Map<String, String> annotations;

    @JsonCreator
    Metadata(@JsonProperty("labels") Map<String, String> labels, @JsonProperty("annotations") Map<String, String> annotations) {
        this.labels = labels == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(labels));
        this.annotations = annotations == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(annotations));
    }

    @JsonProperty("labels")
    public Map<String, String> getLabels() {
        return labels;
    }

    @JsonProperty("annotations")
    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Metadata metadata = (Metadata) o;
        return labels.equals(metadata.labels) && annotations.equals(metadata.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labels, annotations);
    }

    @Override
    public String toString() {
        return "Metadata{annotations=" + annotations + ", labels=" + labels + "}";
    }

    public static final class Builder {

        private final Map<String, String> labels = new LinkedHashMap<>();
        private final Map<String, String> annotations = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder from(Metadata metadata) {
            if (metadata != null) {
                this.labels.putAll(metadata.getLabels());
                this.annotations.putAll(metadata.getAnnotations());
            }

            return this;
        }

        public Builder label(String key, String value) {
            this.labels.put(key, value);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                this.labels.putAll(labels);
            }

            return this;
        }

        public Builder putAllLabels(Map<String, String> labels) {
            return labels(labels);
        }

        public Builder annotation(String key, String value) {
            this.annotations.put(key, value);
            return this;
        }

        public Builder annotations(Map<String, String> annotations) {
            if (annotations != null) {
                this.annotations.putAll(annotations);
            }

            return this;
        }

        public Builder putAllAnnotations(Map<String, String> annotations) {
            return annotations(annotations);
        }

        public Metadata build() {
            return new Metadata(labels, annotations);
        }
    }
}
