package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;
import org.cloudfoundry.multiapps.controller.client.facade.domain.BitsData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ErrorDetails;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableErrorDetails;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableUpload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Package;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3PackageMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class PackagesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Package>> PACKAGE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    private final Function<Duration, RestClient> uploadRestClientFactory;

    private final ExecutorService uploadMonitorExecutor = createUploadMonitorExecutor();

    public PackagesV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this(client, targetSpace, null);
    }

    public PackagesV3Operations(CloudControllerV3Client client, CloudSpace targetSpace,
                                Function<Duration, RestClient> uploadRestClientFactory) {
        this.client = client;
        this.targetSpace = targetSpace;
        this.uploadRestClientFactory = uploadRestClientFactory;
    }

    public CloudPackage getPackage(UUID packageGuid) {
        V3Package packageResource = client.get(CloudControllerV3Endpoints.PACKAGES + "/" + packageGuid, V3Package.class);

        if (packageResource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.PACKAGE_0_NOT_FOUND, packageGuid));
        }

        return V3PackageMapper.toCloudPackage(packageResource);
    }

    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return client.list(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/packages" + CloudControllerV3Endpoints.QUERY_PER_PAGE
                               + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, PACKAGE_PAGE)
                     .stream()
                     .map(V3PackageMapper::toCloudPackage)
                     .toList();
    }

    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("image", dockerInfo.getImage());

        DockerCredentials credentials = dockerInfo.getCredentials();
        if (credentials != null && credentials.getUsername() != null && credentials.getPassword() != null) {
            data.put("username", credentials.getUsername());
            data.put("password", credentials.getPassword());
        }

        V3Package created = client.getRestClient()
                                  .post()
                                  .uri(CloudControllerV3Endpoints.PACKAGES)
                                  .body(Map.of("type", "docker", "data", data, "relationships",
                                               applicationRelationship(applicationGuid)))
                                  .retrieve()
                                  .body(V3Package.class);

        return getPackage(UUID.fromString(created.guid()));
    }

    public Upload getUploadStatus(UUID packageGuid) {
        CloudPackage cloudPackage = getPackage(packageGuid);
        ErrorDetails errorDetails = null;

        if (cloudPackage.getType() == CloudPackage.Type.BITS) {
            errorDetails = ImmutableErrorDetails.builder()
                                                .description(((BitsData) cloudPackage.getData()).getError())
                                                .build();
        }

        return ImmutableUpload.builder()
                              .status(cloudPackage.getStatus())
                              .errorDetails(errorDetails)
                              .build();
    }

    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback, Duration uploadTimeout) {
        CloudPackage cloudPackage = startUpload(applicationName, file, uploadTimeout);
        processAsyncUploadInBackground(cloudPackage, callback);
        return cloudPackage;
    }

    private CloudPackage startUpload(String applicationName, Path file, Duration uploadTimeout) {
        Assert.notNull(applicationName, "AppName must not be null");
        Assert.notNull(file, "File must not be null");

        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID packageGuid = createBitsPackage(applicationGuid).getGuid();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("bits", new FileSystemResource(file));

        RestClient uploadClient = resolveUploadClient(uploadTimeout);
        uploadClient.post()
                    .uri(CloudControllerV3Endpoints.PACKAGE_UPLOAD, packageGuid)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        return getPackage(packageGuid);
    }

    private RestClient resolveUploadClient(Duration uploadTimeout) {
        if (uploadRestClientFactory != null && uploadTimeout != null && !uploadTimeout.isZero() && !uploadTimeout.isNegative()) {
            return uploadRestClientFactory.apply(uploadTimeout);
        }

        return client.getRestClient();
    }

    private CloudPackage createBitsPackage(UUID applicationGuid) {
        V3Package created = client.getRestClient()
                                  .post()
                                  .uri(CloudControllerV3Endpoints.PACKAGES)
                                  .body(Map.of("type", "bits", "relationships", applicationRelationship(applicationGuid)))
                                  .retrieve()
                                  .body(V3Package.class);

        return getPackage(UUID.fromString(created.guid()));
    }

    private static Map<String, Object> applicationRelationship(UUID applicationGuid) {
        return Map.of("app", Map.of("data", Map.of("guid", applicationGuid.toString())));
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        List<V3App> apps = client.list(buildApplicationQuery(applicationName), new ParameterizedTypeReference<V3ListResponse<V3App>>() {
        });

        if (apps.isEmpty() || apps.getFirst()
                                  .guid() == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

    private String buildApplicationQuery(String applicationName) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (targetSpace != null && targetSpace.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(targetSpace.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(applicationName);

        return query.toString();
    }

    private void processAsyncUploadInBackground(CloudPackage cloudPackage, UploadStatusCallback callback) {
        uploadMonitorExecutor.execute(() -> processAsyncUpload(cloudPackage, callback));
    }

    private void processAsyncUpload(CloudPackage cloudPackage, UploadStatusCallback callback) {
        while (true) {
            Upload upload = getUploadStatus(cloudPackage.getGuid());
            Status uploadStatus = upload.getStatus();
            boolean unsubscribe = callback.onProgress(uploadStatus.toString());

            if (unsubscribe || isUploadReady(uploadStatus)) {
                return;
            }

            if (hasUploadFailed(uploadStatus)) {
                callback.onError(upload.getErrorDetails()
                                       .getDescription());
                return;
            }

            try {
                MiscUtil.sleep(Constants.PACKAGE_UPLOAD_JOB_POLLING_PERIOD);
            } catch (IllegalStateException e) {
                return;
            }
        }
    }

    private boolean isUploadReady(Status status) {
        return status == Status.READY;
    }

    private boolean hasUploadFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    private ExecutorService createUploadMonitorExecutor() {
        ThreadFactory threadFactory = Thread.ofVirtual()
                                            .name(Constants.UPLOAD_MONITOR_THREAD_NAME_PREFIX, 0)
                                            .factory();

        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V3App(@JsonProperty("guid") String guid) {
    }

}
