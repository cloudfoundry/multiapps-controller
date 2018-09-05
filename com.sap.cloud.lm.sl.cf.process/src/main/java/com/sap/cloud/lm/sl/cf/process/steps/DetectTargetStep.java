package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
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

@Component("detectTargetStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectTargetStep extends SyncActivitiStep {

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao deployTargetDaoV1;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao deployTargetDaoV2;

    @Inject
    private com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao deployTargetDaoV3;

    @Inject
    private ApplicationConfiguration configuration;

    protected Function<HandlerFactory, List<Target>> targetsSupplier = handlerFactory -> {
        DeployTargetDao<?, ?> targetDao = handlerFactory.getDeployTargetDao(deployTargetDaoV1, deployTargetDaoV2, deployTargetDaoV3);
        List<PersistentObject<Target>> persistentTargets = cast(targetDao.findAll());
        return persistentTargets.stream()
            .map(target -> target.getObject())
            .collect(Collectors.toList());
    };

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_TARGET);
        try {
            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());

            List<Platform> platforms = configuration.getPlatforms(handlerFactory.getConfigurationParser(),
                handlerFactory.getMajorVersion());
            getStepLogger().debug(Messages.PLATFORM_TYPES, JsonUtil.toJson(platforms, true));
            if (platforms == null || platforms.isEmpty()) {
                throw new NotFoundException(Messages.NO_PLATFORMS_CONFIGURED);
            }

            String targetName = (String) execution.getContext()
                .getVariable(Constants.PARAM_TARGET_NAME);
            if (targetName == null || targetName.isEmpty()) {
                targetName = computeDefaultTargetName(execution.getContext());
                execution.getContext()
                    .setVariable(Constants.PARAM_TARGET_NAME, targetName);
            }

            String space = (String) execution.getContext()
                .getVariable(Constants.VAR_SPACE);
            String org = (String) execution.getContext()
                .getVariable(Constants.VAR_ORG);

            List<Target> targets = targetsSupplier.apply(handlerFactory);
            getStepLogger().debug(Messages.PLATFORMS, new SecureSerializationFacade().toJson(targets));

            Target implicitTarget = handlerFactory.getDeployTargetFactory()
                .create(org, space, platforms.get(0)
                    .getName());
            DescriptorHandler descriptorHandler = handlerFactory.getDescriptorHandler();

            Target target = CloudModelBuilderUtil.findTarget(descriptorHandler, targets, targetName, implicitTarget);

            if (target == null) {
                throw new ContentException("Unknown target \"{0}\"", targetName);
            }
            Platform platform = CloudModelBuilderUtil.findPlatform(descriptorHandler, platforms, target);

            validateOrgAndSpace(execution.getContext(), target, platform, handlerFactory);

            StepsUtil.setPlatform(execution.getContext(), platform);
            StepsUtil.setTarget(execution.getContext(), target);

            getStepLogger().info(Messages.TARGET_DETECTED, targetName);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_TARGET);
            throw e;
        }
        return StepPhase.DONE;
    }

    private String computeDefaultTargetName(DelegateExecution context) {
        String space = (String) context.getVariable(Constants.VAR_SPACE);
        String org = (String) context.getVariable(Constants.VAR_ORG);
        return CloudModelBuilderUtil.buildImplicitDeployTargetName(org, space);
    }

    private void validateOrgAndSpace(DelegateExecution context, Target target, Platform platform, HandlerFactory handlerFactory) {
        Pair<String, String> orgSpace = handlerFactory.getOrgAndSpaceHelper(target, platform)
            .getOrgAndSpace();
        getStepLogger().debug(Messages.ORG_SPACE, orgSpace._1, orgSpace._2);

        validateOrgAndSpace(context, orgSpace._1, orgSpace._2);
    }

    private void validateOrgAndSpace(DelegateExecution context, String org, String space) {
        StepsUtil.validateSpace(space, context);
        StepsUtil.validateOrg(org, context);
    }

}
