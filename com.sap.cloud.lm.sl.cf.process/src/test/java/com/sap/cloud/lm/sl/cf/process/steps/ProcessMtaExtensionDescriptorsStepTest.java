package com.sap.cloud.lm.sl.cf.process.steps;

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
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.util.DescriptorTestUtil;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.builders.ExtensionDescriptorChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;

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

        step.descriptorParserFacade = descriptorParserFacade;
        step.extensionDescriptorChainBuilder = extensionDescriptorChainBuilder;

        step.execute(execution);

        List<ExtensionDescriptor> actualExtensionDescriptorChain = StepsUtil.getExtensionDescriptorChain(execution);
        String expectedJson = JsonUtil.toJson(extensionDescriptorChain, true);
        String actualJson = JsonUtil.toJson(actualExtensionDescriptorChain, true);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testExecuteWithNoExtensionDescriptors() throws FileStorageException {
        prepare(Collections.emptyList());

        step.execute(execution);

        List<ExtensionDescriptor> extensionDescriptorChain = StepsUtil.getExtensionDescriptorChain(execution);
        assertTrue(extensionDescriptorChain.isEmpty());
    }

    private void prepare(List<String> extensionDescriptors) throws FileStorageException {
        Map<String, String> fileIdToExtensionDescriptor = generateIds(extensionDescriptors);

        execution.setVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID, String.join(",", fileIdToExtensionDescriptor.keySet()));
        execution.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor("node-hello-mtad.yaml", getClass());
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);

        prepareFileService(fileIdToExtensionDescriptor);
    }

    private void prepareFileService(Map<String, String> fileIdToExtensionDescriptor) throws FileStorageException {
        Mockito.doAnswer((invocation) -> {
            String fileId = invocation.getArgument(1);
            FileContentProcessor contentProcessor = invocation.getArgument(2);
            String fileContent = fileIdToExtensionDescriptor.get(fileId);

            contentProcessor.processFileContent(IOUtils.toInputStream(fileContent, StandardCharsets.UTF_8));
            return null;
        })
               .when(fileService)
               .processFileContent(Mockito.anyString(), Mockito.anyString(), Mockito.any());
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
