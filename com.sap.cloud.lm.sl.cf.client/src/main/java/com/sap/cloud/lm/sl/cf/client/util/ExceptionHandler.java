package com.sap.cloud.lm.sl.cf.client.util;

public interface ExceptionHandler {

    void handleException(Exception e) throws RuntimeException;

}
