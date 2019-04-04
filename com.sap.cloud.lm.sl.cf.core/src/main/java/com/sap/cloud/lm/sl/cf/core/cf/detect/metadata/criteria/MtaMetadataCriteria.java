package com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria;

import java.net.URLEncoder;

public class MtaMetadataCriteria {
    public static final String NONE = "";
    private String query;

    public MtaMetadataCriteria(String query) {
        this.query = query;
    }

    public String get() {
        prepareQuery();
        return query;
    }

    private void prepareQuery() {
        query = URLEncoder.encode(query);
    }
}
