package com.sap.cloud.lm.sl.cf.core.helpers;

import java.io.InputStream;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class DescriptorParserFacadeFactoryTest {

    @Test
    public void testGetInstance() {
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