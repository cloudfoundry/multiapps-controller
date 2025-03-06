package org.cloudfoundry.multiapps.controller.process.util;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsupportedParameterFinderTest {

    @Mock
    private SupportedParameterChecker supportedParameterChecker;

    @InjectMocks
    private UnsupportedParameterFinder finder;

    @Mock
    private ReferenceContainerMatcher referenceMatcher;

    @Mock
    private CustomParameterContainerProcessor customParameterProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testNoUnsupportedParameters() {
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        List<ReferenceContainer> referenceContainers = List.of(
            new ReferenceContainer("module1", List.of(new Reference("~{param1}", "param1", null))));
        CustomParameterContainer container = mock(CustomParameterContainer.class);
        when(container.getParameters()).thenReturn(List.of("param1"));
        when(container.getPrefixedName()).thenReturn("module1");
        when(container.getParameterOwner()).thenReturn("module1");

        when(supportedParameterChecker.getCustomParameters(descriptor)).thenReturn(List.of(container));

        when(customParameterProcessor.processCustomParameterContainer(any(), eq(referenceContainers), eq(referenceMatcher)))
            .thenReturn(new AbstractMap.SimpleEntry<>("module1", List.of()));

        Map<String, List<String>> result = finder.findUnsupportedParameters(descriptor, referenceContainers);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUnsupportedParameters() {
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        List<ReferenceContainer> referenceContainers = List.of(
            new ReferenceContainer("module1", List.of(new Reference("~{param1}", "param1", null))));
        CustomParameterContainer container = mock(CustomParameterContainer.class);
        when(container.getParameters()).thenReturn(List.of("param1", "param2"));
        when(container.getPrefixedName()).thenReturn("module1");
        when(container.getParameterOwner()).thenReturn("module1");

        when(supportedParameterChecker.getCustomParameters(descriptor)).thenReturn(List.of(container));
        when(customParameterProcessor.processCustomParameterContainer(any(), eq(referenceContainers), eq(referenceMatcher)))
            .thenReturn(new AbstractMap.SimpleEntry<>("module1", List.of("param2")));

        Map<String, List<String>> result = finder.findUnsupportedParameters(descriptor, referenceContainers);

        assertEquals(1, result.size());
        assertEquals(List.of("param2"), result.get("module1"));
    }

    @Test
    void testMultipleCustomParameters() {
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        List<ReferenceContainer> referenceContainers = List.of(
            new ReferenceContainer("module1", List.of(new Reference("~{param1}", "param1", "module1"))));

        CustomParameterContainer container1 = mock(CustomParameterContainer.class);
        when(container1.getParameters()).thenReturn(List.of("param1", "param2"));
        when(container1.getPrefixedName()).thenReturn("module1");
        when(container1.getParameterOwner()).thenReturn("module1");

        CustomParameterContainer container2 = mock(CustomParameterContainer.class);
        when(container2.getParameters()).thenReturn(List.of("param3"));
        when(container2.getPrefixedName()).thenReturn("module2");
        when(container2.getParameterOwner()).thenReturn("module2");

        when(supportedParameterChecker.getCustomParameters(descriptor)).thenReturn(List.of(container1, container2));
        when(customParameterProcessor.processCustomParameterContainer(eq(container1), eq(referenceContainers), eq(referenceMatcher)))
            .thenReturn(new AbstractMap.SimpleEntry<>("module1", List.of("param2")));
        when(customParameterProcessor.processCustomParameterContainer(eq(container2), eq(referenceContainers), eq(referenceMatcher)))
            .thenReturn(new AbstractMap.SimpleEntry<>("module2", List.of("param3")));

        Map<String, List<String>> result = finder.findUnsupportedParameters(descriptor, referenceContainers);

        assertEquals(2, result.size());
        assertEquals(List.of("param2"), result.get("module1"));
        assertEquals(List.of("param3"), result.get("module2"));
    }
}
