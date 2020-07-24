package org.cloudfoundry.multiapps.controller.web.interceptors;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Marker interface via which all custom interceptors are injected as a list in
 * {@link org.cloudfoundry.multiapps.controller.web.configuration.WebMvcConfiguration}.
 *
 */
public interface CustomHandlerInterceptor extends HandlerInterceptor {

}
