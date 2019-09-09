package com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria;

import java.util.ArrayList;
import java.util.List;

public class MtaMetadataCriteriaBuilder {
    public static final String LABEL_APP_NAME = "app_name";
    public static final String LABEL_MTA_ID = "mta_id";
    
    private List<String> queries = new ArrayList<>();

    public static MtaMetadataCriteriaBuilder builder() {
        return new MtaMetadataCriteriaBuilder();
    }

    public LabelBuilder label(String label) {
        MtaMetadataCriteriaValidator.validateLabelKey(label);
        return new LabelBuilder(this, label);
    }

    public List<String> getQueries() {
        return queries;
    }
}