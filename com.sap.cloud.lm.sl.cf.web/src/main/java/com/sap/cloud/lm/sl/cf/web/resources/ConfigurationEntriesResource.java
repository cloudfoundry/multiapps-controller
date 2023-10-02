package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static java.text.MessageFormat.format;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntriesDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationFilterDto;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaConfigurationPurger;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@Component
@Path("/configuration-entries")
@Produces(MediaType.APPLICATION_XML)
public class ConfigurationEntriesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesResource.class);

    private static final String KEYVALUE_SEPARATOR = ":";
    private static final String PURGE_COMMAND = "Purge configuration entries and subscriptions";

    private static final URL CONFIGURATION_FILTER_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource("/configuration-filter-schema.xsd");
    protected Supplier<UserInfo> userInfoSupplier = SecurityContextUtil::getUserInfo;

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    private SpaceGetter spaceGetter;

    @Inject
    private AuthorizationChecker authorizationChecker;

    @Inject
    private ApplicationConfiguration configuration;

    @Context
    private HttpServletRequest request;

    protected Response filterConfigurationEntries(ConfigurationFilter filter) {
        try {
            CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
            List<ConfigurationEntry> entries = findConfigurationEntries(entryDao, filter, getUserTargets(), globalConfigTarget);
            return Response.status(Response.Status.OK)
                           .entity(wrap(entries))
                           .build();
        } catch (IllegalArgumentException e) {
            /**
             * Thrown if the version parameter is not a valid version requirement.
             */
            throw new ParsingException(e.getMessage(), e);
        }
    }

    private List<CloudTarget> getUserTargets() {
        UserInfo userInfo = userInfoSupplier.get();
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName());
        return client.getSpaces()
                     .stream()
                     .map(this::getCloudTarget)
                     .collect(Collectors.toList());
    }

    private CloudTarget getCloudTarget(CloudSpace cloudSpace) {
        return new CloudTarget(cloudSpace.getOrganization()
                                         .getName(),
                               cloudSpace.getName());
    }

    private ConfigurationEntriesDto wrap(List<ConfigurationEntry> entries) {
        List<ConfigurationEntryDto> dtos = new ArrayList<>();
        for (ConfigurationEntry entry : entries) {
            dtos.add(new ConfigurationEntryDto(entry));
        }
        return new ConfigurationEntriesDto(dtos);
    }

    private Map<String, Object> parseContentFilterParameter(List<String> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        ParsingException contentJsonParsingException = null;
        ParsingException contentListParsingException = null;

        try {
            return parseContentQueryJsonParameter(content);
        } catch (ParsingException e) {
            contentJsonParsingException = e;
        }
        try {
            return parseContentQueryListParameter(content);
        } catch (ParsingException e) {
            contentListParsingException = e;
        }

        LOGGER.error(format(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER_AS_JSON, content), contentJsonParsingException);
        LOGGER.error(format(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER_AS_LIST, content), contentListParsingException);

        throw new ParsingException(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER);
    }

    private Map<String, Object> parseContentQueryJsonParameter(List<String> content) {
        return JsonUtil.fromJson(content.get(0), new TypeToken<Map<String, String>>() {
        }.getType());
    }

    private Map<String, Object> parseContentQueryListParameter(List<String> content) {
        Map<String, Object> parsedContent = new HashMap<>();
        for (String property : content) {
            String[] keyValuePair = property.split(KEYVALUE_SEPARATOR, 2);
            if (keyValuePair.length != 2) {
                throw new ParsingException(Messages.PROPERTY_DOES_NOT_CONTAIN_KEY_VALUE_PAIR, property);
            }
            parsedContent.put(keyValuePair[0], keyValuePair[1]);
        }
        return parsedContent;
    }

    private ConfigurationFilterDto parseFilterBean(String fXml) {
        return XmlUtil.fromXml(fXml, ConfigurationFilterDto.class, CONFIGURATION_FILTER_SCHEMA_LOCATION);
    }

    @Path("/searches")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response search(String xml) {
        return filterConfigurationEntries(asConfigurationFilter(parseFilterBean(xml)));
    }

    private ConfigurationFilter asConfigurationFilter(ConfigurationFilterDto bean) {
        String providerId = bean.getProviderId();
        String providerVersion = bean.getProviderVersion();
        String providerNid = bean.getProviderNid();
        Map<String, Object> content = parseContentFilterParameter(bean.getContent());
        CloudTarget target = bean.getCloudTarget() == null ? ConfigurationEntriesUtil.createImplicitCloudTarget(bean.getTargetSpace())
            : bean.getCloudTarget();
        return new ConfigurationFilter(providerNid, providerId, providerVersion, target, content);
    }

    @Path("/purg")
    @POST
    public Response purgeConfigurationRegistry(@QueryParam("org") String org, @QueryParam("space") String space) {
 
        return Response.status(Response.Status.NO_CONTENT)
                       .build();
    }
}
