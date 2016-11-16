package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("detectPlatformStep")
public class DetectPlatformStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectPlatformStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("detectPlatformTask", "Detect Platform", "Detect Platform");
    }

    protected Function<HandlerFactory, List<TargetPlatformType>> platformTypesSupplier = (
        handlerFactory) -> ConfigurationUtil.getPlatformTypes(handlerFactory.getConfigurationParser(), handlerFactory.getMajorVersion());

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao targetPlatformDaoV1;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao targetPlatformDaoV2;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao targetPlatformDaoV3;

    protected Function<HandlerFactory, List<TargetPlatform>> platformsSupplier = (
        handlerFactory) -> handlerFactory.getTargetPlatformDao(targetPlatformDaoV1, targetPlatformDaoV2, targetPlatformDaoV3).findAll();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.DETECTING_PLATFORM, LOGGER);
        try {
            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            List<TargetPlatformType> platformTypes = platformTypesSupplier.apply(handlerFactory);
            debug(context, format(Messages.PLATFORM_TYPES, JsonUtil.toJson(platformTypes, true)), LOGGER);
            if (platformTypes == null || platformTypes.isEmpty()) {
                throw new NotFoundException(Messages.NO_TARGET_PLATFORM_TYPES_CONFIGURED);
            }

            String platformName = (String) context.getVariable(Constants.PARAM_PLATFORM_NAME);
            if (platformName == null || platformName.isEmpty()) {
                platformName = computeDefaultPlatformName(context);
                context.setVariable(Constants.PARAM_PLATFORM_NAME, platformName);
            }

            List<TargetPlatform> platforms = platformsSupplier.apply(handlerFactory);
            debug(context, format(Messages.PLATFORMS, new SecureSerializationFacade().toJson(platforms)), LOGGER);

            TargetPlatform implicitPlatform = handlerFactory.getTargetPlatformFactory().create(platformName,
                platformTypes.get(0).getName());

            DescriptorHandler handler = handlerFactory.getDescriptorHandler();
            TargetPlatform platform = CloudModelBuilderUtil.findPlatform(handler, platforms, platformName, implicitPlatform);
            TargetPlatformType platformType = CloudModelBuilderUtil.findPlatformType(handler, platformTypes, platform);

            validateOrgAndSpace(context, platform, platformType, handlerFactory);

            StepsUtil.setPlatformType(context, platformType);
            StepsUtil.setPlatform(context, platform);

            info(context, format(Messages.PLATFORM_DETECTED, platformName), LOGGER);
        } catch (SLException e) {
            error(context, Messages.ERROR_DETECTING_PLATFORM, e, LOGGER);
            throw e;
        }
        return ExecutionStatus.SUCCESS;
    }

    private String computeDefaultPlatformName(DelegateExecution context) {
        String space = (String) context.getVariable(Constants.VAR_SPACE);
        String org = (String) context.getVariable(Constants.VAR_ORG);
        return CloudModelBuilderUtil.buildImplicitTargetPlatformName(org, space);
    }

    private void validateOrgAndSpace(DelegateExecution context, TargetPlatform platform, TargetPlatformType platformType,
        HandlerFactory handlerFactory) throws SLException {
        Pair<String, String> orgSpace = handlerFactory.getOrgAndSpaceHelper(platform, platformType).getOrgAndSpace();
        debug(context, format(Messages.ORG_SPACE, orgSpace._1, orgSpace._2), LOGGER);

        validateOrgAndSpace(context, orgSpace._1, orgSpace._2);
    }

    private void validateOrgAndSpace(DelegateExecution context, String org, String space) throws SLException {
        StepsUtil.validateSpace(space, context);
        StepsUtil.validateOrg(org, context);
    }

}
