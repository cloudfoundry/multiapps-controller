package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ExternalFileProcessorTest {

    private static final String RESOURCE_NAME = "resourceName";

    private static final String RESOURCE_NAME_2 = "resourceName_2";

    private static final String FILENAME = "fileName";

    private ExternalFileProcessor externalFileProcessor;

    @Mock
    private ApplicationConfiguration configuration;

    @Mock
    private ArchiveEntryExtractor archiveEntryExtractor;
    @Mock
    private ProcessContext context;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(context.getRequiredVariable(Variables.SPACE_GUID))
               .thenReturn("SPACE_GUID");
        externalFileProcessor = new ExternalFileProcessor(new ContentLengthTracker(), configuration, archiveEntryExtractor, context);
    }

    @Test
    void testFileProcessor() {
        Mockito.when(configuration.getMaxResolvedExternalContentSize())
               .thenReturn(66L);
        ArchiveEntryWithStreamPositions mockStreamPositions = Mockito.mock(ArchiveEntryWithStreamPositions.class);
        Mockito.when(mockStreamPositions.getName())
               .thenReturn(FILENAME);
        Mockito.when(context.getVariable(Variables.ARCHIVE_ENTRIES_POSITIONS))
               .thenReturn(List.of(mockStreamPositions));
        Map<String, Object> expectedParameters = Map.of("param1", "value1");
        when(archiveEntryExtractor.extractEntryBytes(any(), any())).thenReturn(JsonUtil.toJson(expectedParameters)
                                                                                       .getBytes());

        Map<String, List<String>> archiveRequiresDependenciesAttributes = Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2));
        for (var entry : archiveRequiresDependenciesAttributes.entrySet()) {
            Map<String, Object> result = externalFileProcessor.processFileContent("ARCHIVE_ID", entry);
            assertEquals(expectedParameters, result);
        }
    }

    @Test
    void testFileProcessorExceedMaxSize() {
        Mockito.when(configuration.getMaxResolvedExternalContentSize())
               .thenReturn(65L);
        ArchiveEntryWithStreamPositions mockStreamPositions = Mockito.mock(ArchiveEntryWithStreamPositions.class);
        Mockito.when(mockStreamPositions.getName())
               .thenReturn(FILENAME);
        Mockito.when(context.getVariable(Variables.ARCHIVE_ENTRIES_POSITIONS))
               .thenReturn(List.of(mockStreamPositions));
        when(archiveEntryExtractor.extractEntryBytes(any(), any())).thenReturn(JsonUtil.toJson(Map.of(RESOURCE_NAME, RESOURCE_NAME_2))
                                                                                       .getBytes());
        Map<String, List<String>> archiveRequiresDependenciesAttributes = Map.of(FILENAME, List.of(RESOURCE_NAME, RESOURCE_NAME_2));
        for (var entry : archiveRequiresDependenciesAttributes.entrySet()) {
            assertThrows(SLException.class, () -> {
                externalFileProcessor.processFileContent("ARCHIVE_ID", entry);
            });
        }

    }
}
