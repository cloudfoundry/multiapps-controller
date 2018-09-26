package com.sap.cloud.lm.sl.cf.core.resolvers.v2_0;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.mta.resolvers.PropertiesResolver;
import com.sap.cloud.lm.sl.mta.resolvers.ProvidedValuesResolver;
import com.sap.cloud.lm.sl.mta.resolvers.Reference;
import com.sap.cloud.lm.sl.mta.resolvers.ReferencePattern;

public class PartialPropertiesResolver extends PropertiesResolver {

    private List<String> dependenciesToIgnore;

    public PartialPropertiesResolver(Map<String, Object> properties, ProvidedValuesResolver valuesResolver,
        ReferencePattern referencePattern, String prefix, List<String> dependenciesToIgnore) {
        super(properties, valuesResolver, referencePattern, prefix);
        this.dependenciesToIgnore = dependenciesToIgnore;
    }

    @Override
    protected Object resolveReferenceInContext(String key, Reference reference) {
        if (reference.getDependencyName() != null && dependenciesToIgnore.contains(reference.getDependencyName())) {
            return reference.getMatchedPattern();
        }
        return super.resolveReferenceInContext(key, reference);
    }

}
