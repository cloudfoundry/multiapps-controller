package com.sap.cloud.lm.sl.cf.core.model;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class CachedObject<T> {

    private T object;
    private long expirationTimeInSeconds;
    private long lastRefreshTime;
    private LongSupplier currentTimeSupplier;

    public CachedObject(long expirationTimeInSeconds) {
        this(expirationTimeInSeconds, System::currentTimeMillis);
    }

    public CachedObject(long expirationTimeInSeconds, LongSupplier currentTimeSupplier) {
        this.expirationTimeInSeconds = expirationTimeInSeconds;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public synchronized T get(Supplier<T> refreshFunction) {
        long currentTime = currentTimeSupplier.getAsLong();
        long millisecondsSinceLastRefresh = currentTime - lastRefreshTime;
        long secondsSinceLastRefresh = TimeUnit.MILLISECONDS.toSeconds(millisecondsSinceLastRefresh);
        if (object == null || secondsSinceLastRefresh > expirationTimeInSeconds) {
            this.object = refreshFunction.get();
            this.lastRefreshTime = currentTimeSupplier.getAsLong();
        }
        return object;
    }

    public synchronized T forceRefresh(Supplier<T> refreshFunction) {
        object = refreshFunction.get();
        lastRefreshTime = currentTimeSupplier.getAsLong();
        return object;
    }

}
