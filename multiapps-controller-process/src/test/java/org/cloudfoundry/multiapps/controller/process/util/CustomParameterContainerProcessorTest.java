package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CustomParameterContainerProcessorTest {
    @Mock
    private ReferenceContainerMatcher referenceMatcher;

    @Mock
    private CustomParameterContainer container;

    @Mock
    private ReferenceContainer referenceContainer;

    @InjectMocks
    private CustomParameterContainerProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessCustomParameterContainer_WithNoUnreferencedParameters() {
        String customParameter = "param1";
        List<ReferenceContainer> references = List.of(referenceContainer);

        when(container.getParameters()).thenReturn(List.of(customParameter));
        when(container.getPrefixedName()).thenReturn("module1");
        when(referenceMatcher.isReferenceContainerMatching(any(), eq(container), eq(customParameter))).thenReturn(
            true);

        Map.Entry<String, List<String>> result = processor.processCustomParameterContainer(container, references, referenceMatcher);

        assertEquals("module1", result.getKey());
        assertTrue(result.getValue()
                         .isEmpty());
    }

    @Test
    void testProcessCustomParameterContainer_WithUnreferencedParameters() {

        String referencedParam = "param1";
        String unreferencedParam = "param2";
        List<ReferenceContainer> references = List.of(referenceContainer);

        when(container.getParameters()).thenReturn(List.of(referencedParam, unreferencedParam));
        when(container.getPrefixedName()).thenReturn("module1");
        when(referenceMatcher.isReferenceContainerMatching(any(), eq(container), eq(referencedParam))).thenReturn(true);
        when(referenceMatcher.isReferenceContainerMatching(any(), eq(container), eq(unreferencedParam))).thenReturn(
            false);

        Map.Entry<String, List<String>> result = processor.processCustomParameterContainer(container, references, referenceMatcher);

        assertEquals("module1", result.getKey());
        assertEquals(List.of(unreferencedParam), result.getValue());
    }

    @Test
    void testProcessCustomParameterContainer_WithNullPrefixedName() {

        String customParameter = "param1";
        List<ReferenceContainer> references = List.of(referenceContainer);

        when(container.getParameters()).thenReturn(List.of(customParameter));
        when(container.getPrefixedName()).thenReturn(null);
        when(referenceMatcher.isReferenceContainerMatching(any(), eq(container), eq(customParameter))).thenReturn(false);

        Map.Entry<String, List<String>> result = processor.processCustomParameterContainer(container, references, referenceMatcher);

        assertEquals("global-parameter", result.getKey());
        assertEquals(List.of(customParameter), result.getValue());
    }

    @Test
    void testProcessCustomParameterContainer_WithNonNullPrefixedName() {

        String customParameter = "param1";
        List<ReferenceContainer> references = List.of(referenceContainer);

        when(container.getParameters()).thenReturn(List.of(customParameter));
        when(container.getPrefixedName()).thenReturn("custom-module");
        when(referenceMatcher.isReferenceContainerMatching(any(), eq(container), eq(customParameter))).thenReturn(false);

        Map.Entry<String, List<String>> result = processor.processCustomParameterContainer(container, references, referenceMatcher);

        assertEquals("custom-module", result.getKey());
        assertEquals(List.of(customParameter), result.getValue());
    }
}
