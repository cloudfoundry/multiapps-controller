package com.sap.cloud.lm.sl.cf.core.resolvers.v2;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.resolvers.PropertiesResolver;
import org.cloudfoundry.multiapps.mta.resolvers.ProvidedValuesResolver;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern;

public class PartialPropertiesResolver extends PropertiesResolver {

    private final List<String> dependenciesToIgnore;

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
