package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.resolvers.v3.DynamicParametersResolver;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.helpers.VisitableObject;

@Named
public class ConfigurationEntryDynamicParameterResolver {

    public List<ConfigurationEntry>
           resolveDynamicParametersOfConfigurationEntries(List<ConfigurationEntry> entriesToPublish,
                                                          Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        return entriesToPublish.stream()
                               .map(entry -> resolveDynamicParametersOfConfigurationEntry(entry, dynamicResolvableParameters))
                               .collect(Collectors.toList());
    }

    private ConfigurationEntry resolveDynamicParametersOfConfigurationEntry(ConfigurationEntry entry,
                                                                            Set<DynamicResolvableParameter> dynamicResolvableParameters) {

        DynamicParametersResolver resolver = new DynamicParametersResolver(entry.getConfigurationName(),
                                                                           new DynamicResolvableParametersHelper(dynamicResolvableParameters));
        Map<String, Object> map = JsonUtil.convertJsonToMap(entry.getContent());
        Map<String, Object> resolvedServiceParameters = MiscUtil.cast(new VisitableObject(map).accept(resolver));

        if (!resolvedServiceParameters.isEmpty()) {
            return ConfigurationEntriesUtil.setContent(entry, JsonUtil.toJson(resolvedServiceParameters));
        }
        return entry;

    }

}
