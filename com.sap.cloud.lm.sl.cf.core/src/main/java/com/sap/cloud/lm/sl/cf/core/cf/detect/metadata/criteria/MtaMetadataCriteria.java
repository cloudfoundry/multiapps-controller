package com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria;

public class MtaMetadataCriteria {
    public static final String NONE = "";
    private String query;

    public MtaMetadataCriteria(String query) {
        this.query = query;
    }

    public String get() {
        return query;
    }
}
