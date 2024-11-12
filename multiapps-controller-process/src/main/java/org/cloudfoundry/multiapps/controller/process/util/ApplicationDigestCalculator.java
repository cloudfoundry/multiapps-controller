package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
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
    private final ApplicationArchiveIterator applicationArchiveIterator;
    private final ArchiveEntryExtractor archiveEntryExtractor;

    @Inject
    public ApplicationDigestCalculator(FileService fileService, ApplicationArchiveIterator applicationArchiveIterator,
                                       ArchiveEntryExtractor archiveEntryExtractor) {
        this.fileService = fileService;
        this.applicationArchiveIterator = applicationArchiveIterator;
        this.archiveEntryExtractor = archiveEntryExtractor;
    }

    public String calculateApplicationDigest(ApplicationArchiveContext applicationArchiveContext) {
        try {
            iterateApplicationArchive(applicationArchiveContext);
            return applicationArchiveContext.getDigestCalculator()
                                            .getDigest();
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, applicationArchiveContext.getModuleFileName());
        }
    }

    private void iterateApplicationArchive(ApplicationArchiveContext applicationArchiveContext) throws FileStorageException {
        // TODO: backwards compatibility for one tact
        if (applicationArchiveContext.getArchiveEntryWithStreamPositions() == null) {
            fileService.consumeFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                           archiveStream -> calculateDigestFromDirectory(applicationArchiveContext, archiveStream));
            return;
        }
        // TODO: backwards compatibility for one tact
        if (ArchiveEntryExtractorUtil.containsDirectory(applicationArchiveContext.getModuleFileName(),
                                                        applicationArchiveContext.getArchiveEntryWithStreamPositions())) {
            fileService.consumeFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                           archiveStream -> calculateDigestFromDirectory(applicationArchiveContext, archiveStream));
        } else {
            ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = ArchiveEntryExtractorUtil.findEntry(applicationArchiveContext.getModuleFileName(),
                                                                                                                  applicationArchiveContext.getArchiveEntryWithStreamPositions());
            DigestCalculator applicationDigestCalculator = applicationArchiveContext.getDigestCalculator();
            archiveEntryExtractor.processFileEntryContent(ImmutableFileEntryProperties.builder()
                                                                                      .guid(applicationArchiveContext.getAppArchiveId())
                                                                                      .name(archiveEntryWithStreamPositions.getName())
                                                                                      .spaceGuid(applicationArchiveContext.getSpaceId())
                                                                                      .maxFileSizeInBytes(applicationArchiveContext.getMaxSizeInBytes())
                                                                                      .build(),
                                                          archiveEntryWithStreamPositions, (bytesBuffer, bytesRead) -> {
                                                              applicationArchiveContext.calculateCurrentSizeInBytes(bytesRead);
                                                              applicationDigestCalculator.updateDigest(bytesBuffer, 0, bytesRead);
                                                          });
        }
    }

    private void calculateDigestFromDirectory(ApplicationArchiveContext applicationArchiveContext, InputStream archiveStream)
        throws IOException {
        try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(archiveStream)) {
            String moduleFileName = applicationArchiveContext.getModuleFileName();
            ZipEntry zipEntry = applicationArchiveIterator.getFirstZipEntry(applicationArchiveContext.getModuleFileName(),
                                                                            zipArchiveInputStream);
            do {
                if (!zipEntry.isDirectory()) {
                    calculateDigestFromArchive(applicationArchiveContext, zipArchiveInputStream);
                }
            } while ((zipEntry = applicationArchiveIterator.getNextEntryByName(moduleFileName, zipArchiveInputStream)) != null);
        }
    }

    private void calculateDigestFromArchive(ApplicationArchiveContext applicationArchiveContext, InputStream inputStream)
        throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        DigestCalculator applicationDigestCalculator = applicationArchiveContext.getDigestCalculator();
        while ((numberOfReadBytes = inputStream.read(buffer)) != -1) {
            long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
            if (currentSizeInBytes + numberOfReadBytes > maxSizeInBytes) {
                throw new ContentException(org.cloudfoundry.multiapps.mta.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                           currentSizeInBytes + numberOfReadBytes,
                                           applicationArchiveContext.getModuleFileName(),
                                           maxSizeInBytes);
            }
            applicationArchiveContext.calculateCurrentSizeInBytes(numberOfReadBytes);
            applicationDigestCalculator.updateDigest(buffer, 0, numberOfReadBytes);
        }
    }

}