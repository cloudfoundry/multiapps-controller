package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Job;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

public class CloudControllerV3Client {

    private final RestClient restClient;

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

        List<R> resultFromAllPages = new ArrayList<>(firstPage.resources());
        resultFromAllPages.addAll(fetchRemainingPagesConcurrently(firstPageRelativeUri, totalPages, pageType));
        return resultFromAllPages;
    }

    private <R> List<R> listSequentially(V3ListResponse<R> firstPage, ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        List<R> resultFromAllPages = new ArrayList<>(firstPage.resources());
        String nextUri = toRelativeIfAbsolute(firstPage.nextPageHref());

        while (nextUri != null) {
            V3ListResponse<R> currentPage = restClient.get()
                                                      .uri(nextUri)
                                                      .retrieve()
                                                      .body(pageType);

            if (currentPage == null) {
                break;
            }

            resultFromAllPages.addAll(currentPage.resources());
            nextUri = toRelativeIfAbsolute(currentPage.nextPageHref());
        }

        return resultFromAllPages;
    }

    private <R> List<R> fetchRemainingPagesConcurrently(String firstPageRelativeUri, int totalPages,
                                                        ParameterizedTypeReference<V3ListResponse<R>> pageType) {
        return Flux.range(2, totalPages - 1)
                   .flatMapSequential(page -> webClient.get()
                                                       .uri(pageUri(firstPageRelativeUri, page))
                                                       .retrieve()
                                                       .bodyToMono(pageType)
                                                       .onErrorMap(WebClientResponseException.class, this::toCloudOperationException),
                                      Constants.MAX_CONCURRENT_PAGES)
                   .flatMapIterable(V3ListResponse::resources)
                   .collectList()
                   .block();
    }

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

    public void followAsyncJob(ResponseEntity<Void> acceptedResponse, Duration timeout) {
        URI location = acceptedResponse.getHeaders()
                                       .getLocation();

        if (location == null) {
            return; // when location is null, the job has already completed and we can return immediately
        }

        waitForAsyncJob(extractJobGuid(location.toString()), timeout);
    }

    public V3Job waitForAsyncJob(String jobGuid, Duration timeout) {
        long startTimeNanos = System.nanoTime();
        long timeoutNanos = timeout.toNanos();
        Duration interval = Constants.JOB_POLL_MIN_INTERVAL;

        while (true) {
            V3Job asyncJob = get("/v3/jobs/" + jobGuid, V3Job.class);

            if (asyncJob != null && asyncJob.isTerminal()) {

                if (asyncJob.isFailed()) {
                    throw jobFailed(asyncJob);
                }

                return asyncJob;
            }

            long elapsedNanos = System.nanoTime() - startTimeNanos;
            if (elapsedNanos >= timeoutNanos) {
                throw new CloudOperationException(HttpStatus.GATEWAY_TIMEOUT, Messages.JOB_TIMEOUT,
                                                  MessageFormat.format(Messages.JOB_0_DID_NOT_COMPLETE_WITHIN_1, jobGuid, timeout));
            }

            long remainingNanos = timeoutNanos - elapsedNanos;
            Duration sleepDuration = interval.toNanos() > remainingNanos ? Duration.ofNanos(remainingNanos) : interval;

            sleep(sleepDuration);
            interval = nextInterval(interval);
        }
    }

    private static Duration nextInterval(Duration current) {
        Duration doubled = current.multipliedBy(2);
        return doubled.compareTo(Constants.JOB_POLL_MAX_INTERVAL) > 0 ? Constants.JOB_POLL_MAX_INTERVAL : doubled;
    }

    private static void sleep(Duration interval) {
        try {
            Thread.sleep(interval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new CloudException(MessageFormat.format(Messages.INTERRUPTED_WHILE_POLLING_ASYNC_JOB_0, e.getMessage()), e);
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

        return new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY, Messages.JOB_FAILED, detail);
    }

    private static String extractJobGuid(String location) {
        int index = location.lastIndexOf("/v3/jobs/");

        if (index < 0) {
            return location.substring(location.lastIndexOf('/') + 1);
        }

        return location.substring(index + "/v3/jobs/".length());
    }

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
