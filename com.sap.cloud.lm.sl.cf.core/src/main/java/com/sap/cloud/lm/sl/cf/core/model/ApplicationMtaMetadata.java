package com.sap.cloud.lm.sl.cf.core.model;

/**
 * MTA metadata information associated with an application;
 */
public class ApplicationMtaMetadata {

    private MtaMetadata mtaMetadata;
    private DeployedMtaModule deployedMtaModule;

    private ApplicationMtaMetadata(Builder builder) {
        this.mtaMetadata = builder.mtaMetadata;
        this.deployedMtaModule = builder.module;
    }
    
    public MtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }
    public DeployedMtaModule getDeployedMtaModule() {
        return deployedMtaModule;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MtaMetadata mtaMetadata;
        private DeployedMtaModule module;

        private Builder() {
        }

        public Builder withMtaMetadata(MtaMetadata mtaMetadata) {
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
