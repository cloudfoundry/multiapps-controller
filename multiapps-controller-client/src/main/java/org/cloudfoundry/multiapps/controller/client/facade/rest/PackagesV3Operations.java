package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

/**
 * CF v3 <em>packages</em> operations of the cf-java-client replacement. Reproduces the HTTP shape and domain mapping of the OSS
 * {@code CloudControllerRestClientImpl} package methods:
 * <ul>
 * <li>{@code getPackage(guid)} &rarr; {@code GET /v3/packages/{guid}};</li>
 * <li>{@code getPackagesForApplication(appGuid)} &rarr; {@code GET /v3/apps/{guid}/packages} (paginated);</li>
 * <li>{@code createDockerPackage(appGuid, dockerInfo)} &rarr; {@code POST /v3/packages} then re-fetch;</li>
 * <li>{@code asyncUploadApplication(...)} &rarr; create a bits package ({@code POST /v3/packages}),
 * upload the bits multipart ({@code POST /v3/packages/{guid}/upload}), then poll status in a background thread;</li>
 * <li>{@code getUploadStatus(guid)} &rarr; derived from {@code getPackage(guid)}.</li>
 * </ul>
 */
public class PackagesV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;
    // Mirrors the OSS CloudControllerRestClientImpl.PACKAGE_UPLOAD_JOB_POLLING_PERIOD.
    private static final long PACKAGE_UPLOAD_JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);

    private static final ParameterizedTypeReference<V3ListResponse<V3Package>> PACKAGE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public PackagesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudPackage getPackage(UUID packageGuid) {
        V3Package resource = cc.get("/v3/packages/" + packageGuid, V3Package.class);
        return resource == null ? null : V3PackageMapper.toCloudPackage(resource);
    }

    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return cc.list("/v3/apps/" + applicationGuid + "/packages?per_page=" + DEFAULT_PAGE_SIZE, PACKAGE_PAGE)
                 .stream()
                 .map(V3PackageMapper::toCloudPackage)
                 .toList();
    }

    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("image", dockerInfo.getImage());
        DockerCredentials credentials = dockerInfo.getCredentials();
        if (credentials != null) {
            if (credentials.getUsername() != null) {
                data.put("username", credentials.getUsername());
            }
            if (credentials.getPassword() != null) {
                data.put("password", credentials.getPassword());
            }
        }
        V3Package created = cc.getRestClient()
                              .post()
                              .uri("/v3/packages")
                              .body(java.util.Map.of("type", "docker", "data", data, "relationships",
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
        cc.getRestClient()
          .post()
          .uri("/v3/packages/{guid}/upload", packageGuid)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .toBodilessEntity();

        return getPackage(packageGuid);
    }

    private CloudPackage createBitsPackage(UUID applicationGuid) {
        V3Package created = cc.getRestClient()
                              .post()
                              .uri("/v3/packages")
                              .body(java.util.Map.of("type", "bits", "relationships", applicationRelationship(applicationGuid)))
                              .retrieve()
                              .body(V3Package.class);
        return getPackage(UUID.fromString(created.guid()));
    }

    private static java.util.Map<String, Object> applicationRelationship(UUID applicationGuid) {
        return java.util.Map.of("app", java.util.Map.of("data", java.util.Map.of("guid", applicationGuid.toString())));
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(applicationName);
        List<V3App> apps = cc.list(query.toString(), new ParameterizedTypeReference<V3ListResponse<V3App>>() {
        });
        if (apps.isEmpty() || apps.get(0)
                                  .guid() == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

    private void processAsyncUploadInBackground(CloudPackage cloudPackage, UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", cloudPackage.getGuid());
        new Thread(() -> processAsyncUpload(cloudPackage, callback), threadName).start();
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
                Thread.sleep(PACKAGE_UPLOAD_JOB_POLLING_PERIOD);
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
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

    /**
     * Minimal wire-model of a v3 app resource, used only to resolve an application's GUID by name when starting an upload. The
     * fully-featured app wire model lives in {@code V3Application}; this local record keeps the packages helper self-contained.
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record V3App(@com.fasterxml.jackson.annotation.JsonProperty("guid") String guid) {
    }

}
