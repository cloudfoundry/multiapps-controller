package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

public class OperationMetadata {

    protected Set<ParameterMetadata> parameters;
    protected String diagramId;
    /**
     * Can be used to ensure backwards compatibility when trying to find processes started with an older version of the application.
     */
    protected List<String> previousDiagramIds;
    protected List<String> versions;

    public static OperationMetadataBuilder builder() {
        return new OperationMetadataBuilder();
    }

    public Set<ParameterMetadata> getParameters() {
        return parameters;
    }

    public String getDiagramId() {
        return diagramId;
    }

    public List<String> getPreviousDiagramIds() {
        return previousDiagramIds;
    }

    public List<String> getVersions() {
        return versions;
    }

    public static class OperationMetadataBuilder {

        protected Set<ParameterMetadata> parameters;
        protected String diagramId;
        protected List<String> previousDiagramIds;
        protected List<String> versions;

        public OperationMetadata build() {
            OperationMetadata metadata = new OperationMetadata();
            metadata.parameters = parameters;
            metadata.diagramId = diagramId;
            metadata.previousDiagramIds = ObjectUtils.defaultIfNull(previousDiagramIds, Collections.<String> emptyList());
            metadata.versions = ObjectUtils.defaultIfNull(versions, Collections.<String> emptyList());
            return metadata;
        }

        public OperationMetadataBuilder parameters(Set<ParameterMetadata> parameters) {
            this.parameters = parameters;
            return this;
        }

        public OperationMetadataBuilder diagramId(String diagramId) {
            this.diagramId = diagramId;
            return this;
        }

        public OperationMetadataBuilder previousDiagramIds(String... previousDiagramIds) {
            this.previousDiagramIds = Arrays.asList(previousDiagramIds);
            return this;
        }

        public OperationMetadataBuilder versions(String... versions) {
            this.versions = Arrays.asList(versions);
            return this;
        }

    }

}
