package com.sap.cloud.lm.sl.cf.web.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.immutables.value.Value;

@Value.Immutable
public interface HttpCommunication {

    @Value.Parameter
    HttpServletRequest getRequest();

    @Value.Parameter
    HttpServletResponse getResponse();

}
