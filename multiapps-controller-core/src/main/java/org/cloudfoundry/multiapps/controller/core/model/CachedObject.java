package org.cloudfoundry.multiapps.controller.core.model;

import java.time.Duration;
import java.util.function.Supplier;

public class CachedObject<T> {

    private T object;
    private final long expirationTimestamp;

    public CachedObject(Duration expirationDuration) {
        this(null, System.currentTimeMillis() + expirationDuration.toMillis());
    }

    public CachedObject(T object, long expirationTimestamp) {
        this.object = object;
        this.expirationTimestamp = expirationTimestamp;
    }

    public synchronized T get() {
        return object;
    }

    public synchronized T getOrRefresh(Supplier<T> refresher) {
        if (isExpired() || object == null) {
            return object = refresher.get();
        }
        return object;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTimestamp;
    }

    public boolean isExpired(long currentTime) {
        return currentTime >= expirationTimestamp;
    }

}
