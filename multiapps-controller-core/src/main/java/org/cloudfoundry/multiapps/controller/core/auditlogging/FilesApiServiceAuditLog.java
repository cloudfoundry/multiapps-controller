package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;

public class FilesApiServiceAuditLog {

    private static final String NAMESPACE_PROPERTY_NAME = "namespace";
    private static final String FILE_URL_PROPERTY_NAME = "fileUrl";
    private static final String JOB_ID_PROPERTY_NAME = "jobId";
    private static final String DIGEST_ALGORITHM_PROPERTY_NAME = "digestAlgorithm";
    private static final String FILE_ID_PROPERTY_NAME = "fileId";
    private static final String SIZE_PROPERTY_NAME = "size";
    private static final String DIGEST_PROPERTY_NAME = "digest";

    private final AuditLoggingFacade auditLoggingFacade;

    public FilesApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logGetFiles(String username, String spaceGuid, String namespace) {
        String performedAction = MessageFormat.format(Messages.LIST_FILES_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceGuid,
                                                                           performedAction,
                                                                           Messages.FILE_INFO_AUDIT_LOG_CONFIG,
                                                                           createAuditLogGetFilesConfigurationIdentifier(namespace)));
    }

    public void logUploadFile(String username, String spaceGuid, FileMetadata fileMetadata) {
        String performedAction = MessageFormat.format(Messages.UPLOAD_FILE_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.FILE_UPLOAD_AUDIT_LOG_CONFIG,
                                                                                    createFileMetadataConfigurationIdentifier(
                                                                                        fileMetadata)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logStartUploadFromUrl(String username, String spaceGuid, String fileUrl) {
        String performedAction = MessageFormat.format(Messages.UPLOAD_FILE_FROM_URL_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.FILE_UPLOAD_FROM_URL_AUDIT_LOG_CONFIG,
                                                                                    createAuditLogStartUploadFromUrlConfigurationIdentifier(
                                                                                        fileUrl)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logGetUploadFromUrlJob(String username, String spaceGuid, String namespace, String jobId) {
        String performedAction = MessageFormat.format(Messages.GET_INFO_FOR_UPLOAD_URL_JOB_AUDIT_LOG_MESSAGE, spaceGuid);
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           spaceGuid,
                                                                           performedAction,
                                                                           Messages.UPLOAD_FROM_URL_JOB_INFO_AUDIT_LOG_CONFIG,
                                                                           createAuditLogGetUploadFromUrlJobConfigurationIdentifier(
                                                                               namespace,
                                                                               jobId)));
    }

    private Map<String, String> createFileMetadataConfigurationIdentifier(FileMetadata fileMetadata) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(FILE_ID_PROPERTY_NAME, fileMetadata.getId());
        identifiers.put(DIGEST_PROPERTY_NAME, fileMetadata.getDigest());
        identifiers.put(DIGEST_ALGORITHM_PROPERTY_NAME, fileMetadata.getDigestAlgorithm());
        identifiers.put(SIZE_PROPERTY_NAME, Objects.toString(fileMetadata.getSize()));
        identifiers.put(NAMESPACE_PROPERTY_NAME, fileMetadata.getNamespace());

        return identifiers;
    }

    private Map<String, String> createAuditLogGetFilesConfigurationIdentifier(String namespace) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(NAMESPACE_PROPERTY_NAME, namespace);

        return identifiers;
    }

    private Map<String, String> createAuditLogGetUploadFromUrlJobConfigurationIdentifier(String namespace, String jobId) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(NAMESPACE_PROPERTY_NAME, namespace);
        identifiers.put(JOB_ID_PROPERTY_NAME, jobId);

        return identifiers;
    }

    private Map<String, String> createAuditLogStartUploadFromUrlConfigurationIdentifier(String fileUrl) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(FILE_URL_PROPERTY_NAME, fileUrl);

        return identifiers;
    }
}
