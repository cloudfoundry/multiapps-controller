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

    public FinalizingBuilder hasValueInOrDoesNotExist(String... values) {
        if (values.length == 0 || StringUtils.isEmpty(values[0])) {
            return doesNotExist();
        }
        return hasValueIn(values);
    }

    public FinalizingBuilder hasValue(String value) {
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        return completeQuery(label + "=" + value);
    }

    public FinalizingBuilder hasValueIn(String... values) {
        for (String value : values) {
            MtaMetadataCriteriaValidator.validateLabelValue(value);
        }
        return completeQuery(label + " in (" + String.join(",", values) + ")");
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
