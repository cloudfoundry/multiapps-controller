package org.cloudfoundry.multiapps.controller.core.helpers;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.yaml.snakeyaml.LoaderOptions;

@Named
public class DescriptorParserFacadeFactory {

    private ApplicationConfiguration applicationConfiguration;

    @Inject
    public DescriptorParserFacadeFactory(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    public DescriptorParserFacade getInstance() {
        LoaderOptions loaderOptions = createLoaderOptions(true);
        YamlParser yamlParser = new YamlParser(loaderOptions);
        return new DescriptorParserFacade(yamlParser);
    }

    public DescriptorParserFacade getInstanceWithDisabledDuplicateKeys() {
        LoaderOptions loaderOptions = createLoaderOptions(false);
        YamlParser yamlParser = new YamlParser(loaderOptions);
        return new DescriptorParserFacade(yamlParser);
    }

    private LoaderOptions createLoaderOptions(boolean shouldAllowDuplicateKeys) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(applicationConfiguration.getSnakeyamlMaxAliasesForCollections());
        loaderOptions.setAllowDuplicateKeys(shouldAllowDuplicateKeys);
        return loaderOptions;
    }
}
