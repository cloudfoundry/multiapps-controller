package com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LabelBuilder {
    private MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder;
    private String label;
    private String prefix;

    public LabelBuilder(MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder, String label) {
        this.mtaMetadataCriteriaBuilder = mtaMetadataCriteriaBuilder;
        this.label = label;
    }

    public LabelBuilder withPrefix(String prefix) {
        MtaMetadataCriteriaValidator.validateLabelKeyPrefix(prefix);
        this.prefix = prefix;
        return this;
    }

    public FinalizingBuilder exists() {
        return completeQuery(buildLabel());
    }

    public FinalizingBuilder notExists() {
        return completeQuery("!" + buildLabel());
    }

    public FinalizingBuilder haveValue(String value) {
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        return completeQuery(buildLabel() + "=" + value);
    }

    public FinalizingBuilder notHaveValue(String value) {
        MtaMetadataCriteriaValidator.validateLabelValue(value);
        return completeQuery(buildLabel() + "!=" + value);
    }

    public FinalizingBuilder valueIn(List<String> values) {
        String concatenatedValues = values.stream()
                                          .peek(MtaMetadataCriteriaValidator::validateLabelValue)
                                          .collect(Collectors.joining(","));
        return completeQuery(buildLabel() + " in (" + concatenatedValues + ")");
    }

    public FinalizingBuilder valueNotIn(List<String> values) {
        String concatenatedValues = values.stream()
                                          .peek(MtaMetadataCriteriaValidator::validateLabelValue)
                                          .collect(Collectors.joining(","));
        return completeQuery(buildLabel() + " notin (" + concatenatedValues + ")");
    }

    private String buildLabel() {
        return Stream.of(prefix, label)
                     .filter(Objects::nonNull)
                     .collect(Collectors.joining(""));
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
