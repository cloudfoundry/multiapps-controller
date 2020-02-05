package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

import java.util.ArrayList;
import java.util.List;

public class MtaMetadataCriteriaBuilder {

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