package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
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
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class MtasApiServiceImplTest {

    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;

    @Mock
    private DeployedMtaDetector deployedMtaDetector;

    @InjectMocks
    private MtasApiServiceImpl testedClass;

    private List<CloudApplication> apps;
    private List<Mta> mtas;

    private static final String USER_NAME = "someUser";
    private static final String SPACE_GUID = UUID.randomUUID()
                                                 .toString();

    @BeforeEach
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        apps = parseApps();
        mtas = parseMtas();
        mockClient();
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
        Mockito.when(deployedMtaDetector.detectDeployedMtasWithoutNamespace(Mockito.any()))
               .thenReturn(getDeployedMtas(mtas));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(mtas, responseMtas);
    }

    @Test
    public void testGetAllMtas() {
        Mockito.when(deployedMtaDetector.detectDeployedMtas(Mockito.any()))
               .thenReturn(getDeployedMtas(mtas));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(mtas, responseMtas);
    }

    @Test
    public void testGetMtasByName() {
        Mta mtaToGet = mtas.get(1);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName(mtaToGet.getMetadata()
                                                                          .getId(),
                                                                  client))
               .thenReturn(Arrays.asList(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, null, mtaToGet.getMetadata()
                                                                                           .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(Arrays.asList(mtaToGet), responseMtas);
    }

    @Test
    public void testGetMtasByNamespace() {
        Mta mtaToGet = mtas.get(0);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByNamespace(mtaToGet.getMetadata()
                                                                               .getNamespace(),
                                                                       client))
               .thenReturn(Arrays.asList(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                 null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(Arrays.asList(mtaToGet), responseMtas);
    }

    @Test
    public void testGetMtasByNameAndNamespace() {
        Mta mtaToGet = mtas.get(0);
        Mockito.when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaToGet.getMetadata()
                                                                                     .getId(),
                                                                             mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                             client, true))
               .thenReturn(Optional.of(getDeployedMta(mtaToGet)));

        ResponseEntity<List<Mta>> response = testedClass.getMtas(SPACE_GUID, mtaToGet.getMetadata()
                                                                                     .getNamespace(),
                                                                 mtaToGet.getMetadata()
                                                                         .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Mta> responseMtas = response.getBody();
        assertEquals(Arrays.asList(mtaToGet), responseMtas);
    }

    @Test
    public void testGetMta() {
        Mta mtaToGet = mtas.get(1);
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName(mtaToGet.getMetadata()
                                                                          .getId(),
                                                                  client))
               .thenReturn(Arrays.asList(getDeployedMta(mtaToGet)));

        ResponseEntity<Mta> response = testedClass.getMta(SPACE_GUID, mtaToGet.getMetadata()
                                                                              .getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mta responseMta = response.getBody();
        assertEquals(mtaToGet, responseMta);
    }

    @Test
    public void testGetMtaNotFound() {
        Assertions.assertThrows(NotFoundException.class, () -> testedClass.getMta(SPACE_GUID, "not_a_real_mta"));
    }

    @Test
    public void testGetMtaNotUniqueByName() {
        Mockito.when(deployedMtaDetector.detectDeployedMtasByName("name_thats_not_unique", client))
               .thenReturn(getDeployedMtas(mtas));

        Assertions.assertThrows(ConflictException.class, () -> testedClass.getMta(SPACE_GUID, "name_thats_not_unique"));
    }

    private void mockClient() {
        UserInfo userInfo = new UserInfo(null, USER_NAME, null);
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getPrincipal())
               .thenReturn(userInfo);
        SecurityContext securityContextMock = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContextMock);
        Mockito.when(securityContextMock.getAuthentication())
               .thenReturn(auth);
        Mockito.when(clientProvider.getControllerClient(Mockito.anyString(), Mockito.anyString()))
               .thenReturn(client);
        Mockito.when(client.getApplications())
               .thenReturn(apps);
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
                                              .boundMtaServices(services)
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
                                   .namespace(metadata.getNamespace())
                                   .build();
    }

}
