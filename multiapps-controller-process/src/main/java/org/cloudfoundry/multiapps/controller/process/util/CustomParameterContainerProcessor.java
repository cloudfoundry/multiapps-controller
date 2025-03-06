package org.cloudfoundry.multiapps.controller.process.util;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.springframework.stereotype.Component;

@Component
public class CustomParameterContainerProcessor {
    private static final String GLOBAL_PARAMETER = "global-parameter";

    public Map.Entry<String, List<String>> processCustomParameterContainer(CustomParameterContainer container,
                                                                           List<ReferenceContainer> references,
                                                                           ReferenceContainerMatcher referenceMatcher) {
        List<String> unreferenced = filterUnreferencedParameters(container, references, referenceMatcher);
        String prefixedName = getPrefixedName(container);
        return new AbstractMap.SimpleEntry<>(prefixedName, unreferenced);
    }

    private List<String> filterUnreferencedParameters(CustomParameterContainer container, List<ReferenceContainer> references,
                                                      ReferenceContainerMatcher referenceMatcher) {
        return container.getParameters()
                        .stream()
                        .filter(parameter -> !isParameterReferenced(references, container, parameter, referenceMatcher))
                        .collect(Collectors.toList());
    }

    private boolean isParameterReferenced(List<ReferenceContainer> references, CustomParameterContainer container, String customParameter,
                                          ReferenceContainerMatcher referenceMatcher) {
        return references.stream()
                         .anyMatch(referenceContainer -> referenceMatcher.isReferenceContainerMatching(referenceContainer, container,
                                                                                                       customParameter));
    }

    private String getPrefixedName(CustomParameterContainer container) {
        return container.getPrefixedName() != null ? container.getPrefixedName() : GLOBAL_PARAMETER;
    }
}
