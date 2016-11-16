package com.sap.cloud.lm.sl.cf.core.validators;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.TargetPlatformFactory;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.MtaSchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class MtaArchiveValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaArchiveValidator.class);

    private static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1 MB

    private final ConfigurationEntryDao dao;
    private final InputStream mtarStream;
    private final InputStream extensionDescriptorStream;
    private final InputStream platformTypesStream;
    private final InputStream platformsStream;
    private final String platformName;
    private final String deployId;
    private final String userName;
    private final String defaultDomain;
    private final PlatformType xsType;
    private final URL targetUrl;
    private final String authorizationEndpoint;
    private final String deployServiceUrl;
    private final int routerPort;
    private final int minPort;
    private final int maxPort;
    private final DeployedMta deployedMta;
    private final long maxMtaDescriptorSize;
    private final boolean xsPlaceholdersSupported;

    private String organization;
    private String space;
    private List<String> customDomains;
    private List<CloudServiceExtended> services;
    private List<CloudApplicationExtended> applications;

    protected BiFunction<String, String, String> spaceIdSupplier = (org, space) -> {
        return space;
    };

    public MtaArchiveValidator(InputStream mtarStream, InputStream extensionDescriptorStream, InputStream platformTypesStream,
        InputStream platformsStream, String platformName, String deployId, String userName, String defaultDomain, PlatformType xsType,
        URL targetUrl, String authorizationEndpoint, String deployServiceUrl, int routerPort, int minPort, int maxPort,
        DeployedMta deployedMta, ConfigurationEntryDao dao, boolean xsPlaceholdersSupported) {
        this(mtarStream, extensionDescriptorStream, platformTypesStream, platformsStream, platformName, deployId, userName, defaultDomain,
            xsType, targetUrl, authorizationEndpoint, deployServiceUrl, routerPort, minPort, maxPort, deployedMta,
            DEFAULT_MAX_MTA_DESCRIPTOR_SIZE, dao, xsPlaceholdersSupported);
    }

    public MtaArchiveValidator(InputStream mtarStream, InputStream extensionDescriptorStream, InputStream platformTypesStream,
        InputStream platformsStream, String platformName, String deployId, String userName, String defaultDomain, PlatformType xsType,
        URL targetUrl, String authorizationEndpoint, String deployServiceUrl, int routerPort, int minPort, int maxPort,
        DeployedMta deployedMta, long maxMtaDescriptorSize, ConfigurationEntryDao dao, boolean xsPlaceholdersSupported) {
        this.mtarStream = mtarStream;
        this.extensionDescriptorStream = extensionDescriptorStream;
        this.platformTypesStream = platformTypesStream;
        this.platformsStream = platformsStream;
        this.platformName = platformName;
        this.deployId = deployId;
        this.userName = userName;
        this.defaultDomain = defaultDomain;
        this.xsType = xsType;
        this.targetUrl = targetUrl;
        this.authorizationEndpoint = authorizationEndpoint;
        this.deployServiceUrl = deployServiceUrl;
        this.routerPort = routerPort;
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.deployedMta = deployedMta;
        this.maxMtaDescriptorSize = maxMtaDescriptorSize;
        this.dao = dao;
        this.xsPlaceholdersSupported = xsPlaceholdersSupported;
    }

    public String getOrganization() {
        return organization;
    }

    public String getSpace() {
        return space;
    }

    public List<String> getCustomDomains() {
        return customDomains;
    }

    public List<CloudServiceExtended> getServices() {
        return services;
    }

    public List<CloudApplicationExtended> getApplications() {
        return applications;
    }

    public void validate() throws SLException, IOException {
        // Get MTAR contents as byte array
        byte[] mtar = IOUtils.toByteArray(mtarStream);

        // Read the MTAR manifest
        Manifest manifest = ArchiveHandler.getManifest(new ByteArrayInputStream(mtar));

        // Create and initialize MTA archive helper
        MtaArchiveHelper mtaArchiveHelper = new MtaArchiveHelper(manifest);
        mtaArchiveHelper.init();

        // Get module sets
        Set<String> mtaArchiveModules = mtaArchiveHelper.getMtaArchiveModules().keySet();
        LOGGER.debug(format("MTA Archive Modules: {0}", mtaArchiveModules));
        Set<String> mtaModules = mtaArchiveHelper.getMtaModules();
        LOGGER.debug(format("MTA Modules: {0}", mtaModules));

        // Get descriptor strings
        String deploymentDescriptorString = ArchiveHandler.getDescriptor(new ByteArrayInputStream(mtar), maxMtaDescriptorSize);
        List<String> extensionDescriptorStrings = Collections.emptyList();
        if (extensionDescriptorStream != null) {
            extensionDescriptorStrings = Arrays.asList(IOUtils.toString(extensionDescriptorStream));
        }

        // Create handler factory
        Version schemaVersion = new MtaSchemaVersionDetector().detect(deploymentDescriptorString, extensionDescriptorStrings);
        HandlerFactory handlerFactory = new HandlerFactory(schemaVersion.getMajor(), schemaVersion.getMinor());

        // Parse configuration files
        ConfigurationParser configurationParser = handlerFactory.getConfigurationParser();
        List<TargetPlatformType> platformTypes = configurationParser.parsePlatformTypesJson(platformTypesStream);
        List<TargetPlatform> platforms = configurationParser.parsePlatformsJson(platformsStream);

        // Determine platform and platform type
        DescriptorHandler handler = handlerFactory.getDescriptorHandler();
        TargetPlatformFactory factory = handlerFactory.getTargetPlatformFactory();
        TargetPlatform implicitPlatform = factory.create(platformName, platformTypes.get(0).getName());
        TargetPlatform platform = CloudModelBuilderUtil.findPlatform(handler, platforms, platformName, implicitPlatform);
        LOGGER.debug(format("Platform: {0}", JsonUtil.toJson(platform, true)));
        TargetPlatformType platformType = CloudModelBuilderUtil.findPlatformType(handler, platformTypes, platform);
        LOGGER.debug(format("Platform type: {0}", JsonUtil.toJson(platformType, true)));

        // Get organization and space
        Pair<String, String> orgSpace = handlerFactory.getOrgAndSpaceHelper(platform, platformType).getOrgAndSpace();
        organization = orgSpace._1;
        space = orgSpace._2;
        LOGGER.debug(format("Organization: {0}, space: {1}", organization, space));

        boolean portBasedRouting = xsType.equals(PlatformType.XS2);

        // Create port allocator
        PortAllocator portAllocator = new PortAllocatorMock(minPort, maxPort);

        // Create system parameters builder
        SystemParametersBuilder parametersBuilder = new SystemParametersBuilder(platform.getName(), organization, space, userName,
            defaultDomain, xsType, targetUrl, authorizationEndpoint, deployServiceUrl, routerPort, portBasedRouting, true, portAllocator,
            Collections.emptyMap(), false, false, deployedMta, schemaVersion.getMajor(), xsPlaceholdersSupported);

        // Create and initialize cloud model helper
        MtaDescriptorMerger descriptorMerger = new MtaDescriptorMerger(handlerFactory, platformType, platform);
        DeploymentDescriptor deploymentDescriptor = descriptorMerger.merge(deploymentDescriptorString, extensionDescriptorStrings);
        SystemParameters systemParameters = parametersBuilder.build(deploymentDescriptor);

        MtaDescriptorPropertiesResolver descriptorProcessor = new MtaDescriptorPropertiesResolver(handlerFactory, platformType, platform,
            systemParameters, spaceIdSupplier, dao);
        deploymentDescriptor = descriptorProcessor.resolve(deploymentDescriptor);

        // Merge DeploymentDescriptor and TargetPlatform
        handlerFactory.getTargetPlatformMerger(platform).mergeInto(deploymentDescriptor);

        // Merge DeploymentDescriptor and TargetPlatformType
        handlerFactory.getTargetPlatformTypeMerger(platformType).mergeInto(deploymentDescriptor);

        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setRouterPort(routerPort);
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);

        // Get a cloud model builder from the helper
        CloudModelBuilder builder = handlerFactory.getCloudModelBuilder(deploymentDescriptor, systemParameters, portBasedRouting, true,
            false, false, false, deployId, xsPlaceholderResolver);

        // Build a list of custom domains and save them in the context
        customDomains = builder.getCustomDomains();
        LOGGER.debug(format("Custom domains: {0}", customDomains));

        // Build a list of cloud applications and save them in the context
        applications = builder.getApplications(mtaArchiveModules, mtaModules, Collections.emptySet());
        LOGGER.debug(format("Cloud applications: {0}", JsonUtil.toJson(applications, true)));

        // Build a list of cloud services and save them in the context
        services = builder.getServices(mtaArchiveModules);
        LOGGER.debug(format("Cloud services: {0}", JsonUtil.toJson(services, true)));
    }

}
