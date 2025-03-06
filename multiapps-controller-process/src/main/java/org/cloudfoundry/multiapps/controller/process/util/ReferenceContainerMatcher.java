package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.springframework.stereotype.Component;

@Component
public class ReferenceContainerMatcher {
    public boolean isReferenceContainerMatching(ReferenceContainer referenceContainer,
                                                CustomParameterContainer container,
                                                String customParameter) {
        return isReferenceMatchingParameter(referenceContainer, customParameter, container)
            && isReferenceOwnerMatched(referenceContainer.getReferenceOwner(), container.getParameterOwner());
    }

    private boolean isReferenceMatchingParameter(ReferenceContainer referenceContainer,
                                                 String customParameter, CustomParameterContainer container) {
        return referenceContainer.getReferences()
                                 .stream()
                                 .anyMatch(reference -> isReferenceNameMatched(reference, customParameter)
                                     && isReferenceDependencyMatched(reference, container));
    }

    private boolean isReferenceNameMatched(Reference reference, String customParameter) {
        return reference.getKey()
                        .equals(customParameter);
    }

    private boolean isReferenceDependencyMatched(Reference reference, CustomParameterContainer container) {
        String dependencyName = reference.getDependencyName();
        String parameterOwner = container.getParameterOwner();

        return dependencyName == null || parameterOwner == null || dependencyName.equals(parameterOwner);
    }

    private boolean isReferenceOwnerMatched(String referenceOwner, String parameterOwner) {
        return parameterOwner == null || parameterOwner.equals(referenceOwner);
    }
}
