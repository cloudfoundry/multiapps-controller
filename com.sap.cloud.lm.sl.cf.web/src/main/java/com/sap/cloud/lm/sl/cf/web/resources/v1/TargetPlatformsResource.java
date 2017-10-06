package com.sap.cloud.lm.sl.cf.web.resources.v1;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.TargetPlatformsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@Component("targetPlatformsResourceV1")
@Path("/platforms")
@Produces(MediaType.APPLICATION_XML)
@Deprecated
public class TargetPlatformsResource
    extends com.sap.cloud.lm.sl.cf.web.resources.TargetPlatformsResource<Target, DeployTargetDto, DeployTargetDao> {

    private static final URL TARGET_SCHEMA = TargetPlatformsResource.class.getResource("/target-platform-schema-v1.xsd");

    @Inject
    private DeployTargetDao dao;

    public TargetPlatformsResource() {
        super(TARGET_SCHEMA, false);
    }

    protected TargetPlatformsResource(URL schemaLocation, boolean disableAuthorization) {
        super(schemaLocation, disableAuthorization);
    }

    @Override
    protected com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, DeployTargetDto> getDao() {
        return dao;
    }

    @Override
    protected void validateRequestPayload(Target target) throws ParsingException {
        Map<String, Object> properties = target.getProperties();
        if (properties == null || !isOrgSpaceSpecified(properties)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_1);
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
