package org.cloudfoundry.multiapps.controller.core.helpers;

import java.io.InputStream;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DescriptorParserFacadeFactoryTest {

    @Test
    void testGetInstance() {
        final int maxAliases = 5;
        ApplicationConfiguration applicationConfiguration = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.getSnakeyamlMaxAliasesForCollections())
               .thenReturn(maxAliases);
        DescriptorParserFacadeFactory descriptorParserFacadeFactory = new DescriptorParserFacadeFactory(applicationConfiguration);
        DescriptorParserFacade instance = descriptorParserFacadeFactory.getInstance();
        InputStream mtadYaml = getClass().getResourceAsStream("billion-laughs.mtad.yaml");
        Assertions.assertThrows(ParsingException.class, () -> instance.parseDeploymentDescriptor(mtadYaml));
    }

}