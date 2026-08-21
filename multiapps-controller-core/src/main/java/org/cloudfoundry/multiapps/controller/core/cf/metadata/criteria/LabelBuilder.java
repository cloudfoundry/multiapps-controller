package org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria;

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

    public FinalizingBuilder hasValueWithLegacyFallback(String value, String legacyValue) {
        if (StringUtils.isEmpty(legacyValue)) {
            return hasValue(value);
        }
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        MtaMetadataCriteriaValidator.validateLabelValue(legacyValue);
        return completeQuery(label + " in (" + value + "," + legacyValue + ")");
    }

    public FinalizingBuilder hasValueWithLegacyFallbackOrIsntPresent(String value, String legacyValue) {
        if (StringUtils.isEmpty(value)) {
            return doesNotExist();
        }
        return hasValueWithLegacyFallback(value, legacyValue);
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
