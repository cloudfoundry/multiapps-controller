package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.FilesApiService;
import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.FileUrl;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableAsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileMetadata;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry.State;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.util.Configuration;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
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
    private static final int ERROR_RESPONSE_BODY_MAX_LENGTH = 4 * 1024;
    private static final int INPUT_STREAM_BUFFER_SIZE = 16 * 1024;
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofMinutes(10);
    private static final String RETRY_AFTER_SECONDS = "30";

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
    @Named("asyncFileUploadExecutor")
    private ExecutorService deployFromUrlExecutor;

    private final CachedMap<String, AtomicLong> jobCounters = new CachedMap<>(Duration.ofHours(1));

    private final CachedMap<String, Future<?>> runningTasks = new CachedMap<>(Duration.ofHours(1));

    private final ResilientOperationExecutor resilientOperationExecutor = getResilientOperationExecutor();

    @Override
    public ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace) {
        try {
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
            FileEntry fileEntry = fileService.addFile(spaceGuid, namespace, multipartFile.getOriginalFilename(), in,
                                                      multipartFile.getSize());
            FileMetadata file = parseFileEntry(fileEntry);
            AuditLoggingProvider.getFacade()
                                .logConfigCreate(file);
            var endTime = LocalDateTime.now();
            LOGGER.trace(Messages.UPLOADED_FILE, file.getId(), file.getName(), file.getSize(), file.getDigest(), file.getDigestAlgorithm(),
                         ChronoUnit.MILLIS.between(startTime, endTime));
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
        var existingJob = getExistingJob(spaceGuid, namespace, urlWithoutUserInfo);
        if (existingJob != null) {
            if (runningTasks.get(existingJob.getId()) != null) {
                LOGGER.info(Messages.ASYNC_UPLOAD_JOB_EXISTS, urlWithoutUserInfo, existingJob);
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                                     .header(HttpHeaders.LOCATION, getLocationHeader(spaceGuid, existingJob.getId()))
                                     .header("x-cf-app-instance", configuration.getApplicationGuid() + ":" + existingJob.getInstanceIndex())
                                     .build();
            } else {
                LOGGER.warn(Messages.THE_JOB_EXISTS_BUT_IT_IS_NOT_RUNNING_DELETING);
                deleteAsyncJobEntry(existingJob);
            }
        }
        return triggerUploadFromUrl(spaceGuid, namespace, urlWithoutUserInfo, decodedUrl);
    }

    private String getLocationHeader(String spaceGuid, String jobId) {
        return "spaces/" + spaceGuid + "/files/jobs/" + jobId;
    }

    @Override
    public ResponseEntity<AsyncUploadResult> getUploadFromUrlJob(String spaceGuid, String namespace, String jobId) {
        AsyncUploadJobEntry job = getJob(jobId, spaceGuid, namespace);
        if (job == null) {
            return ResponseEntity.notFound()
                                 .build();
        }
        return getAsyncUploadResult(spaceGuid, namespace, job);
    }

    private ResponseEntity<AsyncUploadResult> getAsyncUploadResult(String spaceGuid, String namespace, AsyncUploadJobEntry job) {
        if (job.getState() == State.RUNNING || job.getState() == State.INITIAL) {
            Future<?> futureTask = runningTasks.get(job.getId());
            if (futureTask == null) {
                LOGGER.error(MessageFormat.format(Messages.JOB_0_WAS_NOT_FOUND_IN_THE_RUNNING_TASKS, job.getId()));
                return ResponseEntity.ok(createErrorResult(Messages.JOB_IS_NOT_BEING_EXECUTED,
                                                           AsyncUploadResult.ClientAction.RETRY_UPLOAD));
            }
            if (!futureTask.isDone()) {
                var count = jobCounters.getOrDefault(job.getId(), new AtomicLong(-1));
                return ResponseEntity.ok(ImmutableAsyncUploadResult.builder()
                                                                   .status(AsyncUploadResult.JobStatus.RUNNING)
                                                                   .bytes(count.get())
                                                                   .build());
            }
            var jobWithLatestState = getJob(job.getId(), spaceGuid, namespace);
            if (jobWithLatestState.getState() == State.RUNNING || jobWithLatestState.getState() == State.INITIAL) {
                LOGGER.error(MessageFormat.format(Messages.JOB_0_EXISTS_IN_STATE_1_BUT_DOES_NOT_EXISTS_IN_THE_RUNNING_TASKS, job.getId(),
                                                  jobWithLatestState.getState()));
                return ResponseEntity.ok(createErrorResult(Messages.JOB_THREAD_IS_NOT_RUNNING_BUT_STATE_IS_STILL_IN_PROGRESS_UPLOAD_FAILED,
                                                           AsyncUploadResult.ClientAction.RETRY_UPLOAD));
            }
        }
        if (job.getState() == State.ERROR) {
            jobCounters.remove(job.getId());
            runningTasks.remove(job.getId());
            return ResponseEntity.ok(createErrorResult(job.getError()));
        }
        return addFileEntryToAsyncUploadResult(spaceGuid, job);
    }

    private ResponseEntity<AsyncUploadResult> addFileEntryToAsyncUploadResult(String spaceGuid, AsyncUploadJobEntry job) {
        FileEntry fileEntry;
        try {
            fileEntry = fileService.getFile(spaceGuid, job.getFileId());
        } catch (FileStorageException e) {
            LOGGER.error(MessageFormat.format(Messages.FETCHING_FILE_FAILED, job.getFileId(), spaceGuid, e.getMessage()), e);
            return ResponseEntity.ok(createErrorResult(e.getMessage()));
        }
        FileMetadata file = parseFileEntry(fileEntry);
        AuditLoggingProvider.getFacade()
                            .logConfigCreate(file);
        jobCounters.remove(job.getId());
        runningTasks.remove(job.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ImmutableAsyncUploadResult.builder()
                                                             .status(AsyncUploadResult.JobStatus.FINISHED)
                                                             .file(file)
                                                             .mtaId(job.getMtaId())
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

    protected ResilientOperationExecutor getResilientOperationExecutor() {
        return new ResilientOperationExecutor();
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
                                   .instanceIndex(configuration.getApplicationInstanceIndex())
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

    private ResponseEntity<Void> triggerUploadFromUrl(String spaceGuid, String namespace, String urlWithoutUserInfo, String decodedUrl) {
        var entry = createJobEntry(spaceGuid, namespace, urlWithoutUserInfo);
        LOGGER.debug(Messages.CREATING_ASYNC_UPLOAD_JOB, urlWithoutUserInfo, entry.getId());
        uploadJobService.add(entry);
        try {
            Future<?> runningTask = deployFromUrlExecutor.submit(() -> uploadFileFromUrl(entry, spaceGuid, namespace, decodedUrl));
            runningTasks.put(entry.getId(), runningTask);
        } catch (RejectedExecutionException ignored) {
            LOGGER.debug(Messages.ASYNC_UPLOAD_JOB_REJECTED, entry.getId());
            deleteAsyncJobEntry(entry);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                 .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                                 .build();
        }
        return ResponseEntity.accepted()
                             .header("x-cf-app-instance",
                                     configuration.getApplicationGuid() + ":" + configuration.getApplicationInstanceIndex())
                             .header(HttpHeaders.LOCATION, getLocationHeader(spaceGuid, entry.getId()))
                             .build();
    }

    private AsyncUploadJobEntry createJobEntry(String spaceGuid, String namespace, String url) {
        return ImmutableAsyncUploadJobEntry.builder()
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .user(SecurityContextUtil.getUsername())
                                           .addedAt(LocalDateTime.now())
                                           .spaceGuid(spaceGuid)
                                           .namespace(namespace)
                                           .instanceIndex(configuration.getApplicationInstanceIndex())
                                           .url(url)
                                           .state(State.INITIAL)
                                           .build();
    }

    private AsyncUploadJobEntry getJob(String id, String spaceGuid, String namespace) {
        var jobs = uploadJobService.createQuery()
                                   .id(id)
                                   // even though the ID fully qualifies the job, we add these filters
                                   // to prevent accessing a job from another space, namespace or a different user
                                   .spaceGuid(spaceGuid)
                                   .user(SecurityContextUtil.getUsername())
                                   .namespace(namespace)
                                   .instanceIndex(configuration.getApplicationInstanceIndex())
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

    private void uploadFileFromUrl(AsyncUploadJobEntry jobEntry, String spaceGuid, String namespace, String fileUrl) {
        var counter = new AtomicLong(0);
        jobCounters.put(jobEntry.getId(), counter);
        LOGGER.info(Messages.STARTING_DOWNLOAD_OF_MTAR, jobEntry.getUrl());
        var startTime = LocalDateTime.now();
        AsyncUploadJobEntry jobEntryWithStartTime = ImmutableAsyncUploadJobEntry.copyOf(jobEntry)
                                                                                .withState(State.RUNNING)
                                                                                .withStartedAt(startTime);
        try {
            jobEntryWithStartTime = uploadJobService.update(jobEntry, jobEntryWithStartTime);
            FileEntry fileEntry = resilientOperationExecutor.execute((CheckedSupplier<FileEntry>) () -> doUploadFileFromUrl(spaceGuid,
                                                                                                                            namespace,
                                                                                                                            fileUrl,
                                                                                                                            counter));
            LOGGER.trace(Messages.UPLOADED_MTAR_FROM_REMOTE_ENDPOINT_AND_JOB_ID, jobEntry.getUrl(), jobEntry.getId(),
                         ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()));
            var descriptor = fileService.processFileContent(spaceGuid, fileEntry.getId(), this::extractDeploymentDescriptor);
            LOGGER.debug(Messages.ASYNC_UPLOAD_JOB_FINISHED, jobEntry.getId());
            uploadJobService.update(jobEntryWithStartTime, ImmutableAsyncUploadJobEntry.copyOf(jobEntryWithStartTime)
                                                                                       .withFileId(fileEntry.getId())
                                                                                       .withMtaId(descriptor.getId())
                                                                                       .withFinishedAt(LocalDateTime.now())
                                                                                       .withState(State.FINISHED));
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ASYNC_UPLOAD_JOB_FAILED, jobEntry.getId(), e.getMessage()), e);
            uploadJobService.update(jobEntryWithStartTime, ImmutableAsyncUploadJobEntry.copyOf(jobEntryWithStartTime)
                                                                                       .withError(e.getMessage())
                                                                                       .withState(State.ERROR));
        }
    }

    private FileEntry doUploadFileFromUrl(String spaceGuid, String namespace, String fileUrl, AtomicLong counter) throws Exception {
        if (!UriUtil.isUrlSecure(fileUrl)) {
            throw new SLException(Messages.MTAR_ENDPOINT_NOT_SECURE);
        }
        UriUtil.validateUrl(fileUrl);
        HttpClient client = buildHttpClient(fileUrl);

        HttpResponse<InputStream> response = callRemoteEndpointWithRetry(client, fileUrl);
        long fileSize = response.headers()
                                .firstValueAsLong(Constants.CONTENT_LENGTH)
                                .orElseThrow(() -> new SLException(Messages.FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH));

        long maxUploadSize = new Configuration().getMaxUploadSize();
        if (fileSize > maxUploadSize) {
            throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED, maxUploadSize));
        }

        String fileName = extractFileName(fileUrl);
        FileUtils.validateFileHasExtension(fileName);
        resetCounterOnRetry(counter);
        // Normal stream returned from the http response always returns 0 when InputStream::available() is executed which seems to break
        // JClods library: https://issues.apache.org/jira/browse/JCLOUDS-1623
        try (CountingInputStream source = new CountingInputStream(response.body(), counter);
            BufferedInputStream bufferedContent = new BufferedInputStream(source, INPUT_STREAM_BUFFER_SIZE)) {
            LOGGER.debug(Messages.UPLOADING_MTAR_STREAM_FROM_REMOTE_ENDPOINT, response.uri());
            return fileService.addFile(spaceGuid, namespace, fileName, bufferedContent, fileSize);
        }
    }

    private HttpResponse<InputStream> callRemoteEndpointWithRetry(HttpClient client, String decodedUrl) throws Exception {
        return resilientOperationExecutor.execute((CheckedSupplier<HttpResponse<InputStream>>) () -> {
            var request = buildFetchFileRequest(decodedUrl);
            LOGGER.debug(Messages.CALLING_REMOTE_MTAR_ENDPOINT, request.uri());
            var response = client.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                String error = readErrorBodyFromResponse(response);
                throw new SLException(MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT, request.uri(), response.statusCode(),
                                                           error));
            }
            return response;
        });
    }

    private void resetCounterOnRetry(AtomicLong counter) {
        counter.set(0);
    }

    protected HttpClient buildHttpClient(String decodedUrl) {
        return HttpClient.newBuilder()
                         .version(HttpClient.Version.HTTP_2)
                         .connectTimeout(HTTP_CONNECT_TIMEOUT)
                         .followRedirects(Redirect.NORMAL)
                         .authenticator(buildPasswordAuthenticator(decodedUrl))
                         .build();
    }

    private Authenticator buildPasswordAuthenticator(String decodedUrl) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                var uri = URI.create(decodedUrl);
                var userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    var separatorIndex = userInfo.indexOf(':');
                    var username = userInfo.substring(0, separatorIndex);
                    var password = userInfo.substring(separatorIndex + 1);
                    return new PasswordAuthentication(username, password.toCharArray());
                }
                return super.getPasswordAuthentication();
            }
        };
    }

    private HttpRequest buildFetchFileRequest(String decodedUrl) {
        var builder = HttpRequest.newBuilder()
                                 .GET()
                                 .timeout(Duration.ofMinutes(15));
        var uri = URI.create(decodedUrl);
        var userInfo = uri.getUserInfo();
        if (userInfo != null) {
            builder.uri(URI.create(decodedUrl.replace(userInfo + "@", "")));
        } else {
            builder.uri(uri);
        }
        return builder.build();
    }

    private String readErrorBodyFromResponse(HttpResponse<InputStream> response) throws IOException {
        try (InputStream is = response.body()) {
            byte[] buffer = new byte[ERROR_RESPONSE_BODY_MAX_LENGTH];
            int read = IOUtils.read(is, buffer);
            return new String(Arrays.copyOf(buffer, read));
        }
    }

    private String extractFileName(String url) {
        String path = URI.create(url)
                         .getPath();
        if (path.indexOf('/') == -1) {
            return path;
        }
        String[] pathFragments = path.split("/");
        return pathFragments[pathFragments.length - 1];
    }

    private DeploymentDescriptor extractDeploymentDescriptor(InputStream appArchiveStream) {
        String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        return descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
    }

    private static class CountingInputStream extends ProxyInputStream {
        private final AtomicLong bytes;

        public CountingInputStream(InputStream proxy, AtomicLong counterRef) {
            super(proxy);
            bytes = counterRef;
        }

        @Override
        protected void afterRead(int n) {
            bytes.addAndGet(n);
        }
    }

}
