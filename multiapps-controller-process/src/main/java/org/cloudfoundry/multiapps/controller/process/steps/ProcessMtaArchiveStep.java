package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.stream.ImmutableArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class ProcessMtaArchiveStep extends SyncFlowableStep {

    private static final int EOCD_SIGNATURE = 0x06054b50; // End of Central Directory Signature
    private static final int CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50; // Central Directory Header Signature
    private static final int LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50; // Local File Header Signature
    private static final int EOCD_MIN_SIZE = 22; // Minimum size of EOCD for non-ZIP64 format

    @Inject
    private OperationService operationService;
    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);

        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);
        try {
            processApplicationArchive(context, appArchiveId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setMtaIdForProcess(context);
        acquireOperationLock(context);

        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

    private void processApplicationArchive(ProcessContext context, String appArchiveId) throws FileStorageException, IOException {

        List<ArchiveEntryWithStreamPositions> archiveEntries = new ArrayList<>();

        String archiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        String[] split = archiveId.split(",");
        List<FileEntry> fileEntries = Arrays.stream(split)
                                            .map(id -> {
                                                try {
                                                    return fileService.getFile(context.getRequiredVariable(Variables.SPACE_GUID), id);
                                                } catch (FileStorageException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            })
                                            .collect(Collectors.toList());

        long sizeOfAllFiles = fileEntries.stream()
                                         .mapToLong(fileEntry -> fileEntry.getSize()
                                                                          .longValue())
                                         .sum();

        List<ZipFileEntry> zipFileEntries = readCentralDirectory(context, appArchiveId, sizeOfAllFiles);
        List<ZipFileEntry> updatedEntries = new ArrayList<>();
        fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, appArchiveStream -> {
            try (CountingInputStream countingInputStream = new CountingInputStream(appArchiveStream)) {
                for (ZipFileEntry entry : zipFileEntries) {
                    entry = readLocalFileHeader(countingInputStream, entry);
                    updatedEntries.add(entry);
                    System.out.println("File: " + entry.fileName + " | Data Start: " + entry.dataStartOffset + " | Data End: "
                        + entry.dataEndOffset);
                }
                return null;
            }
        });
        for (ZipFileEntry updatedEntry : updatedEntries) {
            archiveEntries.add(ImmutableArchiveEntryWithStreamPositions.builder()
                                                                       .name(updatedEntry.fileName)
                                                                       .startPosition(updatedEntry.dataStartOffset)
                                                                       .endPosition(updatedEntry.dataEndOffset)
                                                                       .compressionMethod(updatedEntry.compressionMethod)
                                                                       .build());
        }

        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, archiveEntries);
        StringBuilder archiveEntriesInfo = new StringBuilder();
        archiveEntries.stream()
                      .map(entry -> String.format("Entry: %s, start index: %d, end index: %d\r\n", entry.getName(),
                                                  entry.getStartPosition(), entry.getEndPosition()))
                      .forEach(archiveEntriesInfo::append);
        context.getStepLogger()
               .info("Archive entries: " + archiveEntriesInfo);

        ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = archiveEntries.stream()
                                                                                        .filter(e -> e.getName()
                                                                                                      .equals("META-INF/mtad.yaml"))
                                                                                        .findFirst()
                                                                                        .get();
        MtaArchiveHelper helper = createMtaArchiveHelperFromManifest(context, archiveEntries);
        MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
        addMtaArchiveModulesInMtaArchiveElements(context, helper, mtaArchiveElements);
        addMtaRequiredDependenciesInMtaArchiveElements(helper, mtaArchiveElements);
        addMtaArchiveResourcesInMtaArchiveElements(helper, mtaArchiveElements);
        context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);

        byte[] inflatedMtad = fileService.processFileContentWithOffset(context.getVariable(Variables.SPACE_GUID), appArchiveId,
                                                                       archiveStream -> {
                                                                           byte[] allBytes = archiveStream.readAllBytes();
                                                                           if (archiveEntryWithStreamPositions.getCompressionMethod() == 0) { // means no compression
                                                                               return allBytes;
                                                                           } else {
                                                                               return inflate(allBytes);
                                                                           }
                                                                       }, archiveEntryWithStreamPositions.getStartPosition(),
                                                                       archiveEntryWithStreamPositions.getEndPosition());

        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(new String(inflatedMtad));
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);

        context.getStepLogger()
               .info("THE mtad.yaml: " + new String(inflatedMtad));
    }

    public static byte[] inflate(byte[] compressedBytes) throws DataFormatException, IOException {
        // Create an inflater for decompressing the data
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressedBytes);

        // Output stream to hold decompressed data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length);
        byte[] buffer = new byte[1024]; // Buffer to hold chunks of decompressed data

        try {
            // Inflate the compressed data
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer); // Decompress into buffer
                outputStream.write(buffer, 0, count); // Write decompressed data to output stream
            }
        } catch (DataFormatException e) {
            throw new DataFormatException("Data format error while inflating: " + e.getMessage());
        } finally {
            inflater.end(); // Always call end() to release resources
            outputStream.close(); // Close the output stream
        }

        // Return the decompressed data
        return outputStream.toByteArray();
    }

    public List<ZipFileEntry> readCentralDirectory(ProcessContext context, String appArchiveId, long archiveSize)
        throws IOException, FileStorageException {
        long eocdOffset = findEOCD(context, appArchiveId, archiveSize);
        long centralDirOffset = fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, archiveStream -> {
            long centralDirectoryOffset;
            archiveStream.skipNBytes(eocdOffset);

            byte[] eocdBuffer = new byte[EOCD_MIN_SIZE];
            archiveStream.readNBytes(eocdBuffer, 0, EOCD_MIN_SIZE);
            ByteBuffer eocd = ByteBuffer.wrap(eocdBuffer)
                                        .order(ByteOrder.LITTLE_ENDIAN);

            // Read central directory size and offset
            int centralDirectorySize = eocd.getInt(12);
            centralDirectoryOffset = Integer.toUnsignedLong(eocd.getInt(16));
            return centralDirectoryOffset;
        });

        return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, archiveStream -> {
            // Move to the central directory offset
            archiveStream.skipNBytes(centralDirOffset);

            List<ZipFileEntry> entries = new ArrayList<>();
            while (true) {
                byte[] headerBuffer = new byte[46];
                if (archiveStream.read(headerBuffer) != 46) { // probably better readNBytes()
                    break; // No more headers to read
                }

                ByteBuffer header = ByteBuffer.wrap(headerBuffer)
                                              .order(ByteOrder.LITTLE_ENDIAN);

                if (header.getInt(0) != CENTRAL_DIRECTORY_SIGNATURE) {
                    break; // Central directory entry not found
                }

                int compressedSize = header.getInt(20);
                int uncompressedSize = header.getInt(24);

                int fileNameLength = header.getShort(28) & 0xffff;
                int extraFieldLength = header.getShort(30) & 0xffff;
                int fileCommentLength = header.getShort(32) & 0xffff;
                int relativeOffset = header.getInt(42);

                // Read file name
                byte[] fileNameBuffer = new byte[fileNameLength];
                archiveStream.readNBytes(fileNameBuffer, 0, fileNameLength);
                String fileName = new String(fileNameBuffer, "UTF-8");

                // Add entry to the list
                entries.add(new ZipFileEntry(fileName, relativeOffset, compressedSize));
                // Skip extra fields and file comments
                archiveStream.skipNBytes(extraFieldLength + fileCommentLength);
            }

            return entries;
        });

    }

    // find the end of the central directory
    private long findEOCD(ProcessContext context, String appArchiveId, long archiveSize) throws IOException, FileStorageException {
        return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, archiveStream -> {
            int maxEocdSearch = 65557;
            long searchLength = Math.min(maxEocdSearch, archiveSize);

            // Move the stream to the point where EOCD might be
            long skipTo = archiveSize - searchLength;
            context.getStepLogger()
                   .info("SKIP TO IS " + skipTo);
            archiveStream.skipNBytes(skipTo);

            context.getStepLogger()
                   .info("TYPE IS " + archiveStream.getClass());

            byte[] read = archiveStream.readNBytes((int) searchLength);
            // Copy the value of
            context.getStepLogger()
                   .info("READ IS " + read.length);
            if (read.length != searchLength) {
                throw new IOException("Unable to read the last part of the stream.");
            }

            // Search backwards in the buffer for the EOCD signature
            for (int i = read.length - EOCD_MIN_SIZE; i >= 0; i--) {
                if (ByteBuffer.wrap(read, i, 4)
                              .order(ByteOrder.LITTLE_ENDIAN)
                              .getInt() == EOCD_SIGNATURE) {
                    return skipTo + i; // Return the position of EOCD
                }
            }
            throw new IOException("End of Central Directory (EOCD) not found.");
        });
    }

    // Method to read local file header
    public static ZipFileEntry readLocalFileHeader(CountingInputStream countingStream, ZipFileEntry entry) throws IOException {
        // Skip to the entry's file offset

        long bytesToSkip = entry.fileOffset - countingStream.getByteCount();
        if (bytesToSkip > 0) {
            countingStream.skipNBytes(bytesToSkip); // most likely skipNBytes
        }

        // Read the Local File Header (30 bytes)
        byte[] localFileHeaderBuffer = new byte[30];
        countingStream.readNBytes(localFileHeaderBuffer, 0, 30);

        ByteBuffer localHeader = ByteBuffer.wrap(localFileHeaderBuffer)
                                           .order(ByteOrder.LITTLE_ENDIAN);

        // Check if the Local File Header signature is correct
        if (localHeader.getInt(0) != LOCAL_FILE_HEADER_SIGNATURE) {
            throw new IOException("Local File Header signature not found.");
        }

        // Extract the uncompressed size, file name length, and extra field length
        int uncompressedSize = localHeader.getInt(22); // Uncompressed size
        int fileNameLength = localHeader.getShort(26) & 0xffff;
        int extraFieldLength = localHeader.getShort(28) & 0xffff;

        // Calculate the data start offset based on the file offset and header sizes
        long dataStartOffset = entry.fileOffset + 30 + fileNameLength + extraFieldLength;
        long dataEndOffset = dataStartOffset + entry.compressedSize;

        // Update the entry with the calculated offsets
        entry.dataStartOffset = dataStartOffset;
        entry.dataEndOffset = dataEndOffset;
        int compressionMethod = localHeader.getShort(8) & 0xffff; // Compression method (2 bytes)
        entry.compressionMethod = compressionMethod;

        return entry;
    }

    private DeploymentDescriptor extractDeploymentDescriptor(InputStream appArchiveStream) {
        String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
        getStepLogger().debug("MTA Descriptor length: {0}", descriptorString.length());
        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        return descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
    }

    private MtaArchiveHelper createMtaArchiveHelperFromManifest(ProcessContext context,
                                                                List<ArchiveEntryWithStreamPositions> archiveEntries) {
        ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = archiveEntries.stream()
                                                                                        .filter(e -> e.getName()
                                                                                                      .equals("META-INF/MANIFEST.MF"))
                                                                                        .findFirst()
                                                                                        .get();

        try {
            return fileService.processFileContentWithOffset(context.getVariable(Variables.SPACE_GUID),
                                                            context.getRequiredVariable(Variables.APP_ARCHIVE_ID), archiveStream -> {
                                                                if (archiveEntryWithStreamPositions.getCompressionMethod() == 0) {
                                                                    Manifest manifest = new Manifest(archiveStream);
                                                                    MtaArchiveHelper helper = getHelper(manifest);
                                                                    helper.init();
                                                                    return helper;
                                                                } else {
                                                                    byte[] inflate = inflate(archiveStream.readAllBytes());
                                                                    InputStream inputStream = new ByteArrayInputStream(inflate);
                                                                    Manifest manifest = new Manifest(inputStream);
                                                                    MtaArchiveHelper helper = getHelper(manifest);
                                                                    helper.init();
                                                                    return helper;
                                                                }
                                                            }, archiveEntryWithStreamPositions.getStartPosition(),
                                                            archiveEntryWithStreamPositions.getEndPosition());
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void addMtaArchiveModulesInMtaArchiveElements(ProcessContext context, MtaArchiveHelper helper,
                                                          MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
        mtaArchiveModules.forEach(mtaArchiveElements::addModuleFileName);
        getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, mtaArchiveModules.keySet());
    }

    private void addMtaRequiredDependenciesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
        mtaArchiveRequiresDependencies.forEach(mtaArchiveElements::addRequiredDependencyFileName);
        getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());
    }

    private void addMtaArchiveResourcesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
        mtaArchiveResources.forEach(mtaArchiveElements::addResourceFileName);
        getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());
    }

    private void setMtaIdForProcess(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();

        context.setVariable(Variables.MTA_ID, mtaId);
    }

    private void acquireOperationLock(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, namespace, context.getVariable(Variables.SPACE_GUID),
                                              context.getVariable(Variables.CORRELATION_ID));
    }

    private static class ZipFileEntry {

        private String fileName;
        private long fileOffset;
        private long compressedSize;
        private long dataStartOffset;
        private int compressionMethod;
        private long dataEndOffset;

        public ZipFileEntry(String fileName, long fileOffset, long compressedSize) {
            this.fileName = fileName;
            this.fileOffset = fileOffset;
            this.compressedSize = compressedSize;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileOffset() {
            return fileOffset;
        }

        public void setFileOffset(long fileOffset) {
            this.fileOffset = fileOffset;
        }

        public long getCompressedSize() {
            return compressedSize;
        }

        public void setCompressedSize(long compressedSize) {
            this.compressedSize = compressedSize;
        }

        public long getDataStartOffset() {
            return dataStartOffset;
        }

        public void setDataStartOffset(long dataStartOffset) {
            this.dataStartOffset = dataStartOffset;
        }

        public long getDataEndOffset() {
            return dataEndOffset;
        }

        public void setDataEndOffset(long dataEndOffset) {
            this.dataEndOffset = dataEndOffset;
        }

    }

    static class CountingInputStream extends FilterInputStream {
        private final AtomicLong byteCount = new AtomicLong(0);

        protected CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            byteCount.addAndGet(skipped);
            return skipped;
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) {
                byteCount.incrementAndGet();
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            if (bytesRead != -1) {
                byteCount.addAndGet(bytesRead);
            }
            return bytesRead;
        }

        public long getByteCount() {
            return byteCount.get();
        }
    }

}
