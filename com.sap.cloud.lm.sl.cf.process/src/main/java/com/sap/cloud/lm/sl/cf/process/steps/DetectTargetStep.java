package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;
import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("detectTargetStep")
public class DetectTargetStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectTargetStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("detectTargetTask").displayName("Detect Target").description("Detect Target").build();
    }

    protected Function<HandlerFactory, List<Platform>> platformsSupplier = (handlerFactory) -> ConfigurationUtil.getPlatforms(
        handlerFactory.getConfigurationParser(), handlerFactory.getMajorVersion());

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao deployTargetDaoV1;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao deployTargetDaoV2;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao deployTargetDaoV3;

    protected Function<HandlerFactory, List<Target>> targetsSupplier = (handlerFactory) -> {
        DeployTargetDao<?, ?> targetDao = handlerFactory.getDeployTargetDao(deployTargetDaoV1, deployTargetDaoV2, deployTargetDaoV3);
        List<PersistentObject<Target>> persistentTargets = cast(targetDao.findAll());
        return persistentTargets.stream().map((target) -> target.getObject()).collect(Collectors.toList());
    };

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.DETECTING_TARGET, LOGGER);
        try {
            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            List<Platform> platforms = platformsSupplier.apply(handlerFactory);
            debug(context, format(Messages.PLATFORM_TYPES, JsonUtil.toJson(platforms, true)), LOGGER);
            if (platforms == null || platforms.isEmpty()) {
                throw new NotFoundException(Messages.NO_PLATFORMS_CONFIGURED);
            }

            String targetName = (String) context.getVariable(Constants.PARAM_TARGET_NAME);
            if (targetName == null || targetName.isEmpty()) {
                targetName = computeDefaultTargetName(context);
                context.setVariable(Constants.PARAM_TARGET_NAME, targetName);
            }

            List<Target> targets = targetsSupplier.apply(handlerFactory);
            debug(context, format(Messages.PLATFORMS, new SecureSerializationFacade().toJson(targets)), LOGGER);

            Target implicitTarget = handlerFactory.getDeployTargetFactory().create(targetName, platforms.get(0).getName());
            DescriptorHandler descriptorHandler = handlerFactory.getDescriptorHandler();

            Target target = CloudModelBuilderUtil.findTarget(descriptorHandler, targets, targetName, implicitTarget);

            if (target == null) {
                throw new ContentException("Unknown target \"{0}\"", targetName);
            }
            Platform platform = CloudModelBuilderUtil.findPlatform(descriptorHandler, platforms, target);

            validateOrgAndSpace(context, target, platform, handlerFactory);

            StepsUtil.setPlatform(context, platform);
            StepsUtil.setTarget(context, target);

            info(context, format(Messages.TARGET_DETECTED, targetName), LOGGER);
        } catch (SLException e) {
            error(context, Messages.ERROR_DETECTING_TARGET, e, LOGGER);
            throw e;
        }
        return ExecutionStatus.SUCCESS;
    }

    private String computeDefaultTargetName(DelegateExecution context) {
        String space = (String) context.getVariable(Constants.VAR_SPACE);
        String org = (String) context.getVariable(Constants.VAR_ORG);
        return CloudModelBuilderUtil.buildImplicitDeployTargetName(org, space);
    }

    private void validateOrgAndSpace(DelegateExecution context, Target target, Platform platform, HandlerFactory handlerFactory)
        throws SLException {
        Pair<String, String> orgSpace = handlerFactory.getOrgAndSpaceHelper(target, platform).getOrgAndSpace();
        debug(context, format(Messages.ORG_SPACE, orgSpace._1, orgSpace._2), LOGGER);

        validateOrgAndSpace(context, orgSpace._1, orgSpace._2);
    }

    private void validateOrgAndSpace(DelegateExecution context, String org, String space) throws SLException {
        StepsUtil.validateSpace(space, context);
        StepsUtil.validateOrg(org, context);
    }

}
