package org.cloudfoundry.multiapps.controller.persistence.query.criteria;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class QueryCriteria {

    private final Set<QueryAttributeRestriction<?>> attributeRestrictions = new HashSet<>();

    public void addRestriction(QueryAttributeRestriction<?> attributeRestriction) {
        attributeRestrictions.add(attributeRestriction);
    }

    public <E> List<Predicate> toQueryPredicates(Root<E> root) {
        return attributeRestrictions.stream()
                                    .map(attributeRestriction -> attributeRestriction.satisfiedBy(root))
                                    .collect(Collectors.toList());
    }
}
