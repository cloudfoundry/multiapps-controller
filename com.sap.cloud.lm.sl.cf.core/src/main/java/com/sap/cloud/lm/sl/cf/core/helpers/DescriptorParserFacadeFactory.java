package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.YamlParser;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import org.yaml.snakeyaml.LoaderOptions;

import javax.inject.Inject;
import javax.inject.Named;

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
