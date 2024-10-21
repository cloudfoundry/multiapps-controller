package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;

@Named
public class ApplicationDigestCalculator {

    protected static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;
    private final ApplicationArchiveReader applicationArchiveReader;

    @Inject
    public ApplicationDigestCalculator(FileService fileService, ApplicationArchiveReader applicationArchiveReader) {
        this.fileService = fileService;
        this.applicationArchiveReader = applicationArchiveReader;
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

        List<ArchiveEntryWithStreamPositions> entries = applicationArchiveContext.getArchiveEntryWithStreamPositions()
                                                                                 .stream()
                                                                                 .filter(entry -> entry.getName()
                                                                                                       .startsWith(applicationArchiveContext.getModuleFileName()))
                                                                                 .toList();
        if (entries.stream()
                   .anyMatch(ArchiveEntryWithStreamPositions::isDirectory)) {
            System.out.println("DIRECTORY FOUND FOR APP DIGEST CALC");
            fileService.processFileContent(applicationArchiveContext.getSpaceId(), applicationArchiveContext.getAppArchiveId(),
                                           archiveStream -> {
                                               try (ZipInputStream zipArchiveInputStream = new ZipInputStream(archiveStream)) {
                                                   String moduleFileName = applicationArchiveContext.getModuleFileName();
                                                   ZipEntry zipEntry = applicationArchiveReader.getFirstZipEntry(applicationArchiveContext,
                                                                                                                 zipArchiveInputStream);
                                                   do {
                                                       if (!zipEntry.isDirectory()) {
                                                           calculateDigestFromArchive(applicationArchiveContext, zipArchiveInputStream);
                                                       }
                                                   } while ((zipEntry = applicationArchiveReader.getNextEntryByName(moduleFileName,
                                                                                                                    zipArchiveInputStream)) != null);
                                               }
                                               return null;
                                           });

        } else {
            System.out.println("DRAKULATA ");
            for (ArchiveEntryWithStreamPositions archiveEntryWithStreamPosition : entries) {
                try {
                    System.out.println("ITERATING ENTRY: " + archiveEntryWithStreamPosition.getName());
                    fileService.processFileContentWithOffset(applicationArchiveContext.getSpaceId(),
                                                             applicationArchiveContext.getAppArchiveId(), fileEntryStream -> {
                                                                 calculateDigestFromArchive(applicationArchiveContext, fileEntryStream);
                                                                 return null;
                                                             }, archiveEntryWithStreamPosition.getStartPosition(),
                                                             archiveEntryWithStreamPosition.getEndPosition());
                } catch (FileStorageException e) {
                    throw new SLException(e, e.getMessage());
                }
            }
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