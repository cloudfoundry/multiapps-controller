package com.sap.cloud.lm.sl.cf.web.resources.v3;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.TargetPlatformsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v3_1.TargetPlatform;

@Component("targetPlatformsResourceV3")
@Path("/v3/platforms")
@Produces(MediaType.APPLICATION_XML)
public class TargetPlatformsResource extends com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResource {

    private static final URL PLATFORM_SCHEMA = TargetPlatformsResource.class.getResource("/target-platform-schema-v3.xsd");

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
    protected void validateRequestPayload(com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform platform) throws ParsingException {
        Map<String, Object> parameters = ((TargetPlatform) platform).getParameters();
        if (parameters == null || !isOrgSpaceSpecified(parameters)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_2);
        }
        validatePlatformIdentifiers(platform);
    }

    @Override
    protected TargetPlatformsDto getTargetPlatformsDto(List<com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform> platforms) {
        return new TargetPlatformsDto(ListUtil.cast(platforms));
    }

    @Override
    protected TargetPlatformDto getTargetPlatformDto(com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform platform) {
        return new TargetPlatformDto((TargetPlatform) platform);
    }

    @Override
    protected Class<TargetPlatformDto> getPlatformDtoClass() {
        return TargetPlatformDto.class;
    }

}
