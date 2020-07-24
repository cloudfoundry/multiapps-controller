package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.builders.ExtensionDescriptorChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.junit.Test;
import org.mockito.Mockito;

public class ProcessMtaExtensionDescriptorsStepTest extends SyncFlowableStepTest<ProcessMtaExtensionDescriptorsStep> {

    private static final String SPACE_ID = "foo";

    @Test
    public void testExecute() throws FileStorageException {
        final String extensionDescriptorString1 = "abc";
        final String extensionDescriptorString2 = "def";
        // final ExtensionDescriptor extensionDescriptor1 = DescriptorTestUtil.loadExtensionDescriptor("config-01.mtaext", getClass());
        final ExtensionDescriptor extensionDescriptor2 = DescriptorTestUtil.loadExtensionDescriptor("config-02.mtaext", getClass());
        final ExtensionDescriptor extensionDescriptor3 = DescriptorTestUtil.loadExtensionDescriptor("config-01.mtaext", getClass());
        final List<ExtensionDescriptor> extensionDescriptorChain = Arrays.asList(extensionDescriptor2, extensionDescriptor3);

        prepare(Arrays.asList(extensionDescriptorString1, extensionDescriptorString2));
        DescriptorParserFacade descriptorParserFacade = Mockito.mock(DescriptorParserFacade.class);
        Mockito.when(descriptorParserFacade.parseExtensionDescriptor(Mockito.<InputStream> any()))
               .thenReturn(extensionDescriptor2, extensionDescriptor3);

        ExtensionDescriptorChainBuilder extensionDescriptorChainBuilder = Mockito.mock(ExtensionDescriptorChainBuilder.class);
        Mockito.when(extensionDescriptorChainBuilder.build(Mockito.any(), Mockito.eq(extensionDescriptorChain)))
               .thenReturn(extensionDescriptorChain);

        DescriptorParserFacadeFactory descriptorParserFacadeFactory = Mockito.mock(DescriptorParserFacadeFactory.class);
        Mockito.when(descriptorParserFacadeFactory.getInstance())
               .thenReturn(descriptorParserFacade);
        step.descriptorParserFactory = descriptorParserFacadeFactory;
        step.extensionDescriptorChainBuilder = extensionDescriptorChainBuilder;

        step.execute(execution);

        List<ExtensionDescriptor> actualExtensionDescriptorChain = context.getVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);
        String expectedJson = JsonUtil.toJson(extensionDescriptorChain, true);
        String actualJson = JsonUtil.toJson(actualExtensionDescriptorChain, true);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testExecuteWithNoExtensionDescriptors() throws FileStorageException {
        prepare(Collections.emptyList());

        DescriptorParserFacadeFactory descriptorParserFacadeFactory = Mockito.mock(DescriptorParserFacadeFactory.class);
        Mockito.when(descriptorParserFacadeFactory.getInstance())
               .thenReturn(new DescriptorParserFacade());
        step.descriptorParserFactory = descriptorParserFacadeFactory;
        step.execute(execution);

        List<ExtensionDescriptor> extensionDescriptorChain = context.getVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);
        assertTrue(extensionDescriptorChain.isEmpty());
    }

    private void prepare(List<String> extensionDescriptors) throws FileStorageException {
        Map<String, String> fileIdToExtensionDescriptor = generateIds(extensionDescriptors);

        context.setVariable(Variables.EXT_DESCRIPTOR_FILE_ID, String.join(",", fileIdToExtensionDescriptor.keySet()));
        context.setVariable(Variables.SPACE_GUID, SPACE_ID);
        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml", getClass());
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);

        prepareFileService(fileIdToExtensionDescriptor);
    }

    private void prepareFileService(Map<String, String> fileIdToExtensionDescriptor) throws FileStorageException {
        Mockito.doAnswer((invocation) -> {
            String fileId = invocation.getArgument(1);
            FileContentConsumer contentConsumer = invocation.getArgument(2);
            String fileContent = fileIdToExtensionDescriptor.get(fileId);

            contentConsumer.consume(IOUtils.toInputStream(fileContent, StandardCharsets.UTF_8));
            return null;
        })
               .when(fileService)
               .consumeFileContent(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    private Map<String, String> generateIds(List<String> extensionDescriptors) {
        return extensionDescriptors.stream()
                                   .collect(Collectors.toMap(extensionDescriptor -> generateRandomId(),
                                                             extensionDescriptor -> extensionDescriptor));
    }

    private String generateRandomId() {
        return UUID.randomUUID()
                   .toString();
    }

    @Override
    protected ProcessMtaExtensionDescriptorsStep createStep() {
        return new ProcessMtaExtensionDescriptorsStep();
    }

}
