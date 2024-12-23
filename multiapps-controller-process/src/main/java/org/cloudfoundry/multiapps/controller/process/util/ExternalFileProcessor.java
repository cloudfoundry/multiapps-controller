package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExternalFileProcessor {

    private final ContentLengthTracker sizeTracker;

    private final ProcessContext context;
    private final ApplicationConfiguration configuration;

    private final ArchiveEntryExtractor archiveEntryExtractor;

    public ExternalFileProcessor(ContentLengthTracker sizeTracker, ApplicationConfiguration configuration,
                                 ArchiveEntryExtractor archiveEntryExtractor, ProcessContext context) {
        this.sizeTracker = sizeTracker;
        this.configuration = configuration;
        this.archiveEntryExtractor = archiveEntryExtractor;
        this.context = context;
    }

    public Map<String, Object> processFileContent(String appArchiveId, Map.Entry<String, List<String>> fileManifestEntry) {
        String fileName = fileManifestEntry.getKey();
        ArchiveEntryWithStreamPositions fileArchiveEntry = ArchiveEntryExtractorUtil.findEntry(fileName, context.getVariable(
            Variables.ARCHIVE_ENTRIES_POSITIONS));
        byte[] parametersFile = archiveEntryExtractor.extractEntryBytes(ImmutableFileEntryProperties.builder()
                                                                                                    .guid(appArchiveId)
                                                                                                    .name(fileArchiveEntry.getName())
                                                                                                    .spaceGuid(context.getRequiredVariable(
                                                                                                        Variables.SPACE_GUID))
                                                                                                    .maxFileSizeInBytes(
                                                                                                        configuration.getMaxResourceFileSize())
                                                                                                    .build(), fileArchiveEntry);
        trackSize(parametersFile, fileManifestEntry.getValue());
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(parametersFile)) {
            return JsonUtil.convertJsonToMap(byteArrayInputStream);
        } catch (IOException e) {
            throw new SLException(e, org.cloudfoundry.multiapps.controller.process.Messages.ERROR_RETRIEVING_MTA_RESOURCE_CONTENT,
                                  fileName);
        }
    }

    private void trackSize(byte[] parametersFile, List<String> resourcesForFile) {
        int resourcesCount = resourcesForFile.size();
        sizeTracker.addToTotalFileSize(parametersFile.length * resourcesCount);
        verifyMaxContentSizeIsNotExceeded();
    }

    private void verifyMaxContentSizeIsNotExceeded() {
        if (sizeTracker.getTotalSize() > configuration.getMaxResolvedExternalContentSize()) {
            throw new SLException(Messages.ERROR_RESOLVED_FILE_CONTENT_IS_0_WHICH_IS_LARGER_THAN_MAX_1, sizeTracker.getTotalSize(),
                                  configuration.getMaxResolvedExternalContentSize());
        }
    }

}
