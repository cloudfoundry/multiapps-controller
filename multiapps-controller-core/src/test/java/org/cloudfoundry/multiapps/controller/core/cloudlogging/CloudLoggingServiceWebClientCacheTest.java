package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class CloudLoggingServiceWebClientCacheTest {

    private CloudLoggingServiceWebClientCache cache;

    @BeforeEach
    void setUp() {
        cache = new CloudLoggingServiceWebClientCache();
    }

    @Test
    void getOrCreate_cacheMiss_invokesSupplierAndReturnsResult() {
        WebClient expected = mock(WebClient.class);
        AtomicInteger calls = new AtomicInteger();
        Function<LoggingConfiguration, WebClient> supplier = cfg -> {
            calls.incrementAndGet();
            return expected;
        };

        WebClient result = cache.getOrCreate(config("op-1"), supplier);

        assertSame(expected, result);
        assertEquals(1, calls.get());
    }

    @Test
    void getOrCreate_cacheHit_doesNotInvokeSupplier() {
        WebClient first = mock(WebClient.class);
        AtomicInteger calls = new AtomicInteger();
        Function<LoggingConfiguration, WebClient> supplier = cfg -> {
            calls.incrementAndGet();
            return first;
        };

        WebClient a = cache.getOrCreate(config("op-1"), supplier);
        WebClient b = cache.getOrCreate(config("op-1"), cfg -> {
            throw new AssertionError("supplier should not be called on cache hit");
        });

        assertSame(a, b);
        assertEquals(1, calls.get());
    }

    @Test
    void getOrCreate_supplierReturnsNull_notCached_andSubsequentCallInvokesSupplierAgain() {
        AtomicInteger calls = new AtomicInteger();
        Function<LoggingConfiguration, WebClient> supplier = cfg -> {
            calls.incrementAndGet();
            return null;
        };

        WebClient first = cache.getOrCreate(config("op-1"), supplier);
        WebClient second = cache.getOrCreate(config("op-1"), supplier);

        assertNull(first);
        assertNull(second);
        assertEquals(2, calls.get());
    }

    @Test
    void getOrCreate_differentOperationIds_areCachedSeparately() {
        WebClient client1 = mock(WebClient.class);
        WebClient client2 = mock(WebClient.class);

        WebClient a = cache.getOrCreate(config("op-1"), cfg -> client1);
        WebClient b = cache.getOrCreate(config("op-2"), cfg -> client2);

        assertSame(client1, a);
        assertSame(client2, b);
        assertSame(client1, cache.getOrCreate(config("op-1"), cfg -> {
            throw new AssertionError("supplier should not be called on cache hit");
        }));
        assertSame(client2, cache.getOrCreate(config("op-2"), cfg -> {
            throw new AssertionError("supplier should not be called on cache hit");
        }));
    }

    @Test
    void evict_removesEntry_soNextGetOrCreateInvokesSupplierAgain() {
        AtomicInteger calls = new AtomicInteger();
        Function<LoggingConfiguration, WebClient> supplier = cfg -> {
            calls.incrementAndGet();
            return mock(WebClient.class);
        };

        cache.getOrCreate(config("op-1"), supplier);
        cache.remove("op-1");
        cache.getOrCreate(config("op-1"), supplier);

        assertEquals(2, calls.get());
    }

    @Test
    void evict_unknownOperationId_doesNotThrow() {
        cache.remove("never-cached");
    }

    private static LoggingConfiguration config(String operationId) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId(operationId)
                                            .mtaSpaceId("space-1")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(true)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("server-ca")
                                            .clientCert("client-cert")
                                            .clientKey("client-key")
                                            .build();
    }
}
