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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;

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

    // Reactor's default flatMap concurrency — the in-flight cap the OSS PaginationUtils effectively used when fetching pages 2..N.
    private static final int MAX_CONCURRENT_PAGES = 256;

    private final RestClient restClient;
    // Optional reactive client on the SAME reactor-netty transport, used only to fetch pages 2..N concurrently (as the OSS client did via
    // Reactor flatMap). When null (e.g. the auxiliary space/log-cache clients that never paginate large sets), list() falls back to the
    // sequential next.href walk.
    private final WebClient webClient;

    public CloudControllerV3Client(RestClient restClient) {
        this(restClient, null);
    }

    public CloudControllerV3Client(RestClient restClient, WebClient webClient) {
        this.restClient = restClient;
        this.webClient = webClient;
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
     * List every resource of a v3 list endpoint across all pages. Mirrors the OSS {@code PaginationUtils.requestClientV3Resources}:
     * fetch page 1, read {@code pagination.total_pages}, then request pages {@code 2..N} <b>concurrently</b> (bounded by
     * {@value #MAX_CONCURRENT_PAGES}) via the reactive {@link WebClient} on the shared transport, and concatenate the results in page
     * order. This restores the intra-operation concurrency the OSS client had on large, multi-page listings.
     * <p>
     * Falls back to the sequential {@code pagination.next.href} walk when no {@link WebClient} is configured or the response does not
     * report {@code total_pages} — preserving correctness on any endpoint that omits it.
     */
    public <R> List<R> list(String firstPageUri, ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        String firstPageRelativeUri = toRelativeIfAbsolute(firstPageUri);
        V3ListResponse<R> firstPage = restClient.get()
                                                .uri(firstPageRelativeUri)
                                                .retrieve()
                                                .body(pageType);
        if (firstPage == null) {
            return new ArrayList<>();
        }
        Integer totalPages = firstPage.pagination() == null ? null
            : firstPage.pagination()
                       .totalPages();
        if (webClient == null || totalPages == null || totalPages <= 1) {
            return listSequentially(firstPage, pageType);
        }
        List<R> all = new ArrayList<>(firstPage.resources());
        all.addAll(fetchRemainingPagesConcurrently(firstPageRelativeUri, totalPages, pageType));
        return all;
    }

    // Sequential pagination via pagination.next.href — the fallback path (and the whole behaviour when no WebClient is set).
    private <R> List<R> listSequentially(V3ListResponse<R> firstPage, ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        List<R> all = new ArrayList<>(firstPage.resources());
        String nextUri = toRelativeIfAbsolute(firstPage.nextPageHref());
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

    // Fetch pages 2..totalPages concurrently on the reactive transport, preserving page order (flatMapSequential), and flatten their
    // resources. Errors propagate unwrapped, matching the RestClient error mapping (WebClient maps non-2xx to WebClientResponseException;
    // we surface it as a CloudOperationException to keep callers' handling uniform).
    private <R> List<R> fetchRemainingPagesConcurrently(String firstPageRelativeUri, int totalPages,
                                                        ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        return Flux.range(2, totalPages - 1)
                   .flatMapSequential(page -> webClient.get()
                                                       .uri(pageUri(firstPageRelativeUri, page))
                                                       .retrieve()
                                                       .bodyToMono(pageType)
                                                       .onErrorMap(WebClientResponseException.class, this::toCloudOperationException),
                                      MAX_CONCURRENT_PAGES)
                   .flatMapIterable(V3ListResponse::resources)
                   .collectList()
                   .block();
    }

    // Set/replace the "page" query parameter on the first-page URI, keeping per_page and every filter intact. Package-visible for testing.
    static String pageUri(String firstPageRelativeUri, int page) {
        return UriComponentsBuilder.fromUriString(firstPageRelativeUri)
                                   .replaceQueryParam("page", page)
                                   .build()
                                   .toUriString();
    }

    private CloudOperationException toCloudOperationException(WebClientResponseException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode()
                                                .value());
        return new CloudOperationException(status, status.getReasonPhrase(), e.getResponseBodyAsString(), e);
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
