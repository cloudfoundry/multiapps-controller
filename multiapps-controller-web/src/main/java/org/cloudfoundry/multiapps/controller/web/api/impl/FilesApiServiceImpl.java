package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.FilesApiService;
import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.FileUrl;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableAsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.core.auditlogging.FilesApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry.State;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.util.PriorityCallable;
import org.cloudfoundry.multiapps.controller.process.util.PriorityFuture;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.upload.AsyncUploadJobOrchestrator;
import org.cloudfoundry.multiapps.controller.web.upload.exception.RejectedAsyncUploadJobException;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Named
public class FilesApiServiceImpl implements FilesApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesApiServiceImpl.class);
    private static final String RETRY_AFTER_SECONDS = "30";
    private static final int INPUT_STREAM_BUFFER_SIZE = 16 * 1024;
    private static final int UPDATE_JOB_TIMEOUT = 30;

    static {
        System.setProperty(Constants.RETRY_LIMIT_PROPERTY, "0");
    }

    @Inject
    @Named("fileService")
    private FileService fileService;
    @Inject
    private DescriptorParserFacadeFactory descriptorParserFactory;
    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private AsyncUploadJobService uploadJobService;
    @Inject
    private FilesApiServiceAuditLog filesApiServiceAuditLog;
    @Inject
    private AsyncUploadJobOrchestrator asyncUploadJobOrchestrator;
    @Inject
    private ExecutorService fileStorageThreadPool;

    @Override
    public ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace) {
        try {
            filesApiServiceAuditLog.logGetFiles(SecurityContextUtil.getUsername(), spaceGuid, namespace);
            List<FileEntry> entries = fileService.listFiles(spaceGuid, namespace);
            List<FileMetadata> files = entries.stream()
                                              .map(this::parseFileEntry)
                                              .collect(Collectors.toList());
            return ResponseEntity.ok()
                                 .body(files);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_GET_FILES_0, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<FileMetadata> uploadFile(MultipartHttpServletRequest request, String spaceGuid, String namespace) {
        LOGGER.trace(Messages.RECEIVED_UPLOAD_REQUEST, ServletUtil.decodeUri(request));
        var multipartFile = getFileFromRequest(request);
        try (InputStream in = new BufferedInputStream(multipartFile.getInputStream(), INPUT_STREAM_BUFFER_SIZE)) {
            var startTime = LocalDateTime.now();
            FileEntry fileEntry = fileStorageThreadPool.submit(createUploadFileTask(spaceGuid, namespace, multipartFile, in))
                                                       .get();
            FileMetadata file = parseFileEntry(fileEntry);
            filesApiServiceAuditLog.logUploadFile(SecurityContextUtil.getUsername(), spaceGuid, file);
            var endTime = LocalDateTime.now();
            LOGGER.trace(Messages.UPLOADED_FILE, file.getId(), file.getName(), file.getSize(), file.getSpace(), file.getDigest(),
                         file.getDigestAlgorithm(), ChronoUnit.MILLIS.between(startTime, endTime));
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(file);
        } catch (Exception e) {
            throw new SLException(e, Messages.COULD_NOT_UPLOAD_FILE_0, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> startUploadFromUrl(String spaceGuid, String namespace, FileUrl fileUrl) {
        String decodedUrl = new String(Base64.getUrlDecoder()
                                             .decode(fileUrl.getFileUrl()));
        String urlWithoutUserInfo = UriUtil.stripUserInfo(decodedUrl);
        LOGGER.trace(Messages.RECEIVED_UPLOAD_FROM_URL_REQUEST, urlWithoutUserInfo);
        filesApiServiceAuditLog.logStartUploadFromUrl(SecurityContextUtil.getUsername(), spaceGuid, decodedUrl);
        var existingJob = getExistingJob(spaceGuid, namespace, urlWithoutUserInfo);
        if (existingJob == null) {
            return triggerUploadFromUrl(spaceGuid, namespace, urlWithoutUserInfo, decodedUrl, fileUrl.getUserCredentials());
        }
        if (hasJobStuck(existingJob)) {
            deleteAsyncJobEntry(existingJob);
            return triggerUploadFromUrl(spaceGuid, namespace, urlWithoutUserInfo, decodedUrl, fileUrl.getUserCredentials());
        }
        LOGGER.info(Messages.ASYNC_UPLOAD_JOB_EXISTS, urlWithoutUserInfo, existingJob);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                             .header(HttpHeaders.LOCATION, getLocationHeader(spaceGuid, existingJob.getId()))
                             .build();
    }

    private boolean hasJobStuck(AsyncUploadJobEntry existingJob) {
        return existingJob.getUpdatedAt()
                          .isBefore(LocalDateTime.now()
                                                 .minusSeconds(UPDATE_JOB_TIMEOUT));
    }

    private String getLocationHeader(String spaceGuid, String jobId) {
        return "spaces/" + spaceGuid + "/files/jobs/" + jobId;
    }

    @Override
    public ResponseEntity<AsyncUploadResult> getUploadFromUrlJob(String spaceGuid, String namespace, String jobId) {
        filesApiServiceAuditLog.logGetUploadFromUrlJob(SecurityContextUtil.getUsername(), spaceGuid, namespace, jobId);
        AsyncUploadJobEntry job = getJob(jobId, spaceGuid, namespace);
        if (job == null) {
            return ResponseEntity.notFound()
                                 .build();
        }
        return getAsyncUploadResult(job);
    }

    private ResponseEntity<AsyncUploadResult> getAsyncUploadResult(AsyncUploadJobEntry job) {
        if (job.getState() == State.RUNNING || job.getState() == State.INITIAL) {
            if (hasJobStuck(job)) {
                LOGGER.info(Messages.JOB_WITH_ID_WAS_NOT_UPDATED_WITHIN_SECONDS, job.getId(), UPDATE_JOB_TIMEOUT);
                return ResponseEntity.ok(
                    createErrorResult(MessageFormat.format(Messages.JOB_NOT_UPDATED_FOR_0_SECONDS, UPDATE_JOB_TIMEOUT),
                                      AsyncUploadResult.ClientAction.RETRY_UPLOAD));
            }
            return ResponseEntity.ok(ImmutableAsyncUploadResult.builder()
                                                               .status(AsyncUploadResult.JobStatus.RUNNING)
                                                               .bytes(job.getBytesRead())
                                                               .build());
        }
        if (job.getState() == State.ERROR) {
            return ResponseEntity.ok(createErrorResult(job.getError()));
        }
        return addFileEntryToAsyncUploadResult(job);
    }

    private ResponseEntity<AsyncUploadResult> addFileEntryToAsyncUploadResult(AsyncUploadJobEntry job) {
        FileEntry fileEntry;
        try {
            fileEntry = fileService.getFile(job.getSpaceGuid(), job.getFileId());
        } catch (FileStorageException e) {
            LOGGER.error(MessageFormat.format(Messages.FETCHING_FILE_FAILED, job.getFileId(), job.getSpaceGuid(), e.getMessage()), e);
            return ResponseEntity.ok(createErrorResult(e.getMessage()));
        }
        FileMetadata file = parseFileEntry(fileEntry);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ImmutableAsyncUploadResult.builder()
                                                             .status(AsyncUploadResult.JobStatus.FINISHED)
                                                             .file(file)
                                                             .mtaId(job.getMtaId())
                                                             .schemaVersion(job.getSchemaVersion())
                                                             .build());
    }

    private MultipartFile getFileFromRequest(MultipartHttpServletRequest request) {
        var parts = request.getFileMap();
        var it = parts.values()
                      .iterator();
        if (!it.hasNext()) {
            throw new SLException(Messages.NO_FILES_TO_UPLOAD);
        }
        return it.next();
    }

    private PriorityCallable<FileEntry> createUploadFileTask(String spaceGuid, String namespace, MultipartFile multipartFile,
                                                             InputStream in) {
        return new PriorityCallable<>(PriorityFuture.Priority.LOWEST, () -> doUploadFile(spaceGuid, namespace, multipartFile, in));
    }

    private FileEntry doUploadFile(String spaceGuid, String namespace, MultipartFile multipartFile, InputStream in)
        throws FileStorageException {
        return fileService.addFile(ImmutableFileEntry.builder()
                                                     .space(spaceGuid)
                                                     .namespace(namespace)
                                                     .name(multipartFile.getOriginalFilename())
                                                     .size(BigInteger.valueOf(multipartFile.getSize()))
                                                     .build(),
                                   in);
    }

    private FileMetadata parseFileEntry(FileEntry fileEntry) {
        return ImmutableFileMetadata.builder()
                                    .id(fileEntry.getId())
                                    .digest(fileEntry.getDigest())
                                    .digestAlgorithm(fileEntry.getDigestAlgorithm())
                                    .name(fileEntry.getName())
                                    .size(fileEntry.getSize())
                                    .space(fileEntry.getSpace())
                                    .namespace(fileEntry.getNamespace())
                                    .build();
    }

    private AsyncUploadJobEntry getExistingJob(String spaceGuid, String namespace, String url) {
        var jobs = uploadJobService.createQuery()
                                   .spaceGuid(spaceGuid)
                                   .user(SecurityContextUtil.getUsername())
                                   .namespace(namespace)
                                   .url(url)
                                   .withoutFinishedAt()
                                   .withStateAnyOf(State.INITIAL, State.RUNNING)
                                   .list();
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    private void deleteAsyncJobEntry(AsyncUploadJobEntry entry) {
        try {
            uploadJobService.createQuery()
                            .id(entry.getId())
                            .delete();
        } catch (Exception e) {
            LOGGER.error(Messages.ERROR_OCCURRED_WHILE_DELETING_JOB_ENTRY, e);
        }
    }

    private ResponseEntity<Void> triggerUploadFromUrl(String spaceGuid, String namespace, String urlWithoutUserInfo, String decodedUrl,
                                                      UserCredentials userCredentials) {
        try {
            AsyncUploadJobEntry entry = asyncUploadJobOrchestrator.executeUploadFromUrl(spaceGuid, namespace, urlWithoutUserInfo,
                                                                                        decodedUrl, userCredentials);
            return ResponseEntity.accepted()
                                 .header(HttpHeaders.LOCATION, getLocationHeader(spaceGuid, entry.getId()))
                                 .build();
        } catch (RejectedAsyncUploadJobException rejectedJobException) {
            LOGGER.debug(Messages.ASYNC_UPLOAD_JOB_REJECTED, spaceGuid, namespace, urlWithoutUserInfo);
            if (rejectedJobException.getAsyncUploadJobEntry() != null) {
                deleteAsyncJobEntry(rejectedJobException.getAsyncUploadJobEntry());
            }
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                 .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                                 .build();
        }
    }

    private AsyncUploadJobEntry getJob(String id, String spaceGuid, String namespace) {
        var jobs = uploadJobService.createQuery()
                                   .id(id)
                                   // even though the ID fully qualifies the job, we add these filters
                                   // to prevent accessing a job from another space, namespace or a different user
                                   .spaceGuid(spaceGuid)
                                   .user(SecurityContextUtil.getUsername())
                                   .namespace(namespace)
                                   .list();
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    private AsyncUploadResult createErrorResult(String error, AsyncUploadResult.ClientAction... clientActions) {
        return ImmutableAsyncUploadResult.builder()
                                         .status(AsyncUploadResult.JobStatus.ERROR)
                                         .error(error)
                                         .clientActions(List.of(clientActions))
                                         .build();
    }

}
