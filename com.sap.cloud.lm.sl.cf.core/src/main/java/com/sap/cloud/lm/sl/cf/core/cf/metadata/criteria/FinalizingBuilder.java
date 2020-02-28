package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

import java.util.stream.Collectors;

public class FinalizingBuilder {
    private MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder;

    public FinalizingBuilder(MtaMetadataCriteriaBuilder mtaMetadataCriteriaBuilder) {
        this.mtaMetadataCriteriaBuilder = mtaMetadataCriteriaBuilder;
    }

    public MtaMetadataCriteriaBuilder and() {
        return mtaMetadataCriteriaBuilder;
    }

    public MtaMetadataCriteria build() {
        String query = String.join(",", mtaMetadataCriteriaBuilder.getQueries());
        return new MtaMetadataCriteria(query);
    }
}
