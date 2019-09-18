package com.sap.cloud.lm.sl.cf.core.persistence.query.criteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class QueryCriteria {

    private Map<String, QueryAttributeRestriction<?>> attributeRestrictions = new HashMap<>();

    public void addRestriction(QueryAttributeRestriction<?> attributeRestriction) {
        attributeRestrictions.put(attributeRestriction.getAttribute(), attributeRestriction);
    }

    public <E> List<Predicate> toQueryPredicates(Root<E> root) {
        return attributeRestrictions.values()
                                    .stream()
                                    .map(attributeRestriction -> attributeRestriction.satisfiedBy(root))
                                    .collect(Collectors.toList());
    }
}
