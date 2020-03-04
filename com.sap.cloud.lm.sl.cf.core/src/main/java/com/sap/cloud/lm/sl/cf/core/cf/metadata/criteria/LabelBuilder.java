package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

import java.util.List;

public class LabelBuilder {
    private MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder;
    private String label;

    public LabelBuilder(MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder, String label) {
        this.mtaMetadataCriteriaBuilder = mtaMetadataCriteriaBuilder;
        this.label = label;
    }

    public FinalizingBuilder exists() {
        return completeQuery(label);
    }

    public FinalizingBuilder haveValue(String value) {
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        return completeQuery(label + "=" + value);
    }

    public FinalizingBuilder valueIn(List<String> values) {
        values.forEach(MtaMetadataCriteriaValidator::validateLabelValue);
        String concatenatedValues = String.join(",", values);
        return completeQuery(label + " in (" + concatenatedValues + ")");
    }

    private FinalizingBuilder completeQuery(String query) {
        MtaMetadataCriteriaBuilder nextBuilder = getNextBuilder();
        nextBuilder.getQueries()
                   .add(query);
        return new FinalizingBuilder(nextBuilder);
    }

    private MtaMetadataCriteriaBuilder getNextBuilder() {
        MtaMetadataCriteriaBuilder nextBuilder = new MtaMetadataCriteriaBuilder();
        nextBuilder.getQueries()
                   .addAll(mtaMetadataCriteriaBuilder.getQueries());
        return nextBuilder;
    }

    public String getLabel() {
        return label;
    }
}
