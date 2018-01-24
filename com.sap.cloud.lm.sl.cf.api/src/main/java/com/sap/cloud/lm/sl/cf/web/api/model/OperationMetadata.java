package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class OperationMetadata {

    protected Set<ParameterMetadata> parameters;
    protected String activitiDiagramId;
    /**
     * Can be used to ensure backwards compatibility when trying to find processes started with an older version of the application.
     */
    protected List<String> previousActivitiDiagramIds;
    protected List<String> versions;

    public static OperationMetadataBuilder builder() {
        return new OperationMetadataBuilder();
    }

    public static class OperationMetadataBuilder {

        protected Set<ParameterMetadata> parameters;
        protected String activitiDiagramId;
        protected List<String> previousActivitiDiagramIds;
        protected List<String> versions;

        public OperationMetadata build() {
            OperationMetadata metadata = new OperationMetadata();
            metadata.parameters = parameters;
            metadata.activitiDiagramId = activitiDiagramId;
            metadata.previousActivitiDiagramIds = CommonUtil.getOrDefault(previousActivitiDiagramIds, Collections.<String> emptyList());
            metadata.versions = CommonUtil.getOrDefault(versions, Collections.<String> emptyList());
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

        public OperationMetadataBuilder previousActivitiDiagramIds(String... previousActivitiDiagramIds) {
            this.previousActivitiDiagramIds = Arrays.asList(previousActivitiDiagramIds);
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

    public List<String> getPreviousActivitiDiagramIds() {
        return previousActivitiDiagramIds;
    }

    public List<String> getVersions() {
        return versions;
    }

}
