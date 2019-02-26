package com.sap.cloud.lm.sl.cf.core.model;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CachedObject<T> {

    private T object;
    private long expirationTimeInSeconds;
    private long lastRefreshTime;
    private Supplier<Long> currentTimeSupplier;

    public CachedObject(long expirationTimeInSeconds) {
        this(expirationTimeInSeconds, System::currentTimeMillis);
    }

    public CachedObject(long expirationTimeInSeconds, Supplier<Long> currentTimeSupplier) {
        this.expirationTimeInSeconds = expirationTimeInSeconds;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public synchronized T get(Supplier<T> refreshFunction) {
        long currentTime = currentTimeSupplier.get();
        long millisecondsSinceLastRefresh = currentTime - lastRefreshTime;
        long secondsSinceLastRefresh = TimeUnit.MILLISECONDS.toSeconds(millisecondsSinceLastRefresh);
        if (object == null || secondsSinceLastRefresh > expirationTimeInSeconds) {
            this.object = refreshFunction.get();
            this.lastRefreshTime = currentTime;
        }
        return object;
    }

    public synchronized T forceRefresh(Supplier<T> refreshFunction) {
        lastRefreshTime = currentTimeSupplier.get();
        object = refreshFunction.get();
        return object;
    }

}
