package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OperationMetadata {

    protected Set<ParameterMetadata> parameters;
    protected String activitiDiagramId;
    protected List<String> versions;

    public static OperationMetadataBuilder builder() {
        return new OperationMetadataBuilder();
    }

    public static class OperationMetadataBuilder {

        protected Set<ParameterMetadata> parameters;
        protected String activitiDiagramId;
        protected List<String> versions;

        public OperationMetadata build() {
            OperationMetadata metadata = new OperationMetadata();
            metadata.parameters = parameters;
            metadata.activitiDiagramId = activitiDiagramId;
            metadata.versions = versions != null ? versions : Collections.emptyList();
            return metadata;
        }

        public OperationMetadataBuilder parameters(Set<ParameterMetadata> parameters) {
            this.parameters = parameters;
            return this;
        }

        public OperationMetadataBuilder activitiDiagramId(String activitiDiagramId) {
            this.activitiDiagramId = activitiDiagramId;
            return this;
        }
        
        public OperationMetadataBuilder versions(String... versions) {
            this.versions = Arrays.asList(versions);
            return this;
        }

    }

    public Set<ParameterMetadata> getParameters() {
        return parameters;
    }

    public String getActivitiDiagramId() {
        return activitiDiagramId;
    }

    public List<String> getVersions() {
        return versions;
    }

}
