package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ProcessMtaArchiveStepTest extends SyncFlowableStepTest<ProcessMtaArchiveStep> {

    private static final String SPACE_ID = "0";

    private static final String FILE_ID = "0";

    private StepInput input;

    private Manifest outerManifest;

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareFileService();
        when(configuration.getMaxMtaDescriptorSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        when(configuration.getMaxManifestSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MANIFEST_SIZE);

    }

    private void prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_APP_ARCHIVE_ID, FILE_ID);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        step.conflictPreventerSupplier = (dao) -> mock(ProcessConflictPreventer.class);
    }

    private void prepareFileService() throws Exception {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                FileDownloadProcessor contentProcessor = (FileDownloadProcessor) invocation.getArguments()[0];

                contentProcessor.processContent(getClass().getResourceAsStream(input.archiveFileLocation));
                return null;
            }

        }).when(fileService)
            .processFileContent(any());
    }

    @Test
    public void testModulesResourcesDependenciesCreatedSuccessfully() throws Exception {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString("process-mta-archive-step-1.json", ProcessMtaArchiveStepTest.class),
            StepInput.class);

        step.execute(context);
        assertStepFinishedSuccessfully();

        testModules();
        testResources();
        testDependencies();
    }

    @Test
    public void testResourceWithoutCurrentConfigParameters() throws Exception {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString("process-mta-archive-step-2.json", ProcessMtaArchiveStepTest.class),
            StepInput.class);
        step.execute(context);

        testResources();
    }

    @Test
    public void testConfigParametersFromTheDeploymentDescriptorWillOverrideParametersFromTheConfigFile() throws Exception {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString("process-mta-archive-step-3.json", ProcessMtaArchiveStepTest.class),
            StepInput.class);
        step.execute(context);

        DeploymentDescriptor unresolvedDeploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
        Resource resource = unresolvedDeploymentDescriptor.getResources2()
            .get(0);
        Map<String, Object> configResourceParams = (Map<String, Object>) new PropertiesAccessor().getParameters(resource)
            .get(SupportedParameters.SERVICE_CONFIG);

        String xsappname = "xsappname";
        assertEquals(input.expectedResourceConfigParameters.get(xsappname), (String) configResourceParams.get(xsappname));
    }

    @Test
    public void testAllConfigParametersAreAdded() {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString("process-mta-archive-step-4.json", ProcessMtaArchiveStepTest.class),
            StepInput.class);
        step.execute(context);
        DeploymentDescriptor unresolvedDeploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
        Resource resource = unresolvedDeploymentDescriptor.getResources2()
            .get(0);
        Map<String, Object> configResourceParams = (Map<String, Object>) new PropertiesAccessor().getParameters(resource)
            .get(SupportedParameters.SERVICE_CONFIG);
        String xsappname = "xsappname";
        String anotherField = "anotherField";
        String anotherField1 = "anotherField1";
        assertEquals(input.expectedResourceConfigParameters.get(xsappname), (String) configResourceParams.get(xsappname));
        assertEquals(input.expectedResourceConfigParameters.get(anotherField), (String) configResourceParams.get(anotherField));
        assertEquals(input.expectedResourceConfigParameters.get(anotherField1), (String) configResourceParams.get(anotherField1));
    }

    private void testModules() throws Exception {
        List<String> actualModules = StepsUtil.getArrayVariableAsList(context,
            com.sap.cloud.lm.sl.cf.process.Constants.VAR_MTA_ARCHIVE_MODULES);

        assertEquals(input.expectedModules.size(), actualModules.size());

        for (String actualModuleName : actualModules) {
            assertTrue(input.expectedModules.contains(actualModuleName));
        }
    }

    private void testResources() throws Exception {
        Map<String, Attributes> entries = outerManifest.getEntries();
        DeploymentDescriptor unresolvedDeploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
        for (String expectedResource : input.expectedResources) {
            String resourceFileName = getResourceFileName(entries, expectedResource);
            assertTrue(StepsUtil.getResourceFileName(context, expectedResource)
                .equals(resourceFileName));
            Resource resource = getResource(unresolvedDeploymentDescriptor, expectedResource);
            Map<String, Object> configResourceParams = (Map<String, Object>) new PropertiesAccessor().getParameters(resource)
                .get(SupportedParameters.SERVICE_CONFIG);
            String fileContent = getFileContent(resourceFileName);
            Map<String, Object> convertedJson = JsonUtil.convertJsonToMap(fileContent);
            assertTrue(configResourceParams.entrySet()
                .containsAll(convertedJson.entrySet()));
        }

    }

    private void testDependencies() throws Exception {
        for (String expecedDependecy : input.expectedRequiredDependencies) {
            assertTrue(StepsUtil.getRequiresFileName(context, expecedDependecy) != null);
        }
    }

    private Resource getResource(DeploymentDescriptor deploymentDescriptor, String resourceName) {
        return deploymentDescriptor.getResources2()
            .stream()
            .filter(r -> r.getName()
                .equals(resourceName))
            .findFirst()
            .orElse(null);
    }

    private String getFileContent(String fileName) throws IOException {
        File file = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(getClass().getResourceAsStream(input.archiveFileLocation))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                zipEntry = zipInputStream.getNextEntry();
                if (fileName.equals(zipEntry.getName())) {
                    file = new File(zipEntry.getName());
                    Path filePath = Paths.get(zipEntry.getName());
                    unzipFile(zipInputStream, filePath);
                    return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                }
            }
        } finally {
            FileUtils.deleteQuietly(file);
        }

        return null;
    }

    private void unzipFile(ZipInputStream zipInputStream, Path unzipFilePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath()
            .toString()))) {

            byte[] bytes = new byte[1024];
            int read = 0;
            while ((read = zipInputStream.read(bytes)) != -1) {
                bos.write(bytes, 0, read);
            }
        }
    }

    private static class StepInput {

        String archiveFileLocation;
        Set<String> expectedModules;
        Set<String> expectedResources;
        Set<String> expectedRequiredDependencies;
        Map<String, String> expectedResourceConfigParameters;
    }

    private class ProcessMtaArchiveStepMock extends ProcessMtaArchiveStep {

        @Override
        protected MtaArchiveHelper getHelper(Manifest manifest) {
            Map<String, Attributes> entries = manifest.getEntries();
            outerManifest = manifest;
            MtaArchiveHelper helper = Mockito.mock(MtaArchiveHelper.class);
            when(helper.getMtaArchiveModules()).thenReturn(input.expectedModules.stream()
                .collect(Collectors.toMap(moduleName -> moduleName, Function.identity())));
            when(helper.getMtaArchiveResources()).thenReturn(input.expectedResources.stream()
                .collect(Collectors.toMap(resourceName -> resourceName, resourceName -> getResourceFileName(entries, resourceName))));
            when(helper.getMtaRequiresDependencies()).thenReturn(input.expectedRequiredDependencies.stream()
                .collect(Collectors.toMap(dependencyName -> dependencyName, Function.identity())));
            try {
                doAnswer(a -> null).when(helper)
                    .init();
            } catch (SLException e) {
                // Ignore...
            }

            return helper;
        }

    }

    private String getResourceFileName(Map<String, Attributes> entries, String resourceName) {
        return entries.entrySet()
            .stream()
            .filter(entry -> resourceName.equals(entry.getValue()
                .getValue(MtaArchiveHelper.ATTR_MTA_RESOURCE)))
            .findFirst()
            .map(v -> v.getKey())
            .orElse(null);
    }

    @Override
    protected ProcessMtaArchiveStep createStep() {
        return new ProcessMtaArchiveStepMock();
    }

}
