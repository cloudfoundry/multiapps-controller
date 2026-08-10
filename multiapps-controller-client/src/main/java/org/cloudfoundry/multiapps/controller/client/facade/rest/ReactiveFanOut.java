package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Restores the <em>intra-operation concurrency</em> the OSS cf-java-client had for free. The OSS client composed per-item work
 * (e.g. fetching credentials for N service keys, or resolving the plan/offering of N service instances) as a reactive graph and let
 * Reactor's {@code flatMap} run those calls concurrently (default concurrency 256) before a single {@code .block()} at the boundary.
 * The synchronous {@link RestClient}-based rewrite would otherwise do the same work strictly sequentially ({@code stream().map(fetch)}).
 * <p>
 * This helper reproduces exactly that pattern — {@code Flux.fromIterable(...).flatMap(...).block()} — reusing the existing
 * <b>blocking</b> per-item mappers unchanged. Each blocking mapper is offloaded onto {@link Schedulers#boundedElastic()} so the calls
 * run concurrently without pinning the caller's thread; {@code flatMap} bounds how many are in flight (Reactor's default 256, matching
 * the OSS cap). Results are returned <b>in input order</b> ({@link Flux#flatMapSequential} preserves order while still subscribing
 * eagerly), and the first error aborts the rest and propagates unwrapped — the same fail-fast semantics a {@code .block()} over a failed
 * {@code flatMap} had in the OSS path.
 * <p>
 * This uses only Reactor (a Spring library already on the classpath as the transport); it does <b>not</b> reintroduce cf-java-client.
 */
final class ReactiveFanOut {

    // Reactor's default flatMap concurrency — the in-flight cap the OSS client effectively used for per-item fan-out.
    private static final int MAX_CONCURRENCY = 256;

    private ReactiveFanOut() {
    }

    /**
     * Apply {@code mapper} (a blocking function) to every item concurrently, bounded by {@value #MAX_CONCURRENCY} in-flight calls, and
     * return the results in the same order as {@code items}. The single-element and empty cases short-circuit without touching the
     * reactive machinery (the overwhelmingly common case for typical MTAs).
     */
    static <T, R> List<R> mapConcurrently(List<T> items, Function<? super T, R> mapper) {
        if (items.isEmpty()) {
            return List.of();
        }
        if (items.size() == 1) {
            return List.of(mapper.apply(items.get(0)));
        }
        return Flux.fromIterable(items)
                   .flatMapSequential(item -> Mono.fromCallable(() -> mapper.apply(item))
                                                  .subscribeOn(Schedulers.boundedElastic()),
                                      MAX_CONCURRENCY)
                   .collectList()
                   .block();
    }

}
