package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryExtractor;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryExtractorUtil;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryStreamWithStreamPositionsDeterminer;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableFileEntryProperties;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import jakarta.inject.Inject;

public class ProcessMtaArchiveStep extends SyncFlowableStep {

    @Inject
    private OperationService operationService;
    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;
    @Inject
    private ArchiveEntryExtractor archiveEntryExtractor;
    @Inject
    private ArchiveEntryStreamWithStreamPositionsDeterminer archiveEntryStreamWithStreamPositionsDeterminer;

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);
        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);
        processApplicationArchive(context, appArchiveId);
        setMtaIdForProcess(context);
        acquireOperationLock(context);
        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

    private void processApplicationArchive(ProcessContext context, String appArchiveId) {
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = archiveEntryStreamWithStreamPositionsDeterminer.determineArchiveEntries(context,
                                                                                                                                                          appArchiveId);
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, archiveEntriesWithStreamPositions);
        MtaArchiveHelper helper = createMtaArchiveHelperFromManifest(context, appArchiveId, archiveEntriesWithStreamPositions);
        MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
        addMtaArchiveModulesInMtaArchiveElements(context, helper, mtaArchiveElements);
        addMtaRequiredDependenciesInMtaArchiveElements(helper, mtaArchiveElements);
        addMtaArchiveResourcesInMtaArchiveElements(helper, mtaArchiveElements);
        context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
        DeploymentDescriptor deploymentDescriptor = extractDeploymentDescriptor(context, appArchiveId, archiveEntriesWithStreamPositions);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    private DeploymentDescriptor extractDeploymentDescriptor(ProcessContext context, String appArchiveId,
                                                             List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {

        ArchiveEntryWithStreamPositions deploymentDescriptorEntry = ArchiveEntryExtractorUtil.findEntry(ArchiveHandler.MTA_DEPLOYMENT_DESCRIPTOR_NAME,
                                                                                                        archiveEntriesWithStreamPositions);
        byte[] inflatedDeploymentDescriptor = archiveEntryExtractor.readFullEntry(ImmutableFileEntryProperties.builder()
                                                                                                              .guid(appArchiveId)
                                                                                                              .spaceGuid(context.getRequiredVariable(Variables.SPACE_GUID))
                                                                                                              .maxFileSize(configuration.getMaxMtaDescriptorSize())
                                                                                                              .build(),
                                                                                  deploymentDescriptorEntry);
        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(new String(inflatedDeploymentDescriptor));
        getStepLogger().debug("MTA Descriptor length: {0}", inflatedDeploymentDescriptor.length);
        return deploymentDescriptor;
    }

    private MtaArchiveHelper createMtaArchiveHelperFromManifest(ProcessContext context, String appArchiveId,
                                                                List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        ArchiveEntryWithStreamPositions deploymentDescriptorEntry = ArchiveEntryExtractorUtil.findEntry(ArchiveHandler.MTA_MANIFEST_NAME,
                                                                                                        archiveEntriesWithStreamPositions);
        byte[] inflatedManifestFile = archiveEntryExtractor.readFullEntry(ImmutableFileEntryProperties.builder()
                                                                                                      .guid(appArchiveId)
                                                                                                      .spaceGuid(context.getRequiredVariable(Variables.SPACE_GUID))
                                                                                                      .maxFileSize(configuration.getMaxMtaDescriptorSize())
                                                                                                      .build(),
                                                                          deploymentDescriptorEntry);
        try (InputStream inputStream = new ByteArrayInputStream(inflatedManifestFile)) {
            Manifest manifest = new Manifest(inputStream);
            MtaArchiveHelper helper = getHelper(manifest);
            helper.init();
            return helper;
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void addMtaArchiveModulesInMtaArchiveElements(ProcessContext context, MtaArchiveHelper helper,
                                                          MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
        mtaArchiveModules.forEach(mtaArchiveElements::addModuleFileName);
        getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, mtaArchiveModules.keySet());
    }

    private void addMtaRequiredDependenciesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
        mtaArchiveRequiresDependencies.forEach(mtaArchiveElements::addRequiredDependencyFileName);
        getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());
    }

    private void addMtaArchiveResourcesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
        mtaArchiveResources.forEach(mtaArchiveElements::addResourceFileName);
        getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());
    }

    private void setMtaIdForProcess(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();

        context.setVariable(Variables.MTA_ID, mtaId);
    }

    private void acquireOperationLock(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, namespace, context.getVariable(Variables.SPACE_GUID),
                                              context.getVariable(Variables.CORRELATION_ID));
    }

}
