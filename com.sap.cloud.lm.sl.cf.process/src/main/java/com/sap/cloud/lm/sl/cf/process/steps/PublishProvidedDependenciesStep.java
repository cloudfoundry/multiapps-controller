package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("publishProvidedDependenciesStep")
public class PublishProvidedDependenciesStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishProvidedDependenciesStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    ConfigurationEntryDao configurationEntryDao;

    public static StepMetadata getMetadata() {
        return new StepMetadata("publishProvidedDependenciesTask", "Publish Provided Dependencies", "Publish Provided Dependencies");
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, LOGGER);

            String org = StepsUtil.getOrg(context);
            String space = StepsUtil.getSpace(context);
            String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);
            String targetSpace = ConfigurationEntriesUtil.computeTargetSpace(new Pair<>(org, space));

            List<ProvidedDependency> dependenciesToPublish = StepsUtil.getDependenciesToPublish(context);
            String mtaVersion = StepsUtil.getNewMtaVersion(context);

            List<ConfigurationEntry> publishedEntries = publish(dependenciesToPublish, mtaId, targetSpace, mtaVersion);
            debug(context, format(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries)), LOGGER);
            StepsUtil.setPublishedEntries(context, publishedEntries);

            debug(context, Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, e, LOGGER);
            throw e;
        }
    }

    private List<ConfigurationEntry> publish(List<ProvidedDependency> dependenciesToPublish, String mtaId, String targetSpace,
        String mtaVersion) throws SLException {
        List<ConfigurationEntry> publishedEntries = new ArrayList<>();
        for (ProvidedDependency dependency : dependenciesToPublish) {
            ConfigurationEntry configurationEntry = createConfigurationEntry(mtaId, targetSpace, mtaVersion, dependency);
            ConfigurationEntry publishedConfigurationEntry = publishConfigurationEntry(configurationEntry);
            publishedEntries.add(publishedConfigurationEntry);
        }
        return publishedEntries;
    }

    private ConfigurationEntry publishConfigurationEntry(ConfigurationEntry entry) throws SLException {
        ConfigurationEntry currentEntry = getExistingEntry(entry);
        if (currentEntry == null) {
            return configurationEntryDao.add(entry);
        } else {
            return configurationEntryDao.update(currentEntry.getId(), entry);
        }
    }

    private ConfigurationEntry getExistingEntry(ConfigurationEntry targetEntry) {
        List<ConfigurationEntry> existingEntries = configurationEntryDao.find(targetEntry.getProviderNid(), targetEntry.getProviderId(),
            targetEntry.getProviderVersion().toString(), targetEntry.getTargetSpace(), Collections.emptyMap(), null);
        return existingEntries.isEmpty() ? null : existingEntries.get(0);
    }

    protected ConfigurationEntry createConfigurationEntry(String mtaId, String target, String version, ProvidedDependency dependency) {
        return new ConfigurationEntry(ConfigurationEntriesUtil.PROVIDER_NID,
            ConfigurationEntriesUtil.computeProviderId(mtaId, dependency.getName()), Version.parseVersion(version), target,
            JsonUtil.toJson(dependency.getProperties()));
    }

}
