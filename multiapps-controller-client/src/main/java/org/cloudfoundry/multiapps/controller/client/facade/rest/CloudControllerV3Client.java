package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Low-level Cloud Controller v3 access shared by every operation of {@link CloudControllerRestClientV3Impl}. This is the reusable
 * machinery of the cf-java-client replacement:
 * <ul>
 * <li>typed {@code GET} / list helpers over the configured {@link RestClient} (error mapping is already attached to the RestClient via
 * {@link CloudControllerResponseErrorHandler});</li>
 * <li>a {@link #list(String, ParameterizedTypeReference) pagination walker} that follows {@code pagination.next.href} across pages;</li>
 * <li>an {@link #waitForAsyncJob(String, Duration) async job poller} reproducing the OSS {@code JobV3Util} contract (exponential
 * backoff 1s&rarr;15s, capped at a timeout, until COMPLETE/FAILED).</li>
 * </ul>
 * It deals in the thin {@code resources.*} wire types; mapping those to the {@code domain.Cloud*} model happens in the per-resource
 * operations that call this.
 */
public class CloudControllerV3Client {


    // Mirrors the OSS JobV3Util backoff so async behaviour is unchanged.
    private static final Duration JOB_POLL_MIN_INTERVAL = Duration.ofSeconds(1);
    private static final Duration JOB_POLL_MAX_INTERVAL = Duration.ofSeconds(15);
    private static final Duration DEFAULT_JOB_TIMEOUT = Duration.ofMinutes(5);

    private final RestClient restClient;

    public CloudControllerV3Client(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> T get(String uri, Class<T> responseType) {
        return restClient.get()
                         .uri(uri)
                         .retrieve()
                         .body(responseType);
    }

    /**
     * GET a single resource, returning empty on a 404 rather than throwing — several callers rely on "not found" being a normal,
     * non-exceptional outcome (the OSS impl expressed this with {@code onErrorResume} on NOT_FOUND).
     */
    public <T> Optional<T> getOptional(String uri, Class<T> responseType) {
        try {
            return Optional.ofNullable(get(uri, responseType));
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Walk every page of a v3 list endpoint, following {@code pagination.next.href} until it is {@code null}. The caller supplies the
     * fully-formed first-page URI (path + query) and a type reference for the paged {@link V3ListResponse}.
     */
    public <R> List<R> list(String firstPageUri, ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        List<R> all = new ArrayList<>();
        String nextUri = firstPageUri;
        while (nextUri != null) {
            V3ListResponse<R> page = restClient.get()
                                               .uri(nextUri)
                                               .retrieve()
                                               .body(pageType);
            if (page == null) {
                break;
            }
            all.addAll(page.resources());
            nextUri = toRelativeIfAbsolute(page.nextPageHref());
        }
        return all;
    }

    /**
     * Follow the {@code Location} header of a 202-Accepted async response to its job, then poll to completion.
     */
    public void followAsyncJob(ResponseEntity<Void> acceptedResponse, Duration timeout) {
        URI location = acceptedResponse.getHeaders()
                                       .getLocation();
        if (location == null) {
            return; // Operation completed synchronously (no job to poll).
        }
        waitForAsyncJob(extractJobGuid(location.toString()), timeout);
    }

    public void waitForAsyncJob(String jobGuid) {
        waitForAsyncJob(jobGuid, DEFAULT_JOB_TIMEOUT);
    }

    /**
     * Poll {@code GET /v3/jobs/{guid}} until the job reaches a terminal state, with exponential backoff bounded by {@code timeout}.
     * Throws {@link CloudOperationException} if the job fails, or if it does not complete within the timeout — matching the OSS
     * {@code JobV3Util} contract.
     */
    public V3Job waitForAsyncJob(String jobGuid, Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        Duration interval = JOB_POLL_MIN_INTERVAL;
        while (true) {
            V3Job job = get("/v3/jobs/" + jobGuid, V3Job.class);
            if (job != null && job.isTerminal()) {
                if (job.isFailed()) {
                    throw jobFailed(job);
                }
                return job;
            }
            if (System.nanoTime() >= deadlineNanos) {
                throw new CloudOperationException(HttpStatus.GATEWAY_TIMEOUT, "Job timeout",
                                                  "Job " + jobGuid + " did not complete within " + timeout);
            }
            sleep(interval);
            interval = nextInterval(interval);
        }
    }

    private static Duration nextInterval(Duration current) {
        Duration doubled = current.multipliedBy(2);
        return doubled.compareTo(JOB_POLL_MAX_INTERVAL) > 0 ? JOB_POLL_MAX_INTERVAL : doubled;
    }

    private static void sleep(Duration interval) {
        try {
            Thread.sleep(interval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new CloudException("Interrupted while polling an async job", e);
        }
    }

    private static CloudOperationException jobFailed(V3Job job) {
        String detail = "Job failed";
        if (job.errors() != null && !job.errors()
                                        .isEmpty()) {
            detail = job.errors()
                        .stream()
                        .map(V3Job.V3Error::detail)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse(detail);
        }
        return new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY, "Job failed", detail);
    }

    private static String extractJobGuid(String location) {
        int idx = location.lastIndexOf("/v3/jobs/");
        if (idx < 0) {
            return location.substring(location.lastIndexOf('/') + 1);
        }
        return location.substring(idx + "/v3/jobs/".length());
    }

    // The next.href CF returns is absolute; RestClient has a baseUrl, so strip the origin to a path+query to keep requests baseUrl-relative.
    private String toRelativeIfAbsolute(String href) {
        if (href == null) {
            return null;
        }
        URI uri = URI.create(href);
        if (uri.isAbsolute()) {
            String pathAndQuery = uri.getRawPath();
            if (uri.getRawQuery() != null) {
                pathAndQuery = pathAndQuery + "?" + uri.getRawQuery();
            }
            return pathAndQuery;
        }
        return href;
    }

    public RestClient getRestClient() {
        return restClient;
    }

}
