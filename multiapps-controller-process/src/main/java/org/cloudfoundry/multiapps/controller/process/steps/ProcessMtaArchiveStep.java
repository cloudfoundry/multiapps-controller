package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.stream.ImmutableArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.InflatorUtil;
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

        List<ZipFileEntry> zipFileEntries = parseEntries(context, appArchiveId, sizeOfAllFiles);

        for (ZipFileEntry updatedEntry : zipFileEntries) {
            archiveEntries.add(ImmutableArchiveEntryWithStreamPositions.builder()
                                                                       .name(updatedEntry.fileName)
                                                                       .startPosition(updatedEntry.dataStartOffset)
                                                                       .endPosition(updatedEntry.dataEndOffset)
                                                                       .compressionMethod(updatedEntry.compressionMethod)
                                                                       .isDirectory(updatedEntry.isDirectory)
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
                                                                           if (archiveEntryWithStreamPositions.getCompressionMethod() == 0) { // means
                                                                                                                                              // no
                                                                                                                                              // compression
                                                                               return allBytes;
                                                                           } else {
                                                                               return InflatorUtil.inflate(allBytes, configuration.getMaxMtaDescriptorSize());
                                                                           }
                                                                       }, archiveEntryWithStreamPositions.getStartPosition(),
                                                                       archiveEntryWithStreamPositions.getEndPosition());

        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(new String(inflatedMtad));
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);

        context.getStepLogger()
               .debug("THE mtad.yaml: " + new String(inflatedMtad));
    }

    private List<ZipFileEntry> parseEntries(ProcessContext context, String appArchiveId, long sizeOfAllFiles) throws FileStorageException {
        return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, archiveStream -> {
            List<ZipFileEntry> zipFileEntries = new ArrayList<>();

            try (var c = new CountingInputStream(archiveStream);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(c, 16 * 1024);
                ZipArchiveInputStream zf = new ZipArchiveInputStream(bufferedInputStream)) {
                // Get the zip entry
                ZipArchiveEntry entry = zf.getNextEntry();
                while (entry != null) {
                    validateEntry(entry);
                    long startOffset = entry.getDataOffset(); // Apache Commons method
                    long endOffset = startOffset;
                    long read;
                    long act = 0;
                    byte[] buffer = new byte[16 * 1024];
                    while ((read = zf.read(buffer, 0, buffer.length)) != -1) {
                    }
                    endOffset += zf.getCompressedCount();

                    ZipFileEntry zipFileEntry = new ZipFileEntry(entry.getName(),
                                                                 startOffset,
                                                                 endOffset,
                                                                 entry.getMethod(),
                                                                 entry.isDirectory());
                    zipFileEntries.add(zipFileEntry);
                    entry = zf.getNextEntry();
                }
            }
            return zipFileEntries;
        });
    }

    protected void validateEntry(ZipEntry entry) {
        FileUtils.validatePath(entry.getName());
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
                                                                    byte[] inflate = InflatorUtil.inflate(archiveStream.readAllBytes(), configuration.getMaxManifestSize());
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
        private long dataStartOffset;
        private long dataEndOffset;
        private int compressionMethod;
        private boolean isDirectory;

        public ZipFileEntry(String fileName, long dataStartOffset, long dataEndOffset, int compressionMethod, boolean isDirectory) {
            this.fileName = fileName;
            this.dataStartOffset = dataStartOffset;
            this.dataEndOffset = dataEndOffset;
            this.compressionMethod = compressionMethod;
            this.isDirectory = isDirectory;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getDataStartOffset() {
            return dataStartOffset;
        }

        public void setDataStartOffset(long dataStartOffset) {
            this.dataStartOffset = dataStartOffset;
        }

        public int getCompressionMethod() {
            return compressionMethod;
        }

        public void setCompressionMethod(int compressionMethod) {
            this.compressionMethod = compressionMethod;
        }

        public long getDataEndOffset() {
            return dataEndOffset;
        }

        public void setDataEndOffset(long dataEndOffset) {
            this.dataEndOffset = dataEndOffset;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public void setDirectory(boolean directory) {
            isDirectory = directory;
        }
    }

    static class CountingInputStream extends FilterInputStream {
        public final AtomicLong byteCount = new AtomicLong(0);

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
