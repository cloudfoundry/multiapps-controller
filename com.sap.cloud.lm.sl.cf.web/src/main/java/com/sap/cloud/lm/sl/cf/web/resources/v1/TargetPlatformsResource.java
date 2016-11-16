package com.sap.cloud.lm.sl.cf.web.resources.v1;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.TargetPlatformsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;

@Component("targetPlatformsResourceV1")
@Path("/platforms")
@Produces(MediaType.APPLICATION_XML)
public class TargetPlatformsResource extends com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResource {

    private static final URL PLATFORM_SCHEMA = TargetPlatformsResource.class.getResource("/target-platform-schema-v1.xsd");

    @Inject
    private TargetPlatformDao dao;

    public TargetPlatformsResource() {
        super(PLATFORM_SCHEMA, false);
    }

    protected TargetPlatformsResource(URL schemaLocation, boolean disableAuthorization) {
        super(schemaLocation, disableAuthorization);
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao getDao() {
        return dao;
    }

    @Override
    protected void validateRequestPayload(TargetPlatform platform) throws ParsingException {
        Map<String, Object> properties = platform.getProperties();
        if (properties == null || !isOrgSpaceSpecified(properties)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_1);
        }
        validatePlatformIdentifiers(platform);
    }

    @Override
    protected TargetPlatformsDto getTargetPlatformsDto(List<TargetPlatform> platforms) {
        return new TargetPlatformsDto(platforms);
    }

    @Override
    protected TargetPlatformDto getTargetPlatformDto(TargetPlatform platform) {
        return new TargetPlatformDto(platform);
    }

    @Override
    protected Class<TargetPlatformDto> getPlatformDtoClass() {
        return TargetPlatformDto.class;
    }

}
