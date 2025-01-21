package org.cloudfoundry.multiapps.controller.core.model;

import java.time.Duration;
import java.util.function.Supplier;

public class CachedObject<T> {

    private T object;
    private long expirationTimestamp;

    private final Duration expirationDuration;

    public CachedObject(Duration expirationDuration) {
        this(null, expirationDuration);
    }

    public CachedObject(T object, Duration expirationDuration) {
        this.object = object;
        this.expirationDuration = expirationDuration;
        this.expirationTimestamp = System.currentTimeMillis() + expirationDuration.toMillis();
    }

    public synchronized T get() {
        return object;
    }

    public synchronized T getOrRefresh(Supplier<T> refresher) {
        if (isExpired() || object == null) {
            expirationTimestamp = System.currentTimeMillis() + expirationDuration.toMillis();
            object = refresher.get();
        }
        return object;
    }

    public synchronized void refresh(Supplier<T> refresher) {
        expirationTimestamp = System.currentTimeMillis() + expirationDuration.toMillis();
        object = refresher.get();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTimestamp;
    }

    public boolean isExpired(long currentTime) {
        return currentTime >= expirationTimestamp;
    }

}
