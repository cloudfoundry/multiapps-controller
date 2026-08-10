package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;

class ReactiveFanOutTest {

    @Test
    void testEmptyReturnsEmpty() {
        Assertions.assertEquals(List.of(), ReactiveFanOut.mapConcurrently(List.of(), x -> x));
    }

    @Test
    void testSingleElementShortCircuits() {
        List<String> result = ReactiveFanOut.mapConcurrently(List.of("a"), s -> s + "!");

        Assertions.assertEquals(List.of("a!"), result);
    }

    @Test
    void testResultsPreserveInputOrderEvenWhenTasksFinishOutOfOrder() {
        List<Integer> input = IntStream.rangeClosed(1, 20)
                                       .boxed()
                                       .toList();

        // Later items finish sooner (inverse sleep) — the result must still be in input order.
        List<Integer> result = ReactiveFanOut.mapConcurrently(input, i -> {
            sleep((20 - i));
            return i * 10;
        });

        List<Integer> expected = input.stream()
                                      .map(i -> i * 10)
                                      .toList();
        Assertions.assertEquals(expected, result);
    }

    @Test
    @Timeout(10)
    void testWorkRunsConcurrently() throws Exception {
        int n = 8;
        // If the mapper ran sequentially, the barrier (which needs all n parties) would never trip and the test would time out.
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Integer> input = IntStream.range(0, n)
                                       .boxed()
                                       .toList();

        List<Integer> result = ReactiveFanOut.mapConcurrently(input, i -> {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return i;
        });

        Assertions.assertEquals(input, result);
    }

    @Test
    void testFirstFailurePropagatesUnwrapped() {
        List<Integer> input = List.of(1, 2, 3, 4);

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> ReactiveFanOut.mapConcurrently(input, i -> {
                                                                     if (i == 3) {
                                                                         throw new CloudOperationException(HttpStatus.BAD_GATEWAY);
                                                                     }
                                                                     return i;
                                                                 }));

        Assertions.assertEquals(HttpStatus.BAD_GATEWAY, thrown.getStatusCode());
    }

    @Test
    void testMapperInvokedOncePerItem() {
        List<Integer> input = IntStream.range(0, 50)
                                       .boxed()
                                       .toList();
        AtomicInteger invocations = new AtomicInteger();

        List<Integer> result = ReactiveFanOut.mapConcurrently(input, i -> {
            invocations.incrementAndGet();
            return i;
        });

        Assertions.assertEquals(input, result);
        Assertions.assertEquals(input.size(), invocations.get());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
    }

}
