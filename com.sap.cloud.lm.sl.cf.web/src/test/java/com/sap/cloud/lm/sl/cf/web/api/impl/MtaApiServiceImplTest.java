package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class MtaApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;

    @Mock
    private AuthorizationChecker authorizationChecker;

    @Mock
    private HttpServletRequest request;

    @Mock
    private CloudControllerClient client;

    @InjectMocks
    private MtasApiServiceImpl testedClass;

    List<CloudApplication> apps;
    List<Mta> mtas;

    private static final String USER_NAME = "someUser";
    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";

    @Before
    public void initialize() throws Exception {
        MockitoAnnotations.initMocks(this);
        apps = parseApps();
        mtas = parseMtas();
    }

    private List<CloudApplication> parseApps() throws IOException {
        String appsJson = TestUtil.getResourceAsString("apps-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<List<CloudApplication>>() {
        });
    }

    private List<Mta> parseMtas() throws IOException {
        String appsJson = TestUtil.getResourceAsString("mtas-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<List<Mta>>() {
        });
    }

    @Test
    public void testGetMtas() throws Exception {
        Response response = testedClass.getMtas(mockSecurityContext(USER_NAME), SPACE_GUID, request);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<Mta> responseMtas = (List<Mta>) response.getEntity();
        mtas.equals(responseMtas);

    }

    @Test
    public void testGetMta() throws Exception {
        Mta mtaToGet = mtas.get(1);
        Response response = testedClass.getMta(mtaToGet.getMetadata()
                                                       .getId(),
                                               mockSecurityContext(USER_NAME), SPACE_GUID, request);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Mta responseMtas = (Mta) response.getEntity();
        mtaToGet.equals(responseMtas);

    }

    @Test
    public void testGetMtaNotFound() throws Exception {
        Assertions.assertThrows(NotFoundException.class,
                                () -> testedClass.getMta("not_a_real_mta", mockSecurityContext(USER_NAME), SPACE_GUID, request));
    }

    private SecurityContext mockSecurityContext(String user) {
        SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
        if (user != null) {
            Principal principalMock = Mockito.mock(Principal.class);
            Mockito.when(principalMock.getName())
                   .thenReturn(user);
            Mockito.when(securityContextMock.getUserPrincipal())
                   .thenReturn(principalMock);
            mockClient(user);
        }
        return securityContextMock;
    }

    private void mockClient(String user) {
        com.sap.cloud.lm.sl.cf.core.util.UserInfo userInfo = new com.sap.cloud.lm.sl.cf.core.util.UserInfo(null, user, null);
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getPrincipal())
               .thenReturn(userInfo);
        org.springframework.security.core.context.SecurityContext securityContextMock = Mockito.mock(org.springframework.security.core.context.SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        Mockito.when(securityContextMock.getAuthentication())
               .thenReturn(auth);
        Mockito.when(clientProvider.getControllerClient(Mockito.anyString(), Mockito.anyString()))
               .thenReturn(client);
        Mockito.when(client.getApplications(Mockito.anyBoolean()))
               .thenReturn(apps);
    }

}
