package com.sap.cloud.lm.sl.cf.web.resources.v1;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.web.resources.RestResponse;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@RunWith(Parameterized.class)
public class DeployTargetsResourceTest extends com.sap.cloud.lm.sl.cf.web.resources.DeployTargetsResourceTest {

    private static final URL TARGET_SCHEMA = DeployTargetsResource.class.getResource("/deploy-target-schema-v1.xsd");

    @Mock
    private DeployTargetDao dao;

    @InjectMocks
    DeployTargetsResource resource = new DeployTargetsResource(TARGET_SCHEMA, true);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Get all platforms:
            {
                "targets-1.json", new GetAllRequestInput(), new RestResponse(200, "R:targets-1.xml"),
            },
            // (01) Get platform 1:
            {
                "targets-1.json", new GetRequestInput(1), new RestResponse(200, "R:target-1.xml"),
            },
            // (02) Get platform 2:
            {
                "targets-1.json", new GetRequestInput(2), new RestResponse(200, "R:target-2.xml"),
            },
            // (03) Get non-existing platform:
            {
                "targets-1.json", new GetRequestInput(-2), new RestResponse("E:Deploy target with id \"-2\" does not exist"),
            },
            // (04) No platforms in database:
            {
                "targets-2.json", new GetAllRequestInput(), new RestResponse(200, "R:targets-2.xml"),
            },
            // (05) Create platform:
            {
                "targets-1.json", new PostRequestInput("", "target-3.xml", "target-3-created.xml"), new RestResponse(201, "R:target-3-created.xml"),
            },
            // (06) Create platform with invalid content:
            {
                "targets-1.json", new PostRequestInput("", "target-4.xml", ""), new RestResponse("E:cvc-complex-type.2.4.a: Invalid content was found starting with element parameters."),
            },
            // (07) Create platform with missing organization and space:
            {
                "targets-1.json", new PostRequestInput("", "target-5.xml", ""), new RestResponse("E:Target does not contain 'org' and 'space' properties"),
            },
            // (08) Attempt to create platform when another with the same name already exists:
            {
                "targets-1.json", new PostRequestInput("CF-QUAL", "target-1-duplicate.xml", ""), new RestResponse("E:Deploy target with name \"CF-QUAL\" already exists"),
            },
            // (09) Update platform:
            {
                "targets-1.json", new PutRequestInput(1, "target-6.xml", "target-6-updated.xml"), new RestResponse(200, "R:target-6-updated.xml"),
            },
            // (10) Update platform with invalid content:
            {
                "targets-1.json", new PutRequestInput(1, "target-7.xml", ""), new RestResponse("E:cvc-complex-type.2.4.a: Invalid content was found starting with element parameters."),
            },
            // (11) Update platform with missing organization and space:
            {
                "targets-1.json", new PutRequestInput(1, "target-8.xml", ""), new RestResponse("E:Target does not contain 'org' and 'space' properties"),
            },
            // (12) Attempt to update platform when another with the same name already exists:
            {
                "targets-1.json", new PutRequestInput(1, "target-9.xml", ""), new RestResponse("E:Deploy target with name \"CF-PROD\" already exists"),
            },
            // (13) Delete platform 1:
            {
                "targets-1.json", new DeleteRequestInput(1), new RestResponse(200, ""),
            },
            // (14) Delete non-existing platform:
            {
                "targets-1.json", new DeleteRequestInput(42), new RestResponse("E:Deploy target with id \"42\" does not exist"),
            },
// @formatter:on
        });
    }

    public DeployTargetsResourceTest(String platformsJson, RequestInput input, RestResponse expected) {
        super(platformsJson, input, expected);
    }

    @Before
    public void setUp() throws Throwable {
        MockitoAnnotations.initMocks(this);
        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
        List<Target> rawTargets = new ConfigurationParser().parseTargetsJson(getClass().getResourceAsStream(targetsJson));
        targets = new ArrayList<>();
        int id = 1;
        for (Target rawTarget : rawTargets) {
            targets.add(new PersistentObject<Target>(id, rawTarget));
            id++;
        }
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao getDao() {
        return dao;
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.web.resources.DeployTargetsResource getResource() {
        return resource;
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto deployTargetDtoFromXml(String path) {
        return XmlUtil.fromXml(getClass().getResourceAsStream(path), DeployTargetDto.class);
    }
}
