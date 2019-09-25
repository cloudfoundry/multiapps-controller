package com.sap.cloud.lm.sl.cf.process.util;

import static org.mockito.ArgumentMatchers.any;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ArchiveMergerTest {

    private static final String FILE_ENTRIES = "file-entries-1.json";
    private static final String RANDOM_SORTED_ENTRIES = "random-sorted-file-entries.json";
    private static final String EXPECTED_FILE_ENTRIES = "expected-file-entries.json";
    private static final String FILE_ENTRIES_WITH_INVALID_NAMES = "file-entries-with-invalid-names.json";
    private static final String FILE_ENTRIES_WITHOUT_INDEXES = "file-entries-with-invalid-names-no-indexes.json";
    private static final String FILE_ENTRY_WITHOUT_PARTS = "file-entry-without-parts.json";

    private ArchiveMerger archiveMerger;

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private DelegateExecution context;
    @Mock
    private Logger logger;
    @Spy
    private ApplicationConfiguration configuration;

    public ArchiveMergerTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    public void setUp() {
        archiveMerger = new ArchiveMerger(fileService, stepLogger, context);
    }

    @Test
    public void testCreateArchiveFromPartsFileStorageExceptionThrown() throws FileStorageException {
        Mockito.doThrow(FileStorageException.class)
               .when(fileService)
               .processFileContent(any());
        Assertions.assertThrows(SLException.class, () -> archiveMerger.createArchiveFromParts(createFileEntriesFromFile(FILE_ENTRIES)));
    }

    @Test
    public void testCreateArchiveFromParts() throws FileStorageException {
        List<FileEntry> fileEntries = createFileEntriesFromFile(FILE_ENTRIES);
        Path archiveFromParts = archiveMerger.createArchiveFromParts(fileEntries);
        Assertions.assertTrue(archiveFromParts.toString()
                                              .endsWith(getArchiveName(fileEntries.get(0))));
    }

    @Test
    public void testSortFileEntries() {
        List<FileEntry> randomSortedFileEntries = createFileEntriesFromFile(RANDOM_SORTED_ENTRIES);
        List<FileEntry> expectedFileEntries = createFileEntriesFromFile(EXPECTED_FILE_ENTRIES);
        List<FileEntry> sortedFileEntries = archiveMerger.sort(randomSortedFileEntries);
        Assertions.assertIterableEquals(getFileEntriesNames(expectedFileEntries), getFileEntriesNames(sortedFileEntries));
    }

    @Test
    public void testSortFileEntriesWithInvalidNames() {
        List<FileEntry> invalidFileEntries = createFileEntriesFromFile(FILE_ENTRIES_WITH_INVALID_NAMES);
        Assertions.assertThrows(SLException.class, () -> archiveMerger.sort(invalidFileEntries));
    }

    @Test
    public void testSortFileEntriesWithNamesWhichContainPartButDoNotContainIndexes() {
        List<FileEntry> invalidFileEntries = createFileEntriesFromFile(FILE_ENTRIES_WITHOUT_INDEXES);
        Assertions.assertThrows(SLException.class, () -> archiveMerger.sort(invalidFileEntries));
    }

    @Test
    public void testWithArchiveNameWithoutParts() throws FileStorageException {
        List<FileEntry> fileEntryWithoutParts = createFileEntriesFromFile(FILE_ENTRY_WITHOUT_PARTS);
        archiveMerger.createArchiveFromParts(fileEntryWithoutParts);
        Mockito.verify(fileService)
               .processFileContent(any());
    }

    private List<String> getFileEntriesNames(List<FileEntry> fileEntries) {
        return fileEntries.stream()
                          .map(FileEntry::getName)
                          .collect(Collectors.toList());
    }

    private List<FileEntry> createFileEntriesFromFile(String fileName) {
        FileEntry[] fileEntries = JsonUtil.fromJson(TestUtil.getResourceAsString(fileName, getClass()), FileEntry[].class);
        return Arrays.asList(fileEntries);
    }

    private String getArchiveName(FileEntry fileEntry) {
        return fileEntry.getName()
                        .split("\\.")[0];
    }
}
