package com.sap.cloud.lm.sl.cf.core.model;

/**
 * MTA metadata information associated with an application;
 */
public class ApplicationMtaMetadata {

    private DeployedMtaMetadata mtaMetadata;
    private DeployedMtaModule module;

    private ApplicationMtaMetadata(Builder builder) {
        this.mtaMetadata = builder.mtaMetadata;
        this.module = builder.module;
    }

    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }
    public DeployedMtaModule getModule() {
        return module;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeployedMtaMetadata mtaMetadata;
        private DeployedMtaModule module;

        private Builder() {
        }

        public Builder withMtaMetadata(DeployedMtaMetadata mtaMetadata) {
            this.mtaMetadata = mtaMetadata;
            return this;
        }

        public Builder withModule(DeployedMtaModule module) {
            this.module = module;
            return this;
        }

        public ApplicationMtaMetadata build() {
            return new ApplicationMtaMetadata(this);
        }
    }


}
