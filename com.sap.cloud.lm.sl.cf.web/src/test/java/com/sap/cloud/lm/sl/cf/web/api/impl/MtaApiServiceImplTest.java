package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedMtaDetector;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;
import com.sap.cloud.lm.sl.cf.web.api.model.Metadata;
import com.sap.cloud.lm.sl.cf.web.api.model.Module;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class MtaApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;

    @Mock
    private AuthorizationChecker authorizationChecker;

    @Mock
    private HttpServletRequest request;

    @Mock
    private CloudControllerClient client;

    @Mock
    private DeployedMtaDetector deployedMtaDetector;

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
        mockClient(USER_NAME);
    }

    private List<CloudApplication> parseApps() {
        String appsJson = TestUtil.getResourceAsString("apps-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<List<CloudApplication>>() {
        });
    }

    private List<Mta> parseMtas() {
        String appsJson = TestUtil.getResourceAsString("mtas-01.json", getClass());
        return JsonUtil.fromJson(appsJson, new TypeReference<List<Mta>>() {
        });
    }

    @Test
    public void testGetMtas() {
        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        mtas.equals(responseMtas);
    }

    @Test
    public void testGetMta() {
        Mta mtaToGet = mtas.get(1);
        ResponseEntity<Mta> response = testedClass.getMta(SPACE_GUID, mtaToGet.getMetadata()
                                                                              .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mta responseMtas = response.getBody();
        mtaToGet.equals(responseMtas);
    }

    @Test
    public void testGetMtaNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getMta(SPACE_GUID, "not_a_real_mta"));
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
        Mockito.when(client.getApplications())
               .thenReturn(apps);
        Mockito.when(deployedMtaDetector.detectDeployedMtas(Mockito.any()))
               .thenReturn(getDeployedMtas(mtas));
        Mockito.when(deployedMtaDetector.detectDeployedMta(mtas.get(1)
                                                               .getMetadata()
                                                               .getId(),
                                                           client))
               .thenReturn(Optional.of(getDeployedMta(mtas.get(1))));
    }

    private List<DeployedMta> getDeployedMtas(List<Mta> mtas) {
        return mtas.stream()
                   .map(this::getDeployedMta)
                   .collect(Collectors.toList());
    }

    private DeployedMta getDeployedMta(Mta mta) {
        return ImmutableDeployedMta.builder()
                                   .metadata(getMtaMetadata(mta.getMetadata()))
                                   .applications(getDeployedMtaApplications(mta.getModules()))
                                   .services(mta.getServices()
                                                .stream()
                                                .map(this::getDeployedMtaService)
                                                .collect(Collectors.toList()))
                                   .build();
    }

    private List<DeployedMtaApplication> getDeployedMtaApplications(List<Module> modules) {
        return modules.stream()
                      .map(this::getDeployedMtaApplication)
                      .collect(Collectors.toList());
    }

    private DeployedMtaApplication getDeployedMtaApplication(Module module) {
        List<String> services = module.getServices();
        return ImmutableDeployedMtaApplication.builder()
                                              .name(module.getAppName())
                                              .moduleName(module.getModuleName())
                                              .providedDependencyNames(module.getProvidedDendencyNames())
                                              .uris(module.getUris())
                                              .services(services)
                                              .build();
    }

    private DeployedMtaService getDeployedMtaService(String service) {
        return ImmutableDeployedMtaService.builder()
                                          .name(service)
                                          .resourceName(service)
                                          .build();
    }

    private MtaMetadata getMtaMetadata(Metadata metadata) {
        return ImmutableMtaMetadata.builder()
                                   .id(metadata.getId())
                                   .version(Version.parseVersion(metadata.getVersion()))
                                   .build();
    }

}
