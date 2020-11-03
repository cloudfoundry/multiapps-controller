package org.cloudfoundry.multiapps.controller.core.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.handlers.SchemaVersionDetector;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.cloudfoundry.multiapps.mta.util.NameUtil;

public class MtaArchiveBuilder {

    private static final String MTA_ASSEMBLY_DIR = "mta-assembly";
    private static final String META_INF_DIR = "META-INF/";
    private static final String MTAD_YAML = "mtad.yaml";
    static final String DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH = META_INF_DIR + MTAD_YAML;

    private final Path mtaDir;
    private Path deploymentDescriptorFile;
    private final DeploymentDescriptor deploymentDescriptor;
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

    public MtaArchiveBuilder(Path mtaDirectory, DescriptorParserFacade descriptorParserFacade) {
        this.mtaDir = mtaDirectory;
        this.deploymentDescriptor = getDeploymentDescriptor(mtaDirectory, descriptorParserFacade);
    }

    private DeploymentDescriptor getDeploymentDescriptor(Path mtaDirectory, DescriptorParserFacade descriptorParserFacade) {
        deploymentDescriptorFile = findDeploymentDescriptor(mtaDirectory);
        String deploymentDescriptorString = readDeploymentDescriptor(deploymentDescriptorFile);

        DeploymentDescriptor parsedDeploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(deploymentDescriptorString);
        Version schemaVersion = new SchemaVersionDetector().detect(parsedDeploymentDescriptor, Collections.emptyList());

        if (schemaVersion.getMajor() < 2) {
            throw new ContentException(Messages.THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1,
                                       deploymentDescriptorFile.toAbsolutePath(),
                                       schemaVersion.getMajor());
        }

        return parsedDeploymentDescriptor;
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
                                                       .toString()
            + ".mtar");
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
        Map<String, List<Module>> modulesMap = createModulesMap(deploymentDescriptor.getModules());
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
        for (Resource resource : deploymentDescriptor.getResources()) {
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
        for (RequiredDependency requiredDependency : module.getRequiredDependencies()) {
            String requiredDependencyConfigPath = (String) requiredDependency.getParameters()
                                                                             .get(SupportedParameters.SERVICE_BINDING_CONFIG_PATH);
            if (requiredDependencyConfigPath != null) {
                prepareFile(requiredDependencyConfigPath);

                Attributes dependencyAttributes = new Attributes();
                dependencyAttributes.putValue(MtaArchiveHelper.ATTR_MTA_REQUIRES_DEPENDENCY,
                                              NameUtil.getPrefixedName(module.getName(), requiredDependency.getName(),
                                                                            Constants.MTA_ELEMENT_SEPARATOR));
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
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(source)) {
                for (Path path : dirStream) {
                    addJarEntry(path, out);
                }
            }
            return;
        }

        JarEntry entry = createJarEntry(source);
        out.putNextEntry(entry);
        Files.copy(source, out);
        out.closeEntry();
    }

    private JarEntry createJarEntry(Path source) throws IOException {
        String entryName = FilenameUtils.separatorsToUnix(mtaAssemblyDir.relativize(source)
                                                                        .toString());
        if (source.toFile()
                  .isDirectory()
            && !entryName.endsWith(Constants.UNIX_PATH_SEPARATOR)) {
            entryName += Constants.UNIX_PATH_SEPARATOR;
        }

        JarEntry entry = new JarEntry(entryName);
        entry.setTime(Files.getLastModifiedTime(source)
                           .toMillis());
        return entry;
    }

    private static String readDeploymentDescriptor(Path deploymentDescriptorFile) {
        try {
            return Files.readString(deploymentDescriptorFile);
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0, deploymentDescriptorFile.toAbsolutePath());
        }
    }

    private Path findDeploymentDescriptor(Path mtaDirectory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(mtaDirectory)) {
            for (Path path : dirStream) {
                if (MTAD_YAML.equals(path.getFileName()
                                         .toString())) {
                    return path;
                }
            }
            throw new SLException(Messages.DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1,
                                  mtaDirectory.getFileName()
                                              .toString(),
                                  MTAD_YAML);
        } catch (IOException e) {
            throw new SLException(e, Messages.FAILED_TO_LIST_MULTI_TARGET_APP_DIRECTORY_0, mtaDirectory);
        }
    }

    private Map<String, List<Module>> createModulesMap(List<Module> modules) {
        return modules.stream()
                      .filter(module -> module.getPath() != null)
                      .collect(Collectors.groupingBy(Module::getPath));
    }
}
