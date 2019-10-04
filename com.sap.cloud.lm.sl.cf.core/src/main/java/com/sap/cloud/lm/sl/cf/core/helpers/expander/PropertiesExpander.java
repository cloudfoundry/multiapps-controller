package com.sap.cloud.lm.sl.cf.core.helpers.expander;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.replaceAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.helpers.ReferencingPropertiesVisitor;
import com.sap.cloud.lm.sl.mta.helpers.VisitableObject;
import com.sap.cloud.lm.sl.mta.resolvers.Reference;
import com.sap.cloud.lm.sl.mta.resolvers.ReferencePattern;

public class PropertiesExpander extends ReferencingPropertiesVisitor implements Expander<Map<String, Object>, Map<String, Object>> {

    private static final ReferencePattern REFERENCE_PATTERN = ReferencePattern.FULLY_QUALIFIED;

    private final List<String> newDependencyNames;
    private final List<String> expandedProperties;

    public PropertiesExpander(String originalDependencyName, List<String> newDependencyNames) {
        this(originalDependencyName, newDependencyNames, new ArrayList<>());
    }

    protected PropertiesExpander(String originalDependencyName, List<String> newDependencyNames, List<String> expandedProperties) {
        super(REFERENCE_PATTERN,
              reference -> reference.getDependencyName()
                                    .equals(originalDependencyName));
        this.expandedProperties = expandedProperties;
        this.newDependencyNames = newDependencyNames;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> expand(Map<String, Object> properties) {
        return (Map<String, Object>) new VisitableObject(properties).accept(this);
    }

    public List<String> getExpandedProperties() {
        return expandedProperties;
    }

    private List<String> expandReferences(List<Reference> references, String value) {
        List<StringBuilder> result = newDependencyNames.stream()
                                                       .map(irrelevant -> new StringBuilder(value))
                                                       .collect(Collectors.toList());
        for (Reference reference : references) {
            for (int i = 0; i < newDependencyNames.size(); i++) {
                String newDependencyName = newDependencyNames.get(i);
                String oldReference = REFERENCE_PATTERN.toString(reference);
                String newReference = REFERENCE_PATTERN.toString(new Reference(null, reference.getKey(), newDependencyName));
                replaceAll(result.get(i), oldReference, newReference);
            }
        }
        return result.stream()
                     .map(StringBuilder::toString)
                     .collect(Collectors.toList());
    }

    @Override
    protected Object visit(String key, String value, List<Reference> references) {
        expandedProperties.add(key);
        return expandReferences(references, value);
    }

}
