package org.cloudfoundry.multiapps.controller.process.steps;

import jakarta.inject.Inject;
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
import org.cloudfoundry.multiapps.controller.process.util.ContentLengthTracker;
import org.cloudfoundry.multiapps.controller.process.util.ExternalFileProcessor;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableFileEntryProperties;
import org.cloudfoundry.multiapps.controller.process.util.MtaArchiveContentResolver;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

public class ProcessMtaArchiveStep extends SyncFlowableStep {

    @Inject
    private OperationService operationService;
    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;
    @Inject
    private ArchiveEntryExtractor archiveEntryExtractor;
    @Inject
    protected ArchiveEntryStreamWithStreamPositionsDeterminer archiveEntryStreamWithStreamPositionsDeterminer;

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);
        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        getStepLogger().debug(Messages.MTA_ARCHIVE_ID_0_MESSAGE, appArchiveId);
        processApplicationArchive(context, appArchiveId);

        setMtaIdForProcess(context);
        acquireOperationLock(context);
        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    private void processApplicationArchive(ProcessContext context, String appArchiveId) {
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = archiveEntryStreamWithStreamPositionsDeterminer.determineArchiveEntries(
            context.getRequiredVariable(Variables.SPACE_GUID), appArchiveId);
        context.setVariable(Variables.ARCHIVE_ENTRIES_POSITIONS, archiveEntriesWithStreamPositions);
        MtaArchiveHelper helper = createMtaArchiveHelperFromManifest(context, appArchiveId, archiveEntriesWithStreamPositions);

        DeploymentDescriptor deploymentDescriptor = extractDeploymentDescriptor(context, appArchiveId, archiveEntriesWithStreamPositions);

        ContentLengthTracker sizeTracker = new ContentLengthTracker();
        ExternalFileProcessor fileProcessor = new ExternalFileProcessor(sizeTracker, configuration.getMaxResourceFileSize(), fileService);
        
        if (context.getVariable(Variables.SHOULD_BACKUP_PREVIOUS_VERSION)) {
            MtaArchiveContentResolver contentResolver = new MtaArchiveContentResolver(deploymentDescriptor, configuration, fileProcessor, sizeTracker);
            contentResolver.resolveMtaArchiveFilesInDescriptor(context.getVariable(Variables.SPACE_GUID), appArchiveId, helper);
        }

        MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
        addMtaArchiveModulesInMtaArchiveElements(context, helper, mtaArchiveElements);
        addMtaRequiredDependenciesInMtaArchiveElements(helper, mtaArchiveElements);
        addMtaArchiveResourcesInMtaArchiveElements(helper, mtaArchiveElements);
        context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    private MtaArchiveHelper createMtaArchiveHelperFromManifest(ProcessContext context, String appArchiveId, List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        ArchiveEntryWithStreamPositions mtaManifestEntry = ArchiveEntryExtractorUtil.findEntry(ArchiveHandler.MTA_MANIFEST_NAME,
                                                                                               archiveEntriesWithStreamPositions);
        byte[] inflatedManifestFile = readEntry(context, appArchiveId, mtaManifestEntry);
        try (InputStream inputStream = new ByteArrayInputStream(inflatedManifestFile)) {
            Manifest manifest = new Manifest(inputStream);
            MtaArchiveHelper helper = getHelper(manifest);
            helper.init();
            return helper;
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private byte[] readEntry(ProcessContext context, String appArchiveId, ArchiveEntryWithStreamPositions mtaManifestEntry) {
        return archiveEntryExtractor.extractEntryBytes(ImmutableFileEntryProperties.builder()
                                                                                   .guid(appArchiveId)
                                                                                   .name(mtaManifestEntry.getName())
                                                                                   .spaceGuid(context.getRequiredVariable(Variables.SPACE_GUID))
                                                                                   .maxFileSizeInBytes(configuration.getMaxMtaDescriptorSize())
                                                                                   .build(), mtaManifestEntry);
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void addMtaArchiveModulesInMtaArchiveElements(ProcessContext context, MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
        mtaArchiveModules.forEach(mtaArchiveElements::addModuleFileName);
        getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES_0_MESSAGE, mtaArchiveModules.keySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, mtaArchiveModules.keySet());
    }

    private void addMtaRequiredDependenciesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
        mtaArchiveRequiresDependencies.forEach(mtaArchiveElements::addRequiredDependencyFileName);
        getStepLogger().debug(Messages.MTA_ARCHIVE_REQUIRES_0_MESSAGE, mtaArchiveRequiresDependencies.keySet());
    }

    private void addMtaArchiveResourcesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
        mtaArchiveResources.forEach(mtaArchiveElements::addResourceFileName);
        getStepLogger().debug(Messages.MTA_ARCHIVE_RESOURCES_0_MESSAGE, mtaArchiveResources.keySet());
    }

    private DeploymentDescriptor extractDeploymentDescriptor(ProcessContext context, String appArchiveId, List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {

        ArchiveEntryWithStreamPositions deploymentDescriptorEntry = ArchiveEntryExtractorUtil.findEntry(ArchiveHandler.MTA_DEPLOYMENT_DESCRIPTOR_NAME,
                                                                                                        archiveEntriesWithStreamPositions);
        byte[] inflatedDeploymentDescriptor = readEntry(context, appArchiveId, deploymentDescriptorEntry);
        DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
        DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(new String(inflatedDeploymentDescriptor));
        getStepLogger().debug(Messages.MTA_DESCRIPTOR_LENGTH_0_MESSAGE, inflatedDeploymentDescriptor.length);
        return deploymentDescriptor;
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
                                 .acquireLock(mtaId, namespace, context.getVariable(Variables.SPACE_GUID), context.getVariable(Variables.CORRELATION_ID));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

}
