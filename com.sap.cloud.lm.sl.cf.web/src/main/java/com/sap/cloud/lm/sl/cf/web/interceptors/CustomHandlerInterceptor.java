package com.sap.cloud.lm.sl.cf.web.interceptors;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Marker interface via which all custom interceptors are injected as a list in
 * {@link com.sap.cloud.lm.sl.cf.web.configuration.WebMvcConfiguration}.
 *
 */
public interface CustomHandlerInterceptor extends HandlerInterceptor {

}
