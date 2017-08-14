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
import com.sap.cloud.lm.sl.cf.web.resources.RestResponse;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@RunWith(Parameterized.class)
@SuppressWarnings("deprecation")
public class TargetPlatformsResourceTest extends com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResourceTest {

    private static final URL PLATFORM_SCHEMA = TargetPlatformsResource.class.getResource("/target-platform-schema-v1.xsd");

    @Mock
    private DeployTargetDao dao;

    @InjectMocks
    TargetPlatformsResource resource = new TargetPlatformsResource(PLATFORM_SCHEMA, true);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Get all platforms:
            {
                "platforms-1.json", new GetAllRequestInput(), new RestResponse(200, "R:platforms-1.xml"),
            },
            // (01) Get platform 1:
            {
                "platforms-1.json", new GetRequestInput("CF-QUAL"), new RestResponse(200, "R:platform-1.xml"),
            },
            // (02) Get platform 2:
            {
                "platforms-1.json", new GetRequestInput("CF-PROD"), new RestResponse(200, "R:platform-2.xml"),
            },
            // (03) Get non-existing platform:
            {
                "platforms-1.json", new GetRequestInput("XS-QUAL"), new RestResponse("E:Deploy target with name \"XS-QUAL\" does not exist"),
            },
            // (04) No platforms in database:
            {
                "platforms-2.json", new GetAllRequestInput(), new RestResponse(200, "R:platforms-2.xml"),
            },
            // (05) Create platform:
            {
                "platforms-1.json", new PostRequestInput("XS-QUAL", "platform-3.xml"), new RestResponse(201, ""),
            },
            // (06) Create platform with invalid content:
            {
                "platforms-1.json", new PostRequestInput("XS-QUAL", "platform-4.xml"), new RestResponse("E:cvc-complex-type.2.4.a: Invalid content was found starting with element parameters. One of {properties, module-types, resource-types} is expected."),
            },
            // (07) Create platform with missing organization and space:
            {
                "platforms-1.json", new PostRequestInput("XS-QUAL", "platform-5.xml"), new RestResponse("E:Target does not contain 'org' and 'space' properties"),
            },
            // (08) Attempt to create platform when another with the same name already exists:
            {
                "platforms-1.json", new PostRequestInput("CF-QUAL", "platform-1.xml"), new RestResponse("E:Deploy target with name \"CF-QUAL\" already exists"),
            },
            // (09) Update platform:
            {
                "platforms-1.json", new PutRequestInput("CF-QUAL", "CF-TEST", "platform-6.xml"), new RestResponse(200, ""),
            },
            // (10) Update platform with invalid content:
            {
                "platforms-1.json", new PutRequestInput("CF-QUAL", "CF-QUAL", "platform-7.xml"), new RestResponse("E:cvc-complex-type.2.4.a: Invalid content was found starting with element parameters. One of {name, type, description, properties, module-types, resource-types} is expected."),
            },
            // (11) Update platform with missing organization and space:
            {
                "platforms-1.json", new PutRequestInput("CF-QUAL", "CF-QUAL", "platform-8.xml"), new RestResponse("E:Target does not contain 'org' and 'space' properties"),
            },
            // (12) Attempt to update platform when another with the same name already exists:
            {
                "platforms-1.json", new PutRequestInput("CF-QUAL", "CF-PROD", "platform-9.xml"), new RestResponse("E:Deploy target with name \"CF-PROD\" already exists"),
            },
            // (13) Delete platform 1:
            {
                "platforms-1.json", new DeleteRequestInput("CF-QUAL"), new RestResponse(200, ""),
            },
            // (14) Delete non-existing platform:
            {
                "platforms-1.json", new DeleteRequestInput("CF-TEST"), new RestResponse("E:Deploy target with name \"CF-TEST\" does not exist"),
            },
// @formatter:on
        });
    }

    public TargetPlatformsResourceTest(String platformsJson, RequestInput input, RestResponse expected) {
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
    protected com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResource getResource() {
        return resource;
    }
}
