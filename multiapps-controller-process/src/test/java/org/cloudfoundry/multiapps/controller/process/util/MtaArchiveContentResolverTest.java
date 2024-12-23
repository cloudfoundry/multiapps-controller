package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MtaArchiveContentResolverTest {
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private MtaArchiveHelper helper;

    @Mock
    private ExternalFileProcessor processor;

    @Mock
    private ContentLengthTracker sizeTracker;

    private DeploymentDescriptor descriptor;

    private MtaArchiveContentResolver resolver;

    private static final String SAMPLE_SPACE = "space";

    private static final String SAMPLE_SPACE_ID = "spaceID";

    private static final String RESOURCE_NAME = "resourceName";

    private static final String RESOURCE_NAME_2 = "resourceName_2";

    private static final String FILENAME = "fileName";

    private static final String FILENAME_2 = "fileName_2";

    private static final String CONFIG = "config";

    private static final String FILE_CONTENT_KEY = "fileContentKey";

    private static final String FILE_CONTENT_VALUE = "fileContentValue";

    private static final String FILE_CONTENT_KEY_2 = "fileContentKey_2";

    private static final String FILE_CONTENT_VALUE_2 = "fileContentValue_2";

    private static final String MODULE_NAME = "moduleName";

    private static final String DEPENDENCY_NAME = "dependencyName";

    private static final String MODULE_DEPENDENCY_PATH = MODULE_NAME + "/" + DEPENDENCY_NAME;

    private static final String MODULE_NAME_2 = "moduleName_2";

    private static final String DEPENDENCY_NAME_2 = "dependencyName_2";

    private static final String MODULE_2_DEPENDENCY_2_PATH = MODULE_NAME_2 + "/" + DEPENDENCY_NAME_2;

    private static final Map<String, String> CONTENT_KEY_VALUE_PAIR = Map.of(FILE_CONTENT_KEY, FILE_CONTENT_VALUE);

    private static final Map<String, String> CONTENT_KEY_VALUE_PAIR_2 = Map.of(FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2);

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(helper.getMtaArchiveModules())
               .thenReturn(Collections.emptyMap());
        Mockito.when(helper.getMtaArchiveResources())
               .thenReturn(Collections.emptyMap());
        Mockito.when(helper.getMtaRequiresDependencies())
               .thenReturn(Collections.emptyMap());
        descriptor = DeploymentDescriptor.createV3();
        resolver = new MtaArchiveContentResolver(descriptor, configuration, processor, sizeTracker);

    }

    public static Stream<Arguments> testResourceResolve() {
        return Stream.of(
            Arguments.of(List.of(createResource(RESOURCE_NAME)), Map.of(FILENAME, List.of(RESOURCE_NAME)), Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR),
                         Map.of(RESOURCE_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR))),
            Arguments.of(List.of(createResource(RESOURCE_NAME), createResource(RESOURCE_NAME_2)), Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2)),
                         Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR),
                         Map.of(RESOURCE_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR), RESOURCE_NAME_2, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR))),
            Arguments.of(List.of(createResource(RESOURCE_NAME), createResource(RESOURCE_NAME_2)),
                         Map.of(FILENAME, List.of(RESOURCE_NAME), FILENAME_2, List.of(RESOURCE_NAME_2)),
                         Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR, FILENAME_2, CONTENT_KEY_VALUE_PAIR_2),
                         Map.of(RESOURCE_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR), RESOURCE_NAME_2, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR_2))),
            Arguments.of(List.of(createResource(RESOURCE_NAME), createResource(RESOURCE_NAME_2)),
                         Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2), FILENAME_2, List.of(RESOURCE_NAME_2)),
                         Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR, FILENAME_2, CONTENT_KEY_VALUE_PAIR_2),
                         Map.of(RESOURCE_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR), RESOURCE_NAME_2,
                                Map.of(CONFIG, Map.of(FILE_CONTENT_KEY, FILE_CONTENT_VALUE, FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2)))));
    }

    @ParameterizedTest
    @MethodSource
    public void testResourceResolve(List<Resource> resourcesList, Map<String, List<String>> archiveEntries, Map<String, Map<String, Object>> fileContentEntries, Map<String, Object> resolvedResourcesResult) {
        descriptor.setResources(resourcesList);

        Mockito.when(helper.getResourceFileAttributes())
               .thenReturn(archiveEntries);

        for (var entry : fileContentEntries.keySet()) {
            Mockito.when(processor.processFileContent(any(), any(), eq(entry)))
                   .thenReturn(fileContentEntries.get(entry));
        }

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);

        assertResultMatchesActualResources(resolvedResourcesResult, descriptor);

    }

    @Test
    public void testWithExistingConfigResourceNoOverwrite() {
        Map<String, Object> fileContentForEntry1 = new HashMap<>();
        fileContentForEntry1.put(FILE_CONTENT_KEY, FILE_CONTENT_VALUE);

        Map<String, List<String>> entries = new HashMap<>();
        entries.put(FILENAME, List.of(RESOURCE_NAME));

        Mockito.when(processor.processFileContent(any(), any(), eq(FILENAME)))
               .thenReturn(fileContentForEntry1);

        Map<String, Object> existingParameters = new HashMap<>();
        Map<String, String> configMap = new HashMap<>();
        configMap.put(FILE_CONTENT_KEY, "shouldNotOverrideValue");
        existingParameters.put(CONFIG, configMap);

        Resource existingResource = Resource.createV3()
                                            .setName(RESOURCE_NAME)
                                            .setParameters(existingParameters);

        List<Resource> resourcesList = List.of(existingResource);
        descriptor.setResources(resourcesList);
        Mockito.when(helper.getResourceFileAttributes())
               .thenReturn(entries);

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);

        Map<String, Object> resolvedResourcesResult = Map.of(RESOURCE_NAME, Map.of(CONFIG, Map.of(FILE_CONTENT_KEY, "shouldNotOverrideValue")));

        assertResultMatchesActualResources(resolvedResourcesResult, descriptor);

    }

    private static Resource createResource(String resourceName) {
        return Resource.createV3()
                       .setName(resourceName);
    }

    private static void assertResultMatchesActualResources(Map<String, Object> resolvedResourcesResult, DeploymentDescriptor descriptor) {
        for (var resource : resolvedResourcesResult.keySet()) {

            Map<String, Object> resolvedParameters = descriptor.getResources()
                                                               .stream()
                                                               .filter(resolvedResource -> resource.equals(resolvedResource.getName()))
                                                               .findFirst()
                                                               .get()
                                                               .getParameters();
            assertEquals(resolvedResourcesResult.get(resource), resolvedParameters);
        }
    }

    public static Stream<Arguments> testModuleResolve() {
        return Stream.of(Arguments.of(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME)), Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH)),
                                      Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR),
                                      Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)))),
                         Arguments.of(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME)),
                                      Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH, "moduleName_2/dependencyName")),
                                      Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR),
                                      Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)), MODULE_NAME_2,
                                             Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)))),
                         Arguments.of(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME_2)),
                                      Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH, MODULE_2_DEPENDENCY_2_PATH)), Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR),
                                      Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)), MODULE_NAME_2,
                                             Map.of(DEPENDENCY_NAME_2, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)))),
                         Arguments.of(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME_2)),
                                      Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH), FILENAME_2, List.of(MODULE_2_DEPENDENCY_2_PATH)),
                                      Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR, FILENAME_2, CONTENT_KEY_VALUE_PAIR_2),
                                      Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)), MODULE_NAME_2,
                                             Map.of(DEPENDENCY_NAME_2, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR_2))),
                                      Arguments.of(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME_2)),
                                                   Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH, MODULE_2_DEPENDENCY_2_PATH), FILENAME_2,
                                                          List.of(MODULE_2_DEPENDENCY_2_PATH)),
                                                   Map.of(FILENAME, CONTENT_KEY_VALUE_PAIR, FILENAME_2, CONTENT_KEY_VALUE_PAIR_2),
                                                   Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)), MODULE_NAME_2,
                                                          Map.of(DEPENDENCY_NAME_2, Map.of(CONFIG,
                                                                                           Map.of(FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2, FILE_CONTENT_KEY,
                                                                                                  FILE_CONTENT_VALUE)), DEPENDENCY_NAME,
                                                                 Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)))

                                      )));
    }

    @ParameterizedTest
    @MethodSource
    public void testModuleResolve(List<Module> moduleList, Map<String, List<String>> archiveEntries, Map<String, Map<String, Object>> fileContentEntries, Map<String, Map<String, Object>> resolvedModulesResult) {
        descriptor.setModules(moduleList);
        Mockito.when(helper.getRequiresDependenciesFileAttributes())
               .thenReturn(archiveEntries);

        for (var entry : fileContentEntries.keySet()) {
            Mockito.when(processor.processFileContent(any(), any(), eq(entry)))
                   .thenReturn(fileContentEntries.get(entry));
        }

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);

        assertResultsMatchActualModule(resolvedModulesResult, descriptor);

    }

    @Test
    public void testWithExistingConfigModuleNoOverwrite() {
        Map<String, Object> fileContentForEntry1 = new HashMap<>();
        fileContentForEntry1.put(FILE_CONTENT_KEY, FILE_CONTENT_VALUE);

        Map<String, List<String>> entries = new HashMap<>();
        entries.put(FILENAME, List.of(MODULE_DEPENDENCY_PATH));

        Mockito.when(processor.processFileContent(any(), any(), eq(FILENAME)))
               .thenReturn(fileContentForEntry1);

        Map<String, Object> existingParameters = new HashMap<>();
        Map<String, String> configMap = new HashMap<>();
        configMap.put(FILE_CONTENT_KEY, "shouldNotOverrideValue");
        existingParameters.put(CONFIG, configMap);

        Module existingModule = Module.createV3()
                                      .setName(MODULE_NAME)
                                      .setRequiredDependencies(List.of((RequiredDependency.createV3()
                                                                                          .setName(DEPENDENCY_NAME)
                                                                                          .setParameters(existingParameters))));
        List<Module> modulesList = List.of(existingModule);
        descriptor.setModules(modulesList);
        Mockito.when(helper.getRequiresDependenciesFileAttributes())
               .thenReturn(entries);

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);

        Map<String, Map<String, Object>> resolvedModulesResult = Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, Map.of(FILE_CONTENT_KEY,
                                                                                                                                   "shouldNotOverrideValue"))));

        assertResultsMatchActualModule(resolvedModulesResult, descriptor);

    }

    @Test
    public void testModuleAndResourceResolve() {

        descriptor.setModules(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME_2)));
        descriptor.setResources(List.of(createResource(RESOURCE_NAME), createResource(RESOURCE_NAME_2)));

        Mockito.when(helper.getRequiresDependenciesFileAttributes())
               .thenReturn(Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH, MODULE_2_DEPENDENCY_2_PATH), FILENAME_2, List.of(MODULE_2_DEPENDENCY_2_PATH)));

        Mockito.when(helper.getResourceFileAttributes())
               .thenReturn(Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2), FILENAME_2, List.of(RESOURCE_NAME_2)));

        Map<String, Map<String, Object>> fileContentEntries = Map.of(FILENAME, Map.of(FILE_CONTENT_KEY, FILE_CONTENT_VALUE), FILENAME_2,
                                                                     Map.of(FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2));

        for (var entry : fileContentEntries.keySet()) {
            Mockito.when(processor.processFileContent(any(), any(), eq(entry)))
                   .thenReturn(fileContentEntries.get(entry));
        }

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);

        Map<String, Map<String, Object>> resolvedModulesResult = Map.of(MODULE_NAME, Map.of(DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)),
                                                                        MODULE_NAME_2, Map.of(DEPENDENCY_NAME_2, Map.of(CONFIG, Map.of(FILE_CONTENT_KEY_2,
                                                                                                                                       FILE_CONTENT_VALUE_2,
                                                                                                                                       FILE_CONTENT_KEY,
                                                                                                                                       FILE_CONTENT_VALUE)),
                                                                                              DEPENDENCY_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR)));
        Map<String, Object> resolvedResourcesResult = Map.of(RESOURCE_NAME, Map.of(CONFIG, CONTENT_KEY_VALUE_PAIR), RESOURCE_NAME_2, Map.of(CONFIG, Map.of(
            FILE_CONTENT_KEY, FILE_CONTENT_VALUE, FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2)));

        assertResultMatchesActualResources(resolvedResourcesResult, descriptor);

        assertResultsMatchActualModule(resolvedModulesResult, descriptor);

    }

    @Test
    public void testIfContentSizeTrackerIsCalled() {
        descriptor.setModules(List.of(createModule(MODULE_NAME, DEPENDENCY_NAME), createModule(MODULE_NAME_2, DEPENDENCY_NAME_2)));
        descriptor.setResources(List.of(createResource(RESOURCE_NAME), createResource(RESOURCE_NAME_2)));

        Mockito.when(helper.getRequiresDependenciesFileAttributes())
               .thenReturn(Map.of(FILENAME, List.of(MODULE_DEPENDENCY_PATH, MODULE_2_DEPENDENCY_2_PATH), FILENAME_2, List.of(MODULE_2_DEPENDENCY_2_PATH)));

        Mockito.when(helper.getResourceFileAttributes())
               .thenReturn(Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2), FILENAME_2, List.of(RESOURCE_NAME_2)));

        Map<String, Map<String, Object>> fileContentEntries = Map.of(FILENAME, Map.of(FILE_CONTENT_KEY, FILE_CONTENT_VALUE), FILENAME_2,
                                                                     Map.of(FILE_CONTENT_KEY_2, FILE_CONTENT_VALUE_2));

        for (var entry : fileContentEntries.keySet()) {
            Mockito.when(processor.processFileContent(any(), any(), eq(entry)))
                   .thenReturn(fileContentEntries.get(entry));
        }

        resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);
        verify(sizeTracker, times(6)).incrementFileSize();
    }

    @Test
    public void testIfContentSizeTrackerThrowsException() {
        descriptor.setResources(List.of(createResource(RESOURCE_NAME)));

        Mockito.when(helper.getResourceFileAttributes())
               .thenReturn(Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2)));

        Map<String, Map<String, Object>> fileContentEntries = Map.of(FILENAME, Map.of(FILE_CONTENT_KEY, FILE_CONTENT_VALUE));

        for (var entry : fileContentEntries.keySet()) {
            Mockito.when(processor.processFileContent(any(), any(), eq(entry)))
                   .thenReturn(fileContentEntries.get(entry));
        }
        Mockito.when(sizeTracker.getTotalSize())
               .thenReturn(1L);
        Mockito.when(configuration.getMaxResolvedExternalContentSize())
               .thenReturn(0L);

        assertThrows(SLException.class, () -> {
            resolver.resolveMtaArchiveFilesInDescriptor(SAMPLE_SPACE, SAMPLE_SPACE_ID, helper);
        });
        verify(sizeTracker, times(1)).incrementFileSize();
    }

    private static void assertResultsMatchActualModule(Map<String, Map<String, Object>> resolvedModulesResult, DeploymentDescriptor descriptor) {
        for (var module : resolvedModulesResult.keySet()) {
            List<RequiredDependency> resolvedDependencies = descriptor.getModules()
                                                                      .stream()
                                                                      .filter(resolvedModule -> module.equals(resolvedModule.getName()))
                                                                      .findFirst()
                                                                      .get()
                                                                      .getRequiredDependencies();
            for (RequiredDependency dependency : resolvedDependencies) {
                assertEquals(resolvedModulesResult.get(module)
                                                  .get(dependency.getName()), dependency.getParameters());
            }
        }
    }

    private static Module createModule(String moduleName, String dependencyName) {
        return Module.createV3()
                     .setName(moduleName)
                     .setRequiredDependencies(List.of((RequiredDependency.createV3()
                                                                         .setName(dependencyName))));
    }
}
