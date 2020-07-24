package org.cloudfoundry.multiapps.controller.core.helpers;

import javax.inject.Inject;
import javax.inject.Named;

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
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(applicationConfiguration.getSnakeyamlMaxAliasesForCollections());
        YamlParser yamlParser = new YamlParser(loaderOptions);
        return new DescriptorParserFacade(yamlParser);
    }
}
