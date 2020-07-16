package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;

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