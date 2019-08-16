package com.sap.cloud.lm.sl.cf.process.util;

import java.io.File;
import java.io.IOException;
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
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ArchiveMergerTest {

    private static final String DEFAULT_SPACE_ID = "id";
    private static final String DEFAULT_SERVICE_ID = "service_id";
    private static final String ARCHIVE_FINAL_NAME = "archive";
    private static final String ID = "id";
    private static final String DEFAULT_NAMESPACE = "namespace";

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
    public void testCreateArchiveFromPartsSuccessfulMerge() throws FileStorageException {
        mockContextValid();
        archiveMerger.createArchiveFromParts(createFileEntriesFromFile("file-entries-1.json"));
        Mockito.verify(context, Mockito.times(1))
               .setVariable(Constants.PARAM_APP_ARCHIVE_ID, ID);
    }

    private void mockContextValid() throws FileStorageException {
        Mockito.when(context.getVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID))
               .thenReturn(DEFAULT_SPACE_ID);
        Mockito.when(context.getVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SERVICE_ID))
               .thenReturn(DEFAULT_SERVICE_ID);
        Mockito.when(fileService.addFile(Mockito.eq(DEFAULT_SPACE_ID), Mockito.eq(DEFAULT_SERVICE_ID), Mockito.eq(ARCHIVE_FINAL_NAME),
                                         Mockito.any(File.class)))
               .thenReturn(createFileEntry(ID, ARCHIVE_FINAL_NAME, DEFAULT_NAMESPACE));
    }

    private FileEntry createFileEntry(String id, String name, String namespace) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setId(id);
        fileEntry.setName(name);
        fileEntry.setNamespace(namespace);
        return fileEntry;
    }

    @Test
    public void testCreateArchiveFromPartsFileStorageException() throws FileStorageException {
        Mockito.when(fileService.addFile(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
               .thenThrow(FileStorageException.class);
        Assertions.assertThrows(SLException.class,
                                () -> archiveMerger.createArchiveFromParts(createFileEntriesFromFile("file-entries-1.json")));
    }

    @Test
    public void testCreateArchiveFromPartsFileIOException() throws FileStorageException {
        Mockito.when(fileService.addFile(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class)))
               .thenThrow(IOException.class);
        Assertions.assertThrows(SLException.class,
                                () -> archiveMerger.createArchiveFromParts(createFileEntriesFromFile("file-entries-1.json")));
    }

    @Test
    public void testSortFileEntries() {
        List<FileEntry> randomSortedFileEntries = createFileEntriesFromFile("random-sorted-file-entries.json");
        List<FileEntry> expectedFileEntries = createFileEntriesFromFile("expected-file-entries.json");
        List<FileEntry> sortedFileEntries = archiveMerger.sort(randomSortedFileEntries);
        Assertions.assertIterableEquals(getFileEntriesNames(expectedFileEntries), getFileEntriesNames(sortedFileEntries));
    }

    @Test
    public void testSortFileEntriesWithInvalidNames() {
        List<FileEntry> invalidFileEntries = createFileEntriesFromFile("file-entries-with-invalid-names.json");
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                                                                     () -> archiveMerger.sort(invalidFileEntries));
        Assertions.assertEquals(Messages.INVALID_FILE_ENTRY_NAME, exception.getMessage());
    }

    @Test
    public void testSortFileEntriesWithNamesWhichContainPartButDoNotContainIndexes() {
        List<FileEntry> invalidFileEntries = createFileEntriesFromFile("file-entries-with-invalid-names-no-indexes.json");
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                                                                     () -> archiveMerger.sort(invalidFileEntries));
        Assertions.assertEquals(Messages.INVALID_FILE_ENTRY_NAME, exception.getMessage());
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
}
