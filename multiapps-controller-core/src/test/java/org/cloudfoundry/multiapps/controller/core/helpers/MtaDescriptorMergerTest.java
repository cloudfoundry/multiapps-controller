package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v2.DescriptorParametersCompatibilityValidator;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorMerger;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorValidator;
import org.cloudfoundry.multiapps.mta.mergers.PlatformMerger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class MtaDescriptorMergerTest {

    @Mock
    private CloudHandlerFactory handlerFactory;
    @Mock
    private Platform platform;
    @Mock
    private UserMessageLogger userMessageLogger;
    @Mock
    private DescriptorValidator validator;
    @Mock
    private DescriptorMerger descriptorMerger;
    @Mock
    private PlatformMerger platformMerger;
    @Mock
    private DescriptorParametersCompatibilityValidator compatibilityValidator;

    private DeploymentDescriptor inputDescriptor;
    private DeploymentDescriptor mergedDescriptor;
    private DeploymentDescriptor compatibleDescriptor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        inputDescriptor = DeploymentDescriptor.createV3();
        mergedDescriptor = DeploymentDescriptor.createV3();
        compatibleDescriptor = DeploymentDescriptor.createV3();

        Mockito.when(handlerFactory.getDescriptorValidator())
               .thenReturn(validator);
        Mockito.when(handlerFactory.getDescriptorMerger())
               .thenReturn(descriptorMerger);
        Mockito.when(handlerFactory.getPlatformMerger(platform))
               .thenReturn(platformMerger);
    }

    @Test
    void testMergeRunsValidatorsMergerAndPlatformMerger() {
        List<ExtensionDescriptor> extensionDescriptors = List.of();
        Mockito.when(descriptorMerger.merge(inputDescriptor, extensionDescriptors))
               .thenReturn(mergedDescriptor);
        Mockito.when(handlerFactory.getDescriptorParametersCompatibilityValidator(mergedDescriptor, null))
               .thenReturn(compatibilityValidator);
        Mockito.when(compatibilityValidator.validate())
               .thenReturn(compatibleDescriptor);

        MtaDescriptorMerger merger = new MtaDescriptorMerger(handlerFactory, platform);
        DeploymentDescriptor result = merger.merge(inputDescriptor, extensionDescriptors, List.of());

        Assertions.assertSame(compatibleDescriptor, result);
        Mockito.verify(validator)
               .validateDeploymentDescriptor(inputDescriptor, platform);
        Mockito.verify(validator)
               .validateExtensionDescriptors(extensionDescriptors, inputDescriptor);
        Mockito.verify(validator)
               .validateMergedDescriptor(mergedDescriptor);
        Mockito.verify(platformMerger)
               .mergeInto(mergedDescriptor);
    }

    @Test
    void testMergeWithUserMessageLoggerLogsDebug() {
        List<ExtensionDescriptor> extensionDescriptors = List.of();
        Mockito.when(descriptorMerger.merge(inputDescriptor, extensionDescriptors))
               .thenReturn(mergedDescriptor);
        Mockito.when(handlerFactory.getDescriptorParametersCompatibilityValidator(mergedDescriptor, userMessageLogger))
               .thenReturn(compatibilityValidator);
        Mockito.when(compatibilityValidator.validate())
               .thenReturn(compatibleDescriptor);

        MtaDescriptorMerger merger = new MtaDescriptorMerger(handlerFactory, platform, userMessageLogger);
        DeploymentDescriptor result = merger.merge(inputDescriptor, extensionDescriptors, List.of("password"));

        Assertions.assertSame(compatibleDescriptor, result);
        Mockito.verify(userMessageLogger)
               .debug(Mockito.anyString(), Mockito.any(Object[].class));
    }

}
