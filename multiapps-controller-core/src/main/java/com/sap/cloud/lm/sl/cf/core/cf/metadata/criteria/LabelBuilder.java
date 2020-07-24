package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

import org.apache.commons.lang3.StringUtils;

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

    public FinalizingBuilder doesNotExist() {
        return completeQuery("!" + label);
    }

    public FinalizingBuilder hasValueOrIsntPresent(String value) {
        if (StringUtils.isEmpty(value)) {
            return doesNotExist();
        }
        
        return hasValue(value);
    }

    public FinalizingBuilder hasValue(String value) {        
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        return completeQuery(label + "=" + value);
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
