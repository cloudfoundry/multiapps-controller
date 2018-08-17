package com.sap.cloud.lm.sl.cf.core.helpers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.MtaSchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class MtaArchiveBuilder {

    static final String MTA_ASSEMBLY_DIR = "mta-assembly";
    private static final String META_INF_DIR = "META-INF/";
    private static final String MTAD_YAML = "mtad.yaml";
    static final String DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH = META_INF_DIR + MTAD_YAML;

    private static final int BUFFER_SIZE = 4 * 1024;

    private Path mtaDir;
    private Path deploymentDescriptorFile;
    private DeploymentDescriptor deploymentDescriptor;
    private Path mtaAssemblyDir;

    private Map<String, Attributes> manifestEntries;
    private List<Path> jarEntries;

    Map<String, Attributes> getManifestEntries() {
        return manifestEntries;
    }

    List<Path> getJarEntries() {
        return jarEntries;
    }

    Path getMtaAssemblyDir() {
        return mtaAssemblyDir;
    }

    public MtaArchiveBuilder(Path mtaDirectory) {
        this.mtaDir = mtaDirectory;
        this.deploymentDescriptor = getDeploymentDescriptor(mtaDirectory);
    }

    private DeploymentDescriptor getDeploymentDescriptor(Path mtaDirectory) {
        deploymentDescriptorFile = findDeploymenDescriptor(mtaDirectory);
        String deploymentDescriptorString = readDeploymentDescriptor(deploymentDescriptorFile);

        Version schemaVersion = new MtaSchemaVersionDetector().detect(deploymentDescriptorString, Collections.emptyList());
        HandlerFactory handlerFactory = new HandlerFactory(schemaVersion.getMajor(), schemaVersion.getMinor());

        if (handlerFactory.getMajorVersion() < 2) {
            throw new ContentException(Messages.THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1,
                deploymentDescriptorFile.toAbsolutePath(), handlerFactory.getMajorVersion());
        }

        return (DeploymentDescriptor) handlerFactory.getDescriptorParser()
            .parseDeploymentDescriptorYaml(deploymentDescriptorString);
    }

    public Path buildMtaArchive() {
        mtaAssemblyDir = mtaDir.resolve(MTA_ASSEMBLY_DIR);
        if (mtaAssemblyDir.toFile()
            .exists()) {
            try {
                FileUtils.deleteDirectory(mtaAssemblyDir);
                Files.createDirectory(mtaAssemblyDir);
            } catch (IOException e) {
                throw new SLException(e, Messages.CANNOT_CLEAN_MULTI_TARGET_APP_ASSEMBLY_TARGET_DIR_0, mtaAssemblyDir.toAbsolutePath());
            }
        }

        prepareArchiveContents();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes()
            .put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getEntries()
            .putAll(manifestEntries);

        Path mtaArchive = mtaAssemblyDir.resolve(mtaDir.getFileName()
            .toString() + ".mtar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(mtaArchive), manifest)) {
            for (Path source : jarEntries) {
                addJarEntry(source, jarOutputStream);
            }
        } catch (IOException e) {
            throw new SLException(e, "Failed to build mta archive");
        }

        return mtaArchive;
    }

    private void prepareArchiveContents() {
        manifestEntries = new HashMap<>();
        jarEntries = new ArrayList<>();
        prepareDirectories();
        manifestEntries.put(DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH, new Attributes());
        prepareModules();
        prepareResourceEntries(deploymentDescriptor);

    }

    private void prepareModules() {
        Map<String, List<Module>> modulesMap = createModulesMap(deploymentDescriptor.getModules2_0());
        for (Map.Entry<String, List<Module>> entry : modulesMap.entrySet()) {
            prepareModuleEntries(entry.getKey(), entry.getValue());
            prepareDependencies(entry.getValue());
        }
    }

    private void prepareDependencies(List<Module> modules) {
        for (Module module : modules) {
            prepareModuleDependencies(module);
        }
    }

    private void prepareDirectories() {
        try {
            Files.createDirectories(mtaAssemblyDir.resolve(META_INF_DIR));
            Path targetDeployDescriptorFile = mtaAssemblyDir.resolve(DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH);
            Files.copy(deploymentDescriptorFile, targetDeployDescriptorFile, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
            jarEntries.add(targetDeployDescriptorFile);
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_COPY_FILE_0_TO_ASSEMBLY_DIRECTORY, deploymentDescriptorFile.toAbsolutePath());
        }

    }

    private void prepareResourceEntries(DeploymentDescriptor deploymentDescriptor) {
        for (Resource resource : deploymentDescriptor.getResources2_0()) {
            String resourceConfigPath = (String) resource.getParameters()
                .get(SupportedParameters.SERVICE_CONFIG_PATH);
            if (resourceConfigPath != null) {
                prepareFile(resourceConfigPath);

                Attributes attributes = new Attributes();
                attributes.putValue(MtaArchiveHelper.ATTR_MTA_RESOURCE, resource.getName());
                manifestEntries.put(resourceConfigPath, attributes);
            }
        }
    }

    private void prepareModuleEntries(String path, List<Module> modules) {
        prepareFile(path);
        Attributes attributes = new Attributes();
        List<String> moduleNames = modules.stream()
            .map(Module::getName)
            .collect(Collectors.toList());
        attributes.putValue(MtaArchiveHelper.ATTR_MTA_MODULE, String.join(Constants.MODULE_SEPARATOR, moduleNames));
        manifestEntries.put(path, attributes);
    }

    private void prepareModuleDependencies(Module module) {
        for (RequiredDependency requiredDependency : module.getRequiredDependencies2_0()) {
            String requiredDependencyConfigPath = (String) requiredDependency.getParameters()
                .get(SupportedParameters.SERVICE_BINDING_CONFIG_PATH);
            if (requiredDependencyConfigPath != null) {
                prepareFile(requiredDependencyConfigPath);

                Attributes dependencyAttributes = new Attributes();
                dependencyAttributes.putValue(MtaArchiveHelper.ATTR_MTA_REQUIRES_DEPENDENCY,
                    ValidatorUtil.getPrefixedName(module.getName(), requiredDependency.getName(), Constants.MTA_ELEMENT_SEPARATOR));
                manifestEntries.put(requiredDependencyConfigPath, dependencyAttributes);
            }
        }
    }

    private void prepareFile(String path) {
        MtaPathValidator.validatePath(path);
        Path source = mtaDir.resolve(path);
        File sourceAsFile = source.toFile();
        if (!sourceAsFile.exists()) {
            throw new SLException(Messages.PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE, path, source.toAbsolutePath());
        }
        try {
            Path target = mtaAssemblyDir.resolve(path);
            if (sourceAsFile.isDirectory()) {
                FileUtils.copyDirectory(source, target);
            } else {
                FileUtils.copyFile(source, target);
            }
            jarEntries.add(target);
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_COPY_FILE_0_TO_ASSEMBLY_DIRECTORY, source.toAbsolutePath());
        }
    }

    private void addJarEntry(Path source, JarOutputStream out) throws IOException {
        if (source.toFile()
            .isDirectory()) {
            JarEntry entry = createJarEntry(source);
            out.putNextEntry(entry);
            out.closeEntry();
            try (Stream<Path> dirStream = Files.list(source)) {
                Iterator<Path> nestedFilesIterator = dirStream.iterator();
                while (nestedFilesIterator.hasNext()) {
                    addJarEntry(nestedFilesIterator.next(), out);
                }
            }
            return;
        }

        JarEntry entry = createJarEntry(source);
        out.putNextEntry(entry);

        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(source))) {
            int read = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((read = in.read(buffer)) > -1) {
                out.write(buffer, 0, read);
            }
            out.closeEntry();
        }
    }

    private JarEntry createJarEntry(Path source) throws IOException {
        String entryName = FilenameUtils.separatorsToUnix(mtaAssemblyDir.relativize(source)
            .toString());
        if (source.toFile()
            .isDirectory() && !entryName.endsWith(Constants.UNIX_PATH_SEPARATOR)) {
            entryName += Constants.UNIX_PATH_SEPARATOR;
        }

        JarEntry entry = new JarEntry(entryName);
        entry.setTime(Files.getLastModifiedTime(source)
            .toMillis());
        return entry;
    }

    private static String readDeploymentDescriptor(Path deploymentDescriptorFile) {
        try {
            return new String(Files.readAllBytes(deploymentDescriptorFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0, deploymentDescriptorFile.toAbsolutePath());
        }
    }

    private Path findDeploymenDescriptor(Path mtaDirectory) {
        try (Stream<Path> mtaDirContents = Files.list(mtaDirectory)) {
            Optional<Path> deploymentDescriptor = mtaDirContents.filter(path -> MTAD_YAML.equals(path.getFileName()
                .toString()))
                .findFirst();
            if (!deploymentDescriptor.isPresent()) {
                throw new SLException(Messages.DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1,
                    mtaDirectory.getFileName()
                        .toString(),
                    MTAD_YAML);
            }
            return deploymentDescriptor.get();
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_LIST_MULTI_TARGET_APP_DIRECTORY_0, mtaDirectory);
        }
    }

    private Map<String, List<Module>> createModulesMap(List<Module> modules) {
        Map<String, List<Module>> modulesMap = new HashMap<>();
        modules.forEach(module -> putModuleEntry(modulesMap, module));
        return modulesMap;
    }

    private void putModuleEntry(Map<String, List<Module>> map, Module module) {
        String modulePath = module.getPath();
        if (modulePath != null) {
            List<Module> moduleList = map.computeIfAbsent(modulePath, path -> new ArrayList<>());
            moduleList.add(module);
        }

    }
}