package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.api.MtasApiService;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableModule;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMta;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMtaGraph;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMtaGraphEdge;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMtaGraphNode;
import org.cloudfoundry.multiapps.controller.api.model.Metadata;
import org.cloudfoundry.multiapps.controller.api.model.Module;
import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.cloudfoundry.multiapps.controller.api.model.MtaGraph;
import org.cloudfoundry.multiapps.controller.api.model.MtaGraphEdge;
import org.cloudfoundry.multiapps.controller.api.model.MtaGraphNode;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.core.auditlogging.MtasApiServiceAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Named
public class MtasApiServiceImpl implements MtasApiService {

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    @Qualifier("deployedMtaRequiredDataOnlyDetector")
    private DeployedMtaDetector deployedMtaDetector;
    @Inject
    private MtasApiServiceAuditLog mtasApiServiceAuditLog;

    @Inject
    private DescriptorParserFacadeFactory descriptorParserFacadeFactory;

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid) {
        mtasApiServiceAuditLog.logGetMtas(SecurityContextUtil.getUsername(), spaceGuid);
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasWithoutNamespace(client);
        List<Mta> mtas = getMtas(deployedMtas, client);
        return ResponseEntity.ok()
                             .body(mtas);
    }

    @Override
    public ResponseEntity<Mta> getMta(String spaceGuid, String mtaId) {
        mtasApiServiceAuditLog.logGetMta(SecurityContextUtil.getUsername(), spaceGuid, mtaId);
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        DeployedMta deployedMta = detectSingleMta(mtaId, client);

        return ResponseEntity.ok()
                             .body(getMta(deployedMta, client));
    }

    @Override
    public ResponseEntity<MtaGraph> getMtaGraph(String spaceGuid, String mtaId, String namespace) {
        mtasApiServiceAuditLog.logGetMta(SecurityContextUtil.getUsername(), spaceGuid, mtaId);
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        DeployedMta deployedMta = namespace != null
            ? deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, namespace, client)
                                  .orElseThrow(() -> new NotFoundException(Messages.SPECIFIC_MTA_NOT_FOUND, mtaId, namespace))
            : detectSingleMta(mtaId, client);

        return ResponseEntity.ok()
                             .body(buildGraph(deployedMta));
    }

    private DeployedMta detectSingleMta(String mtaId, CloudControllerClient client) {
        List<DeployedMta> mtas = deployedMtaDetector.detectDeployedMtasByName(mtaId, client);

        if (mtas.isEmpty()) {
            throw new NotFoundException(Messages.MTA_NOT_FOUND, mtaId);
        }

        if (mtas.size() != 1) {
            throw new ConflictException(Messages.MTA_SEARCH_NOT_UNIQUE_BY_NAME, mtaId);
        }

        return mtas.get(0);
    }

    private MtaGraph buildGraph(DeployedMta mta) {
        Map<String, String> instanceNameToResourceName = new HashMap<>();
        for (DeployedMtaService service : mta.getServices()) {
            String resourceName = resolveResourceName(service);
            instanceNameToResourceName.putIfAbsent(service.getName(), resourceName);
        }

        // provided dependency name → module name that provides it
        Map<String, String> providedDepToModule = new HashMap<>();
        for (DeployedMtaApplication application : mta.getApplications()) {
            for (String provided : application.getProvidedDependencyNames()) {
                providedDepToModule.put(provided, application.getModuleName());
            }
        }

        java.util.Set<String> resourceNames = new java.util.HashSet<>();
        for (DeployedMtaService service : mta.getServices()) {
            resourceNames.add(resolveResourceName(service));
        }

        List<MtaGraphNode> nodes = new ArrayList<>();
        for (DeployedMtaApplication application : mta.getApplications()) {
            nodes.add(buildNode(application.getModuleName(), "module"));
        }
        for (DeployedMtaService service : mta.getServices()) {
            nodes.add(buildNode(resolveResourceName(service), "resource"));
        }

        List<MtaGraphEdge> edges = new ArrayList<>();
        for (DeployedMtaApplication application : mta.getApplications()) {
            for (String boundService : application.getBoundMtaServices()) {
                String target = instanceNameToResourceName.getOrDefault(boundService, boundService);
                edges.add(buildEdge(application.getModuleName(), target, "binds-service"));
            }
            for (String required : application.getRequiredDependencyNames()) {
                String providerModule = providedDepToModule.get(required);
                if (providerModule != null) {
                    edges.add(buildEdge(providerModule, application.getModuleName(), "provides", required));
                } else if (resourceNames.contains(required)) {
                    edges.add(buildEdge(application.getModuleName(), required, "requires-resource", required));
                }
            }
        }

        return ImmutableMtaGraph.builder()
                                .nodes(nodes)
                                .edges(edges)
                                .build();
    }

    private String resolveResourceName(DeployedMtaService service) {
        return service.getResourceName() != null ? service.getResourceName() : service.getName();
    }

    private MtaGraphNode buildNode(String id, String type) {
        return ImmutableMtaGraphNode.builder()
                                    .id(id)
                                    .type(type)
                                    .build();
    }

    private MtaGraphEdge buildEdge(String from, String to, String type) {
        return buildEdge(from, to, type, null);
    }

    private MtaGraphEdge buildEdge(String from, String to, String type, String name) {
        return ImmutableMtaGraphEdge.builder()
                                    .from(from)
                                    .to(to)
                                    .type(type)
                                    .name(name)
                                    .build();
    }

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid, String namespace, String name) {
        mtasApiServiceAuditLog.logGetMtas(SecurityContextUtil.getUsername(), spaceGuid, namespace, name);
        if (name == null && namespace == null) {
            return getAllMtas(spaceGuid);
        }

        if (namespace == null) {
            return getMtasByName(spaceGuid, name);
        }

        if (name == null) {
            return getMtasByNamespace(spaceGuid, namespace);
        }

        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(name, namespace, client);
        DeployedMta deployedMta = optionalDeployedMta.orElseThrow(
            () -> new NotFoundException(Messages.SPECIFIC_MTA_NOT_FOUND, name, namespace));

        return ResponseEntity.ok()
                             .body(Arrays.asList(getMta(deployedMta, client)));
    }

    protected ResponseEntity<List<Mta>> getAllMtas(String spaceGuid) {
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtas(client);

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas, client));
    }

    protected ResponseEntity<List<Mta>> getMtasByNamespace(String spaceGuid, String namespace) {
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasByNamespace(namespace, client);

        if (deployedMtas.isEmpty()) {
            throw new NotFoundException(Messages.MTAS_NOT_FOUND_BY_NAMESPACE, namespace);
        }

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas, client));
    }

    protected ResponseEntity<List<Mta>> getMtasByName(String spaceGuid, String name) {
        CloudControllerClient client = getCloudFoundryClient(spaceGuid);
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasByName(name, client);

        if (deployedMtas.isEmpty()) {
            throw new NotFoundException(Messages.MTAS_NOT_FOUND_BY_NAME, name);
        }

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas, client));
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClientWithNoCorrelation(userInfo.getId(), spaceGuid);
    }

    private List<Mta> getMtas(List<DeployedMta> deployedMtas, CloudControllerClient client) {
        return deployedMtas.stream()
                           .map(mta -> getMta(mta, client))
                           .collect(Collectors.toList());
    }

    private Mta getMta(DeployedMta mta, CloudControllerClient client) {
        return ImmutableMta.builder()
                           .metadata(getMetadata(mta.getMetadata()))
                           .modules(getModules(mta.getApplications(), client))
                           .services(mta.getServices()
                                        .stream()
                                        .map(DeployedMtaService::getName)
                                        .collect(Collectors.toSet()))
                           .build();
    }

    private List<Module> getModules(List<DeployedMtaApplication> deployedApplications, CloudControllerClient client) {
        return deployedApplications.stream()
                                   .map(app -> getModule(app, client))
                                   .collect(Collectors.toList());
    }

    private Module getModule(DeployedMtaApplication deployedMtaApplication, CloudControllerClient client) {
        var appRoutes = client.getApplicationRoutes(deployedMtaApplication.getGuid());
        return ImmutableModule.builder()
                              .appName(deployedMtaApplication.getName())
                              .moduleName(deployedMtaApplication.getModuleName())
                              .providedDendencyNames(deployedMtaApplication.getProvidedDependencyNames())
                              .uris(parseRoutesToUris(appRoutes))
                              .services(deployedMtaApplication.getBoundMtaServices())
                              .build();
    }

    private Metadata getMetadata(MtaMetadata metadata) {
        return ImmutableMetadata.builder()
                                .id(metadata.getId())
                                .version(getVersion(metadata.getVersion()))
                                .namespace(metadata.getNamespace())
                                .build();
    }

    private List<String> parseRoutesToUris(List<CloudRoute> routes) {
        return routes.stream()
                     .map(CloudRoute::getUrl)
                     .collect(Collectors.toList());
    }

    private String getVersion(Version version) {
        return version != null ? version.toString() : null;
    }

    @Override
    public ResponseEntity<MtaGraph> buildGraphFromDescriptor(String spaceGuid, String descriptorYaml) {
        DeploymentDescriptor descriptor = descriptorParserFacadeFactory.getInstance()
                                                                       .parseDeploymentDescriptor(descriptorYaml);
        return ResponseEntity.ok()
                             .body(buildGraphFromDescriptor(descriptor));
    }

    @Override
    public ResponseEntity<String> buildGraphFromDescriptorAsDot(String spaceGuid, String descriptorYaml) {
        DeploymentDescriptor descriptor = descriptorParserFacadeFactory.getInstance()
                                                                       .parseDeploymentDescriptor(descriptorYaml);
        MtaGraph graph = buildGraphFromDescriptor(descriptor);
        return ResponseEntity.ok()
                             .contentType(MediaType.TEXT_PLAIN)
                             .body(toDot(graph));
    }

    private String toDot(MtaGraph graph) {
        StringBuilder sb = new StringBuilder("digraph mta {\n  rankdir=LR;\n");
        for (MtaGraphNode node : graph.getNodes()) {
            String shape = "resource".equals(node.getType()) ? "cylinder" : "box";
            sb.append("  \"").append(node.getId()).append("\" [shape=").append(shape).append("];\n");
        }
        for (MtaGraphEdge edge : graph.getEdges()) {
            String label = edge.getName() != null ? edge.getType() + " " + edge.getName() : edge.getType();
            sb.append("  \"").append(edge.getFrom()).append("\" -> \"").append(edge.getTo())
              .append("\" [label=\"").append(label).append("\"];\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private MtaGraph buildGraphFromDescriptor(DeploymentDescriptor descriptor) {
        // provided-dep name → module name that provides it
        Map<String, String> providedDepToModule = new HashMap<>();
        for (org.cloudfoundry.multiapps.mta.model.Module module : descriptor.getModules()) {
            for (org.cloudfoundry.multiapps.mta.model.ProvidedDependency provided : module.getProvidedDependencies()) {
                providedDepToModule.put(provided.getName(), module.getName());
            }
        }

        // resource name → node (for quick lookup)
        java.util.Set<String> resourceNames = descriptor.getResources()
                                                        .stream()
                                                        .map(org.cloudfoundry.multiapps.mta.model.Resource::getName)
                                                        .collect(Collectors.toSet());

        List<MtaGraphNode> nodes = new ArrayList<>();
        for (org.cloudfoundry.multiapps.mta.model.Module module : descriptor.getModules()) {
            nodes.add(buildNode(module.getName(), "module"));
        }
        for (org.cloudfoundry.multiapps.mta.model.Resource resource : descriptor.getResources()) {
            nodes.add(buildNode(resource.getName(), "resource"));
        }

        List<MtaGraphEdge> edges = new ArrayList<>();
        for (org.cloudfoundry.multiapps.mta.model.Module module : descriptor.getModules()) {
            for (org.cloudfoundry.multiapps.mta.model.RequiredDependency req : module.getRequiredDependencies()) {
                String depName = req.getName();
                String providerModule = providedDepToModule.get(depName);
                if (providerModule != null) {
                    edges.add(buildEdge(providerModule, module.getName(), "provides", depName));
                } else if (resourceNames.contains(depName)) {
                    edges.add(buildEdge(module.getName(), depName, "requires-resource", depName));
                }
            }
        }

        return ImmutableMtaGraph.builder()
                                .nodes(nodes)
                                .edges(edges)
                                .build();
    }

}
