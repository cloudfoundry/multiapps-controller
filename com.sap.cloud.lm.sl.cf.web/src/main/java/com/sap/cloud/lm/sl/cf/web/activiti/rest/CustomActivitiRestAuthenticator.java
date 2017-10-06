package com.sap.cloud.lm.sl.cf.web.activiti.rest;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.rest.common.filter.RestAuthenticator;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ClientInfo;
import org.restlet.security.User;

public class CustomActivitiRestAuthenticator implements RestAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(CustomActivitiRestAuthenticator.class);

    private static final String ACTIVITI_API_PATH = "/activiti";
    private static final String EMAIL_ATTRIBUTE = "email";

    private IdentityService identityService;

    public CustomActivitiRestAuthenticator() {
        identityService = ProcessEngines.getDefaultProcessEngine().getIdentityService();
    }

    @Override
    public boolean isRequestAuthorized(Request request) {
        return false;
    }

    @Override
    public boolean requestRequiresAuthentication(Request request) {
        
        // Get relevant information from the request
        String userId = getUserId(request);
        String userId2 = getUserIdFromHeader(request);
        String password = getPassword(request);
        String apiCalled = getApiCalled(request);
        
        // Check if the user is authorized to perform the operation
        ensureUserIsAuthorized(apiCalled, userId);

        // If the BASIC authentication header is present and the user is the session user,
        // persist the user in the Activiti database
        if (userId2 != null && password != null && userId2.equals(userId)) {
            persistUser(userId, password);
        }
        
        // Set authenticated user id for this Activiti thread
        Authentication.setAuthenticatedUserId(userId);
        
        // Update request client info
        setRequestClientInfo(request, userId);
        
        // Return false to skip any Activiti-specific authentication
        return false;
    }

    private String getUserId(Request request) {
        return request.getClientInfo().getPrincipals().get(0).getName();
    }

    private String getUserIdFromHeader(Request request) {
        ChallengeResponse cr = request.getChallengeResponse();
        return (cr != null) ? cr.getIdentifier() : null;
    }

    private String getPassword(Request request) {
        ChallengeResponse cr = request.getChallengeResponse();
        return (cr != null) ? new String(cr.getSecret()) : null;
    }

    private String getApiCalled(Request request) {
        return request.getResourceRef().toString().substring(
            request.getResourceRef().toString().indexOf(ACTIVITI_API_PATH));
    }

    private void ensureUserIsAuthorized(String apiCalled, String userId) {
        // TODO Implement authorization
    }

    private void persistUser(String userId, String password) {
        org.activiti.engine.identity.User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            user = identityService.newUser(userId);
        }
        user.setPassword(password);
        Map<String, String> attributes = getUserAttributes(userId);
        user.setEmail(attributes.get(EMAIL_ATTRIBUTE));
        // TODO Set also first and last name
        identityService.saveUser(user);
    }

    private Map<String, String> getUserAttributes(String userId) {
        Map<String, String> attributes = new HashMap<>();
        // TODO Get user attributes (if supported)
        return attributes;
    }

    private void setRequestClientInfo(Request request, String userId) {
        ClientInfo ci = request.getClientInfo();
        User user = new User();
        user.setIdentifier(userId);
        ci.setUser(user);
        request.setClientInfo(ci);
    }
}