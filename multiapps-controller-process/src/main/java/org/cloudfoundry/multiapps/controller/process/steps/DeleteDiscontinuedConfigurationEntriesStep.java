package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("deleteDiscontinuedConfigurationEntriesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteDiscontinuedConfigurationEntriesStep extends SyncFlowableStep {

    @Inject
    private ConfigurationEntryService configurationEntryService;

    @Inject
    private FlowableFacade flowableFacade;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DELETING_PUBLISHED_DEPENDENCIES);

        List<ConfigurationEntry> entriesToDelete = getEntriesToDelete(context);
        deleteConfigurationEntries(entriesToDelete, context);

        getStepLogger().debug(Messages.PUBLISHED_DEPENDENCIES_DELETED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_PUBLISHED_DEPENDENCIES;
    }

    private List<ConfigurationEntry> getEntriesToDelete(ProcessContext context) {
        List<ConfigurationEntry> publishedEntries = StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacade);

        List<ConfigurationEntry> allEntriesForCurrentMta = getEntries(context);

        List<Long> publishedEntryIds = getEntryIds(publishedEntries);

        return allEntriesForCurrentMta.stream()
                                      .filter(entry -> !publishedEntryIds.contains(entry.getId()))
                                      .collect(Collectors.toList());
    }

    private void deleteConfigurationEntries(List<ConfigurationEntry> entriesToDelete, ProcessContext context) {        
        for (ConfigurationEntry entry : entriesToDelete) {
            getStepLogger().info(MessageFormat.format(Messages.DELETING_DISCONTINUED_DEPENDENCY_0, entry.getProviderId()));
            int deletedEntries = configurationEntryService.createQuery()
                                                          .id(entry.getId())
                                                          .delete();
            if (deletedEntries == 0) {
                getStepLogger().warn(Messages.COULD_NOT_DELETE_PROVIDED_DEPENDENCY, entry.getProviderId());
            }
        }
        getStepLogger().debug(Messages.DELETED_ENTRIES, SecureSerialization.toJson(entriesToDelete));
        context.setVariable(Variables.DELETED_ENTRIES, entriesToDelete);
    }

    private List<ConfigurationEntry> getEntries(ProcessContext context) {
        String mtaId = context.getVariable(Variables.MTA_ID);
        String spaceGuid =  context.getVariable(Variables.SPACE_GUID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        return configurationEntryService.createQuery()
                                        .providerNid(ConfigurationEntriesUtil.PROVIDER_NID)
                                        .spaceId(spaceGuid)
                                        .mtaId(mtaId)
                                        .providerNamespace(namespace, true)
                                        .list();
    }

    private List<Long> getEntryIds(List<ConfigurationEntry> configurationEntries) {
        return configurationEntries.stream()
                                   .map(ConfigurationEntry::getId)
                                   .collect(Collectors.toList());
    }

}
