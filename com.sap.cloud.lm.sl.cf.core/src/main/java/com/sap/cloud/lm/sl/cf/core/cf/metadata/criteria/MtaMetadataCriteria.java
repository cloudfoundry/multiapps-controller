package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

public class MtaMetadataCriteria {

    private String query;

    public MtaMetadataCriteria(String query) {
        this.query = query;
    }

    public String get() {
        return query;
    }
}
