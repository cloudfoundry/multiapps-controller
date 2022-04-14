package org.cloudfoundry.multiapps.controller.client.util;

@FunctionalInterface
public interface CheckedSupplier<T> {

    T get() throws Exception;
}
