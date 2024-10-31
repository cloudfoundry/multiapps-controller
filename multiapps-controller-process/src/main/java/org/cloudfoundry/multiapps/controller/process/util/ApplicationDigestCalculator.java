package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ApplicationDigestCalculator {

    protected static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;
    private final ApplicationArchiveReader applicationArchiveReader;
    private final ArchiveEntryExtractor archiveEntryExtractor;

    @Inject
    public ApplicationDigestCalculator(FileService fileService, ApplicationArchiveReader applicationArchiveReader,
                                       ArchiveEntryExtractor archiveEntryExtractor) {
        this.fileService = fileService;
        this.applicationArchiveReader = applicationArchiveReader;
        this.archiveEntryExtractor = archiveEntryExtractor;
    }

    public String calculateApplicationDigest(ApplicationArchiveContext applicationArchiveContext) {
        try {
            iterateApplicationArchive(applicationArchiveContext);
            return applicationArchiveContext.getApplicationDigestCalculator()
                                            .getDigest();
        } catch (IOException | FileStorageException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    private void iterateApplicationArchive(ApplicationArchiveContext applicationArchiveContext) throws IOException, FileStorageException {
        if (ArchiveEntryExtractorUtil.hasDirectory(applicationArchiveContext.getModuleFileName(),
                                                   applicationArchiveContext.getArchiveEntryWithStreamPositions())) {
            fileService.consumeFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                           archiveStream -> calculateDigestFromDirectory(applicationArchiveContext, archiveStream));
        } else {
            ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = ArchiveEntryExtractorUtil.findEntry(applicationArchiveContext.getModuleFileName(),
                                                                                                                  applicationArchiveContext.getArchiveEntryWithStreamPositions());
            DigestCalculator applicationDigestCalculator = applicationArchiveContext.getApplicationDigestCalculator();
            archiveEntryExtractor.processFileEntryContent(ImmutableFileEntryProperties.builder()
                                                                                      .guid(applicationArchiveContext.getAppArchiveId())
                                                                                      .spaceGuid(applicationArchiveContext.getSpaceId())
                                                                                      .maxFileSize(applicationArchiveContext.getMaxSizeInBytes())
                                                                                      .build(),
                                                          archiveEntryWithStreamPositions, (bytesBuffer, bytesRead) -> {
                                                              applicationArchiveContext.calculateCurrentSizeInBytes(bytesRead);
                                                              applicationDigestCalculator.updateDigest(bytesBuffer, 0, bytesRead);
                                                          });
        }
    }

    private void calculateDigestFromDirectory(ApplicationArchiveContext applicationArchiveContext, InputStream archiveStream)
        throws IOException {
        try (ZipInputStream zipArchiveInputStream = new ZipInputStream(archiveStream)) {
            String moduleFileName = applicationArchiveContext.getModuleFileName();
            ZipEntry zipEntry = applicationArchiveReader.getFirstZipEntry(applicationArchiveContext, zipArchiveInputStream);
            do {
                if (!zipEntry.isDirectory()) {
                    calculateDigestFromArchive(applicationArchiveContext, zipArchiveInputStream);
                }
            } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
        }
    }

    protected void calculateDigestFromArchive(ApplicationArchiveContext applicationArchiveContext, InputStream inputStream)
        throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        DigestCalculator applicationDigestCalculator = applicationArchiveContext.getApplicationDigestCalculator();
        while ((numberOfReadBytes = inputStream.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
            }
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
            applicationDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
        }
    }

}