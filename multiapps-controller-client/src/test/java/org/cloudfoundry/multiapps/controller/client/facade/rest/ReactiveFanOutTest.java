package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
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
    void testMapConcurrentlyReturnsEmptyWhenEmpty() {
        Assertions.assertEquals(List.of(), ReactiveFanOut.mapConcurrently(List.of(), x -> x));
    }

    @Test
    void testMapConcurrentlyShortCircuitsWhenSingleElement() {
        List<String> result = ReactiveFanOut.mapConcurrently(List.of("a"), s -> s + "!");

        Assertions.assertEquals(List.of("a!"), result);
    }

    @Test
    @Timeout(10)
    void testMapConcurrentlyResultPreserveInputOrderEvenWhenTasksFinishOutOfOrder() throws Exception {
        List<Integer> input = IntStream.rangeClosed(1, 20)
                                       .boxed()
                                       .toList();
        CountDownLatch lastItemDone = new CountDownLatch(1);

        List<Integer> result = ReactiveFanOut.mapConcurrently(input, i -> {
            if (i == 20) {
                lastItemDone.countDown();
            } else if (i == 1) {
                try {
                    lastItemDone.await();
                } catch (InterruptedException e) {
                    Thread.currentThread()
                          .interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return i * 10;
        });

        List<Integer> expected = input.stream()
                                      .map(i -> i * 10)
                                      .toList();
        Assertions.assertEquals(expected, result);
    }

    @Test
    @Timeout(10)
    void testMapConcurrentlyWorkRunsConcurrently() {
        int n = 8;
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
    void testMapConcurrentlyPropagatesUnwrappedOnFirstFailure() {
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
    void testMapConcurrentlyIsInvokedOncePerItem() {
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

}
