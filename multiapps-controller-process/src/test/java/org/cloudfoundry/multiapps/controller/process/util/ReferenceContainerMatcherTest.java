package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ReferenceContainerMatcherTest {

    @InjectMocks
    private ReferenceContainerMatcher referenceContainerMatcher;

    @Mock
    private ReferenceContainer referenceContainer;

    @Mock
    private CustomParameterContainer customParameterContainer;

    @Mock
    private Reference reference;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsReferenceContainerMatching_WithMatchingParameters() {
        String customParameter = "param1";
        String referenceOwner = "module1";
        String parameterOwner = "module1";

        when(referenceContainer.getReferenceOwner()).thenReturn(referenceOwner);
        when(referenceContainer.getReferences()).thenReturn(List.of(reference));
        when(reference.getKey()).thenReturn(customParameter);
        when(reference.getDependencyName()).thenReturn(parameterOwner);

        when(customParameterContainer.getParameterOwner()).thenReturn(parameterOwner);

        boolean result = referenceContainerMatcher.isReferenceContainerMatching(referenceContainer, customParameterContainer,
                                                                                customParameter);
        assertTrue(result);
    }

    @Test
    void testIsReferenceContainerMatching_WithNonMatchingReferenceKey() {
        String customParameter = "param1";
        String referenceOwner = "module1";
        String parameterOwner = "module1";

        when(referenceContainer.getReferenceOwner()).thenReturn(referenceOwner);
        when(referenceContainer.getReferences()).thenReturn(List.of(reference));
        when(reference.getKey()).thenReturn("param2");
        when(reference.getDependencyName()).thenReturn(parameterOwner);

        when(customParameterContainer.getParameterOwner()).thenReturn(parameterOwner);

        boolean result = referenceContainerMatcher.isReferenceContainerMatching(referenceContainer, customParameterContainer,
                                                                                customParameter);

        assertFalse(result);
    }

    @Test
    void testIsReferenceContainerMatching_WithNonMatchingDependency() {
        String customParameter = "param1";
        String referenceOwner = "module1";
        String parameterOwner = "module1";

        when(referenceContainer.getReferenceOwner()).thenReturn(referenceOwner);
        when(referenceContainer.getReferences()).thenReturn(List.of(reference));
        when(reference.getKey()).thenReturn(customParameter);
        when(reference.getDependencyName()).thenReturn("module2");

        when(customParameterContainer.getParameterOwner()).thenReturn(parameterOwner);

        boolean result = referenceContainerMatcher.isReferenceContainerMatching(referenceContainer, customParameterContainer,
                                                                                customParameter);

        assertFalse(result);
    }

    @Test
    void testIsReferenceContainerMatching_WithMatchingOwnerButNullDependency() {
        String customParameter = "param1";
        String referenceOwner = "module1";
        String parameterOwner = "module1";

        when(referenceContainer.getReferenceOwner()).thenReturn(referenceOwner);
        when(referenceContainer.getReferences()).thenReturn(List.of(reference));
        when(reference.getKey()).thenReturn(customParameter);
        when(reference.getDependencyName()).thenReturn(null);
        when(customParameterContainer.getParameterOwner()).thenReturn(parameterOwner);

        boolean result = referenceContainerMatcher.isReferenceContainerMatching(referenceContainer, customParameterContainer,
                                                                                customParameter);
        assertTrue(result);
    }
    
    @Test
    void testDoesReferenceMatchOwner_WithNonMatchingOwner() {
        String customParameter = "param1";
        String referenceOwner = "module1";
        String parameterOwner = "module2";

        when(referenceContainer.getReferenceOwner()).thenReturn(referenceOwner);
        when(referenceContainer.getReferences()).thenReturn(List.of(reference));
        when(reference.getKey()).thenReturn(customParameter);
        when(reference.getDependencyName()).thenReturn(parameterOwner);

        when(customParameterContainer.getParameterOwner()).thenReturn(parameterOwner);

        boolean result = referenceContainerMatcher.isReferenceContainerMatching(referenceContainer, customParameterContainer,
                                                                                customParameter);

        assertFalse(result);
    }
}
