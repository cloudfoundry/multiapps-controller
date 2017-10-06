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
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorMerger;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorMock;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.DeployTargetFactory;
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
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

//used by DevX
public class MtaArchiveValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaArchiveValidator.class);

    private static final long DEFAULT_MAX_MTA_DESCRIPTOR_SIZE = 1024 * 1024l; // 1 MB

    private final ConfigurationEntryDao dao;
    private final InputStream mtarStream;
    private final InputStream extensionDescriptorStream;
    private final InputStream platformsStream;
    private final InputStream targetsStream;
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
        this.platformsStream = platformTypesStream;
        this.targetsStream = platformsStream;
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
        List<Platform> platforms = configurationParser.parsePlatformsJson(platformsStream);
        List<Target> targets = configurationParser.parseTargetsJson(targetsStream);

        // Determine target and platform
        DescriptorHandler handler = handlerFactory.getDescriptorHandler();
        DeployTargetFactory factory = handlerFactory.getDeployTargetFactory();
        Target implicitTarget = factory.create(platformName, platforms.get(0).getName());
        Target target = CloudModelBuilderUtil.findTarget(handler, targets, platformName, implicitTarget);
        LOGGER.debug(format("Target: {0}", JsonUtil.toJson(target, true)));
        Platform platform = CloudModelBuilderUtil.findPlatform(handler, platforms, target);
        LOGGER.debug(format("Platform: {0}", JsonUtil.toJson(platform, true)));

        // Get organization and space
        Pair<String, String> orgSpace = handlerFactory.getOrgAndSpaceHelper(target, platform).getOrgAndSpace();
        organization = orgSpace._1;
        space = orgSpace._2;
        LOGGER.debug(format("Organization: {0}, space: {1}", organization, space));

        boolean portBasedRouting = xsType.equals(PlatformType.XS2);

        // Create port allocator
        PortAllocator portAllocator = new PortAllocatorMock(minPort, maxPort);

        // Create system parameters builder
        SystemParametersBuilder parametersBuilder = new SystemParametersBuilder(target.getName(), organization, space, userName,
            defaultDomain, xsType, targetUrl, authorizationEndpoint, deployServiceUrl, routerPort, portBasedRouting, true, portAllocator,
            false, false, deployedMta, schemaVersion.getMajor(), xsPlaceholdersSupported, null, () -> "0");

        // Create and initialize cloud model helper
        MtaDescriptorMerger descriptorMerger = new MtaDescriptorMerger(handlerFactory, platform, target);
        DeploymentDescriptor deploymentDescriptor = descriptorMerger.merge(deploymentDescriptorString, extensionDescriptorStrings);
        SystemParameters systemParameters = parametersBuilder.build(deploymentDescriptor);

        MtaDescriptorPropertiesResolver descriptorProcessor = new MtaDescriptorPropertiesResolver(handlerFactory, platform, target,
            systemParameters, spaceIdSupplier, dao, null);
        deploymentDescriptor = descriptorProcessor.resolve(deploymentDescriptor);

        // Merge DeploymentDescriptor and Target
        handlerFactory.getTargetMerger(target).mergeInto(deploymentDescriptor);

        // Merge DeploymentDescriptor and Platform
        handlerFactory.getPlatformMerger(platform).mergeInto(deploymentDescriptor);

        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setRouterPort(routerPort);
        xsPlaceholderResolver.setDefaultDomain(defaultDomain);

        // Get a cloud model builder from the helper
        CloudModelConfiguration config = new CloudModelConfiguration();
        config.setAllowInvalidEnvNames(false);
        config.setPortBasedRouting(portBasedRouting);
        config.setPrettyPrinting(true);
        config.setUseNamespaces(false);
        config.setUseNamespacesForServices(false);

        // Build a list of custom domains and save them in the context
        customDomains = handlerFactory.getDomainsCloudModelBuilder(systemParameters, xsPlaceholderResolver, deploymentDescriptor).build();
        LOGGER.debug(format("Custom domains: {0}", customDomains));

        // Build a list of cloud applications and save them in the context
        applications = handlerFactory.getApplicationsCloudModelBuilder(deploymentDescriptor, config, deployedMta, systemParameters,
            xsPlaceholderResolver, deployId).build(mtaArchiveModules, mtaModules, Collections.emptySet());
        LOGGER.debug(format("Cloud applications: {0}", JsonUtil.toJson(applications, true)));

        // Build a list of cloud services and save them in the context
        services = handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, handlerFactory.getPropertiesAccessor(), config).build(
            mtaArchiveModules);
        LOGGER.debug(format("Cloud services: {0}", JsonUtil.toJson(services, true)));
    }

}
