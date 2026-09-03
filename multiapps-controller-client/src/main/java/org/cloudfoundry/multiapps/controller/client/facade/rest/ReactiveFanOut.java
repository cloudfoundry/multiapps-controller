package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.Constants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

final class ReactiveFanOut {

    private ReactiveFanOut() {
    }

    static <T, R> List<R> mapConcurrently(List<T> items, Function<? super T, R> mapper) {
        if (items.isEmpty()) {
            return List.of();
        }

        if (items.size() == 1) {
            return List.of(mapper.apply(items.getFirst()));
        }

        return Flux.fromIterable(items)
                   .flatMapSequential(item -> Mono.fromCallable(() -> mapper.apply(item))
                                                  .subscribeOn(Schedulers.boundedElastic()),
                                      Constants.DEFAULT_CONCURRENT_TASKS)
                   .collectList()
                   .block();
    }

}
