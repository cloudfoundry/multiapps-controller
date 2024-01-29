package org.cloudfoundry.multiapps.controller.persistence.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable
public interface QueryAttributeRestriction<T> {

    String getAttribute();

    @Nullable
    T getValue();

    Condition<T> getCondition();

    @Value.Default
    default Predicate satisfiedBy(Root<?> root) {
        return getCondition().satisfiedBy(root.get(getAttribute()), getValue());
    }

    interface Condition<T> {
        Predicate satisfiedBy(Expression<T> attribute, T value);
    }

}
