package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;

public interface UriAuthorizationFilter {

    String getUriRegex();

    void ensureUserIsAuthorized(HttpCommunication communication) throws IOException;

}
