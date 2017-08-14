package com.sap.cloud.lm.sl.cf.web.resources.v3;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.v3.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.TargetPlatformsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v3_1.Target;

@Component("targetPlatformsResourceV3")
@Path("/v3/platforms")
@Produces(MediaType.APPLICATION_XML)
@Deprecated
public class TargetPlatformsResource
    extends com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResource<Target, DeployTargetDto, DeployTargetDao> {

    private static final URL PLATFORM_SCHEMA = TargetPlatformsResource.class.getResource("/target-platform-schema-v3.xsd");

    @Inject
    private DeployTargetDao dao;

    public TargetPlatformsResource() {
        super(PLATFORM_SCHEMA, false);
    }

    protected TargetPlatformsResource(URL schemaLocation, boolean disableAuthorization) {
        super(schemaLocation, disableAuthorization);
    }

    @Override
    protected DeployTargetDao getDao() {
        return dao;
    }

    @Override
    protected void validateRequestPayload(Target target) throws ParsingException {
        Map<String, Object> parameters = (target).getParameters();
        if (parameters == null || !isOrgSpaceSpecified(parameters)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_2);
        }
        validatePlatformIdentifiers(target);
    }

    @Override
    protected TargetPlatformsDto getTargetPlatformsDto(List<Target> targets) {
        return new TargetPlatformsDto(targets);
    }

    @Override
    protected TargetPlatformDto getTargetPlatformDto(Target target) {
        return new TargetPlatformDto(target);
    }

    @Override
    protected Class<TargetPlatformDto> getTargetPlatformDtoClass() {
        return TargetPlatformDto.class;
    }

}
