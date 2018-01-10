package com.sap.cloud.lm.sl.cf.core.validators;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(value = Parameterized.class)
public class MtaArchiveValidatorTest {

    private static final String DEPLOY_ID = "deploy-1";
    private static final String PLATFORM_TYPES = "/mta/platform-types-v2.json";
    private static final String PLATFORMS = "/mta/targets-v2.json";

    private static final int MIN_PORT = 55000;
    private static final int MAX_PORT = 55999;

    private static final long MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1 MB

    private static final String XS2_PLATFORM_NAME = "XS2-INITIAL";
    private static final String XS2_USERNAME = "bootstrap";
    private static final String XS2_DEFAULT_DOMAIN = "sofd60245639a";
    private static final String XS2_TARGET_URL = "http://localhost:9999";
    private static final String XS2_AUTHORIZATION_ENDPOINT = "http://localhost:9998";
    private static final String XS2_TARGET_URL_HTTPS = "https://localhost:9999";
    private static final String XS2_AUTHORIZATION_ENDPOINT_HTTPS = "https://localhost:9998";

    private static final String CF_PLATFORM_NAME = "CF-TRIAL";
    private static final String CF_USERNAME = "i027947";
    private static final String CF_DEFAULT_DOMAIN = "cfapps.neo.ondemand.com";
    private static final String CF_TARGET_URL = "https://api.cf.neo.ondemand.com";
    private static final String CF_AUTHORIZATION_ENDPOINT = "https://api.cf.neo.ondemand.com/uaa";

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (00)
            { "simple.mtar",                 null,                          XS2_PLATFORM_NAME, XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL,       XS2_AUTHORIZATION_ENDPOINT,       "",80,  "simple-apps.json",                 "simple-services.json",                 null,                MAX_MTA_DESCRIPTOR_SIZE, true , },
            // (01)
            { "placeholders.mtar",           "placeholders-op.mtaext",      "initial initial", XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL_HTTPS, XS2_AUTHORIZATION_ENDPOINT_HTTPS, "",443, "placeholders-apps.json",           "placeholders-services.json",           null,                MAX_MTA_DESCRIPTOR_SIZE, true , },
            // (02)
            { "placeholders.mtar",           "placeholders-cf.mtaext",      CF_PLATFORM_NAME,  CF_USERNAME,  CF_DEFAULT_DOMAIN,  PlatformType.CF,  CF_TARGET_URL,        CF_AUTHORIZATION_ENDPOINT,        "",443, "placeholders-apps2.json",          "placeholders-services.json",           null,                MAX_MTA_DESCRIPTOR_SIZE, false, },
            // (03)
            { "xsjs-hello-world.mtar",       "xsjs-hello-world-op.mtaext",  XS2_PLATFORM_NAME, XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL,       XS2_AUTHORIZATION_ENDPOINT,       "",80,  "xsjs-hello-world-apps.json",       "xsjs-hello-world-services.json",       null,                MAX_MTA_DESCRIPTOR_SIZE, true , },
            // (04)
            { "xsjs-hello-world.mtar",       "xsjs-hello-world-cf.mtaext", CF_PLATFORM_NAME,  CF_USERNAME,  CF_DEFAULT_DOMAIN,  PlatformType.CF,  CF_TARGET_URL,        CF_AUTHORIZATION_ENDPOINT,        "",443, "xsjs-hello-world-apps2.json",      "xsjs-hello-world-services.json",       null,                MAX_MTA_DESCRIPTOR_SIZE, false, },
            // (05)
            { "xsjs-hello-world.mtar",       "xsjs-hello-world-op2.mtaext", XS2_PLATFORM_NAME, XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL,       XS2_AUTHORIZATION_ENDPOINT,       "",80,  "xsjs-hello-world-apps3.json",      "xsjs-hello-world-services.json",       null,                MAX_MTA_DESCRIPTOR_SIZE, true , },
            // (06)
            { "cross-mta-dependencies.mtar", null,                          XS2_PLATFORM_NAME, XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL,       XS2_AUTHORIZATION_ENDPOINT,       "",80,  "cross-mta-dependencies-apps.json", "cross-mta-dependencies-services.json", "deployed-mta.json", MAX_MTA_DESCRIPTOR_SIZE, true , },
            // (07)
            { "placeholders.mtar",           "placeholders-op.mtaext",      "initial initial", XS2_USERNAME, XS2_DEFAULT_DOMAIN, PlatformType.XS2, XS2_TARGET_URL_HTTPS, XS2_AUTHORIZATION_ENDPOINT_HTTPS, "",443, "placeholders-apps3.json",          "placeholders-services.json",           null,                MAX_MTA_DESCRIPTOR_SIZE, false, },
            // @formatter:on
        });
    }

    private final String mtar;
    private final String extDescriptor;
    private final String platformName;
    private final String userName;
    private final String defaultDomain;
    private final PlatformType platformType;
    private final String targetUrl;
    private final String authorizationEndpoint;
    private final String deployServiceUrl;
    private final int routerPort;
    private final String appsJson;
    private final String servicesJson;
    private final String deployedMtaJson;
    private final long maxMtaDescriptorSize;
    private final boolean xsPlaceholdersSupported;

    private ConfigurationEntryDao dao;
    private MtaArchiveValidator validator;

    public MtaArchiveValidatorTest(String mtar, String extDescriptor, String platformName, String userName, String defaultDomain,
        PlatformType platformType, String targetUrl, String authorizationEndpoint, String deployServiceUrl, int routerPort, String appsJson,
        String servicesJson, String deployedMtasJson, long maxMtaDescriptorSize, boolean xsPlaceholdersSupported) {
        this.mtar = mtar;
        this.extDescriptor = extDescriptor;
        this.platformName = platformName;
        this.userName = userName;
        this.defaultDomain = defaultDomain;
        this.platformType = platformType;
        this.targetUrl = targetUrl;
        this.authorizationEndpoint = authorizationEndpoint;
        this.deployServiceUrl = deployServiceUrl;
        this.routerPort = routerPort;
        this.appsJson = appsJson;
        this.servicesJson = servicesJson;
        this.deployedMtaJson = deployedMtasJson;
        this.maxMtaDescriptorSize = maxMtaDescriptorSize;
        this.xsPlaceholdersSupported = xsPlaceholdersSupported;
    }

    @Before
    public void setUp() throws Exception {
        Class<? extends MtaArchiveValidatorTest> clazz = getClass();
        InputStream extDescriptorStream = (extDescriptor != null) ? clazz.getResourceAsStream(extDescriptor) : null;
        DeployedMta deployedMta = deployedMtaJson == null ? null
            : JsonUtil.fromJson(TestUtil.getResourceAsString(deployedMtaJson, getClass()), DeployedMta.class);
        prepareConfigurationDao();
        validator = new MtaArchiveValidator(clazz.getResourceAsStream(mtar), extDescriptorStream, clazz.getResourceAsStream(PLATFORM_TYPES),
            clazz.getResourceAsStream(PLATFORMS), platformName, DEPLOY_ID, userName, defaultDomain, platformType,
            MiscUtil.getURL(targetUrl), authorizationEndpoint, deployServiceUrl, routerPort, MIN_PORT, MAX_PORT, deployedMta,
            maxMtaDescriptorSize, dao, xsPlaceholdersSupported);
    }

    private void prepareConfigurationDao() {
        dao = Mockito.mock(ConfigurationEntryDao.class);
        Mockito.when(dao.find("mta", "mta-sample:provides-dependency", "0.0.1", new CloudTarget("initial", "initial"), null, null, null)).thenReturn(
            Arrays.asList(new ConfigurationEntry(0, "mta", "mta-sample:provides-dependency",
                com.sap.cloud.lm.sl.mta.model.Version.parseVersion("0.0.1"), null, "{\"baz\":\"baz\",\"bar\":\"bar\", \"foo\":\"foo\"}",
                null, "")));
    }

    @Test
    public void testValidate() throws Exception {
        validator.validate();
        assertEquals(Collections.emptyList(), validator.getCustomDomains());
        assertEquals(getResourceAsString(appsJson), JsonUtil.toJson(validator.getApplications(), true));
        assertEquals(getResourceAsString(servicesJson), JsonUtil.toJson(validator.getServices(), true));
    }

    public static String getResourceAsString(String name) throws IOException {
        return IOUtils.toString(MtaArchiveValidatorTest.class.getResourceAsStream(name)).replace("\r", "");
    }
}
