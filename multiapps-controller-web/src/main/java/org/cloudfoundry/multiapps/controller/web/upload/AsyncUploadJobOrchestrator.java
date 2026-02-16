package org.cloudfoundry.multiapps.controller.web.upload;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAsyncUploadJobEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.stream.CountingInputStream;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.upload.client.DeployFromUrlRemoteClient;
import org.cloudfoundry.multiapps.controller.web.upload.client.FileFromUrlData;
import org.cloudfoundry.multiapps.controller.web.upload.exception.RejectedAsyncUploadJobException;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AsyncUploadJobOrchestrator {

    private static final int INPUT_STREAM_BUFFER_SIZE = 16 * 1024;

    private static final long WAIT_TIME_BETWEEN_ASYNC_JOB_UPDATES_IN_MILLIS = Duration.ofSeconds(3)
                                                                                      .toMillis();

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUploadJobOrchestrator.class);

    private final ResilientOperationExecutor resilientOperationExecutor = getResilientOperationExecutor();

    private final ExecutorService asyncFileUploadExecutor;
    private final ExecutorService deployFromUrlExecutor;
    private final ApplicationConfiguration applicationConfiguration;
    private final AsyncUploadJobService asyncUploadJobService;
    private final FileService fileService;
    private final DescriptorParserFacadeFactory descriptorParserFactory;
    private final DeployFromUrlRemoteClient deployFromUrlRemoteClient;

    @Inject
    public AsyncUploadJobOrchestrator(ExecutorService asyncFileUploadExecutor, ExecutorService deployFromUrlExecutor,
                                      ApplicationConfiguration applicationConfiguration, AsyncUploadJobService asyncUploadJobService,
                                      FileService fileService, DescriptorParserFacadeFactory descriptorParserFactory,
                                      DeployFromUrlRemoteClient deployFromUrlRemoteClient) {
        this.asyncFileUploadExecutor = asyncFileUploadExecutor;
        this.deployFromUrlExecutor = deployFromUrlExecutor;
        this.applicationConfiguration = applicationConfiguration;
        this.asyncUploadJobService = asyncUploadJobService;
        this.fileService = fileService;
        this.descriptorParserFactory = descriptorParserFactory;
        this.deployFromUrlRemoteClient = deployFromUrlRemoteClient;
    }

    public AsyncUploadJobEntry executeUploadFromUrl(String spaceGuid, String namespace, String urlWithoutUserInfo, String decodedUrl,
                                                    UserCredentials userCredentials) {
        var entry = createJobEntry(spaceGuid, namespace, urlWithoutUserInfo);
        LOGGER.info(Messages.CREATING_ASYNC_UPLOAD_JOB, urlWithoutUserInfo, entry.getId());
        asyncUploadJobService.add(entry);
        try {
            deployFromUrlExecutor.submit(() -> deployFromUrl(entry, decodedUrl, userCredentials));
        } catch (RejectedExecutionException ignored) {
            throw new RejectedAsyncUploadJobException(entry);
        }
        return entry;
    }

    private AsyncUploadJobEntry createJobEntry(String spaceGuid, String namespace, String url) {
        return ImmutableAsyncUploadJobEntry.builder()
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .user(SecurityContextUtil.getUsername())
                                           .addedAt(LocalDateTime.now())
                                           .spaceGuid(spaceGuid)
                                           .namespace(namespace)
                                           .instanceIndex(applicationConfiguration.getApplicationInstanceIndex())
                                           .url(url)
                                           .state(AsyncUploadJobEntry.State.INITIAL)
                                           .updatedAt(LocalDateTime.now())
                                           .bytesRead(0L)
                                           .build();
    }

    private void deployFromUrl(AsyncUploadJobEntry jobEntry, String fileUrl, UserCredentials userCredentials) {
        LOGGER.info(Messages.STARTING_DOWNLOAD_OF_MTAR_WITH_JOB_ID, jobEntry.getUrl(), jobEntry.getId());
        var startTime = LocalDateTime.now();
        Lock lock = new ReentrantLock();
        AtomicLong counterRef = new AtomicLong();
        try {
            var updatedJobEntry = asyncUploadJobService.update(jobEntry, ImmutableAsyncUploadJobEntry.copyOf(jobEntry)
                                                                                                     .withState(
                                                                                                         AsyncUploadJobEntry.State.RUNNING)
                                                                                                     .withUpdatedAt(LocalDateTime.now())
                                                                                                     .withStartedAt(startTime));
            startAsyncUploadFromUrlUpload(ImmutableUploadFromUrlContext.builder()
                                                                       .jobEntry(updatedJobEntry)
                                                                       .fileUrl(fileUrl)
                                                                       .userCredentials(userCredentials)
                                                                       .counterRef(counterRef)
                                                                       .startTime(startTime)
                                                                       .build(), lock);
            updatedJobEntry = asyncUploadJobService.createQuery()
                                                   .id(updatedJobEntry.getId())
                                                   .singleResult();
            monitorAsyncUploadJob(updatedJobEntry, lock, counterRef);
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ASYNC_UPLOAD_JOB_FAILED, jobEntry.getId(), e.getMessage()), e);
            updateFailedAsyncUploadJob(jobEntry, e, lock);
        }
    }

    private void startAsyncUploadFromUrlUpload(UploadFromUrlContext uploadFromUrlContext, Lock lock) {
        asyncFileUploadExecutor.submit(() -> {
            try {
                startSyncUploadFromUrlUpload(uploadFromUrlContext, lock);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                updateFailedAsyncUploadJob(uploadFromUrlContext.getJobEntry(), e, lock);
                throw new SLException(e, e.getMessage());
            }
        });
    }

    private void startSyncUploadFromUrlUpload(UploadFromUrlContext uploadFromUrlContext, Lock lock) throws Exception {
        FileEntry fileEntry = resilientOperationExecutor.execute(
            (CheckedSupplier<FileEntry>) () -> doUploadMtarFromUrl(uploadFromUrlContext, lock));
        LOGGER.trace(Messages.UPLOADED_MTAR_FROM_REMOTE_ENDPOINT_AND_JOB_ID, uploadFromUrlContext.getJobEntry()
                                                                                                 .getUrl(),
                     uploadFromUrlContext.getJobEntry()
                                         .getId(), ChronoUnit.MILLIS.between(uploadFromUrlContext.getStartTime(), LocalDateTime.now()));
        var descriptor = fileService.processFileContent(uploadFromUrlContext.getJobEntry()
                                                                            .getSpaceGuid(), fileEntry.getId(),
                                                        this::extractDeploymentDescriptor);
        LOGGER.debug(Messages.ASYNC_UPLOAD_JOB_FINISHED, uploadFromUrlContext.getJobEntry()
                                                                             .getId());
        try {
            lock.lock();
            var uploadedEntry = asyncUploadJobService.createQuery()
                                                     .id(uploadFromUrlContext.getJobEntry()
                                                                             .getId())
                                                     .singleResult();
            asyncUploadJobService.update(uploadedEntry, ImmutableAsyncUploadJobEntry.copyOf(uploadedEntry)
                                                                                    .withFileId(fileEntry.getId())
                                                                                    .withMtaId(descriptor.getId())
                                                                                    .withSchemaVersion(descriptor.getSchemaVersion())
                                                                                    .withUpdatedAt(LocalDateTime.now())
                                                                                    .withFinishedAt(LocalDateTime.now())
                                                                                    .withBytesRead(uploadFromUrlContext.getCounterRef()
                                                                                                                       .get())
                                                                                    .withState(AsyncUploadJobEntry.State.FINISHED));
        } finally {
            lock.unlock();
        }
    }

    private FileEntry doUploadMtarFromUrl(UploadFromUrlContext uploadFromUrlContext, Lock lock) throws Exception {
        FileFromUrlData fileFromUrlData = deployFromUrlRemoteClient.downloadFileFromUrl(uploadFromUrlContext);
        String fileName = extractFileName(uploadFromUrlContext.getFileUrl());
        FileUtils.validateFileHasExtension(fileName);
        resetCounterOnRetry(uploadFromUrlContext, lock);
        // Normal stream returned from the http response always returns 0 when InputStream::available() is executed which seems to break
        // JClods library: https://issues.apache.org/jira/browse/JCLOUDS-1623
        try (CountingInputStream source = new CountingInputStream(fileFromUrlData.fileInputStream(), uploadFromUrlContext.getCounterRef());
            BufferedInputStream bufferedContent = new BufferedInputStream(source, INPUT_STREAM_BUFFER_SIZE)) {
            LOGGER.debug(Messages.UPLOADING_MTAR_STREAM_FROM_REMOTE_ENDPOINT_WITH_JOB_ID, fileFromUrlData.uri(),
                         uploadFromUrlContext.getJobEntry()
                                             .getId());
            return fileService.addFile(ImmutableFileEntry.builder()
                                                         .space(uploadFromUrlContext.getJobEntry()
                                                                                    .getSpaceGuid())
                                                         .namespace(uploadFromUrlContext.getJobEntry()
                                                                                        .getNamespace())
                                                         .name(fileName)
                                                         .size(BigInteger.valueOf(fileFromUrlData.fileSize()))
                                                         .build(), bufferedContent);
        }
    }

    private void resetCounterOnRetry(UploadFromUrlContext upload, Lock lock) {
        try {
            lock.lock();
            upload.getCounterRef()
                  .set(0);
            AsyncUploadJobEntry asyncUploadJobEntry = asyncUploadJobService.createQuery()
                                                                           .id(upload.getJobEntry()
                                                                                     .getId())
                                                                           .singleResult();
            asyncUploadJobService.update(asyncUploadJobEntry, ImmutableAsyncUploadJobEntry.copyOf(asyncUploadJobEntry)
                                                                                          .withUpdatedAt(LocalDateTime.now())
                                                                                          .withBytesRead(0L));
        } finally {
            lock.unlock();
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
        String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, applicationConfiguration.getMaxMtaDescriptorSize());
        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        return descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
    }

    private void monitorAsyncUploadJob(AsyncUploadJobEntry updatedJobEntry, Lock lock, AtomicLong counterRef) {
        while (updatedJobEntry.getState() == AsyncUploadJobEntry.State.RUNNING) {
            try {
                lock.lock();
                updatedJobEntry = asyncUploadJobService.createQuery()
                                                       .id(updatedJobEntry.getId())
                                                       .singleResult();
                updatedJobEntry = asyncUploadJobService.update(updatedJobEntry, ImmutableAsyncUploadJobEntry.copyOf(updatedJobEntry)
                                                                                                            .withBytesRead(counterRef.get())
                                                                                                            .withUpdatedAt(
                                                                                                                LocalDateTime.now()));
            } finally {
                lock.unlock();
            }
            waitBetweenUpdates();
        }
    }

    protected void waitBetweenUpdates() {
        MiscUtil.sleep(WAIT_TIME_BETWEEN_ASYNC_JOB_UPDATES_IN_MILLIS);
    }

    private void updateFailedAsyncUploadJob(AsyncUploadJobEntry jobEntry, Exception e, Lock lock) {
        try {
            lock.lock();
            var failedEntry = asyncUploadJobService.createQuery()
                                                   .id(jobEntry.getId())
                                                   .singleResult();
            asyncUploadJobService.update(failedEntry, ImmutableAsyncUploadJobEntry.copyOf(failedEntry)
                                                                                  .withUpdatedAt(LocalDateTime.now())
                                                                                  .withFinishedAt(LocalDateTime.now())
                                                                                  .withError(e.getMessage())
                                                                                  .withState(AsyncUploadJobEntry.State.ERROR));
        } finally {
            lock.unlock();
        }
    }

    protected ResilientOperationExecutor getResilientOperationExecutor() {
        return new ResilientOperationExecutor();
    }

}
