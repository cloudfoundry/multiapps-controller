package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

@Named
public class DependentModuleStopResolver {

    private static final int DEPLOYED_AFTER_MIN_SCHEMA_VERSION = 3;

    public List<Module> resolveDependentModulesToStop(ProcessContext context, Module root) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (!isDependencyAwareStopOrderEnabled(context, descriptor)) {
            return Collections.emptyList();
        }
        if (!Boolean.TRUE.equals(context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY))) {
            context.getStepLogger()
                   .warn(Messages.BLUE_GREEN_SKIPPING_DEPENDENCY_ORDER_STOP);
            return Collections.emptyList();
        }
        Map<String, Module> modulesByName = getModulesByName(descriptor);
        Map<String, List<Module>> modulesDependentOn = buildModulesDependentOn(modulesByName.values(), context.getStepLogger());

        List<Module> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        collectModulesDependentOnPostOrder(root.getName(), modulesDependentOn, visited, result);

        result.remove(root);
        return result;
    }

    private boolean isDependencyAwareStopOrderEnabled(ProcessContext context, DeploymentDescriptor descriptor) {
        boolean isExplicitlySetFromContext = VariableHandling.get(context.getExecution(), Variables.STOP_ORDER_IS_DEPENDENCY_AWARE);
        if (isExplicitlySetFromContext) {
            return true;
        }
        return (boolean) PropertiesUtil.getPropertyValue(List.of(descriptor.getParameters()),
                                                         SupportedParameters.BG_DEPENDENCY_AWARE_STOP_ORDER, false);
    }

    private Map<String, Module> getModulesByName(DeploymentDescriptor descriptor) {
        return descriptor.getModules()
                         .stream()
                         .collect(Collectors.toMap(
                             Module::getName,
                             Function.identity()
                         ));
    }

    private Map<String, List<Module>> buildModulesDependentOn(Collection<Module> modules, StepLogger logger) {
        return modules.stream()
                      .filter(module -> supportsDeployedAfter(module, logger))
                      .flatMap(this::toDependentEntries)
                      .collect(groupByDependency());
    }

    private boolean supportsDeployedAfter(Module module, StepLogger logger) {
        if (module.getMajorSchemaVersion() >= DEPLOYED_AFTER_MIN_SCHEMA_VERSION) {
            return true;
        }
        logger.warn(
            Messages.UNSUPPORTED_DEPLOYED_AFTER_SCHEMA_VERSION_WARNING,
            module.getName(),
            module.getMajorSchemaVersion(), DEPLOYED_AFTER_MIN_SCHEMA_VERSION);
        return false;
    }

    private Stream<Map.Entry<String, Module>> toDependentEntries(Module module) {
        List<String> deployedAfter = module.getDeployedAfter();
        if (deployedAfter == null || deployedAfter.isEmpty()) {
            return Stream.empty();
        }

        return deployedAfter.stream()
                            .map(dependencyName -> Map.entry(dependencyName, module));
    }

    private Collector<Map.Entry<String, Module>, ?, Map<String, List<Module>>> groupByDependency() {
        return Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        );
    }

    private void collectModulesDependentOnPostOrder(String moduleName, Map<String, List<Module>> modulesDependentOn, Set<String> visited,
                                                    List<Module> result) {
        for (Module dependent : modulesDependentOn.getOrDefault(moduleName, List.of())) {
            String dependentName = dependent.getName();

            if (!visited.add(dependentName)) {
                continue;
            }

            collectModulesDependentOnPostOrder(dependentName, modulesDependentOn, visited, result);
            result.add(dependent);
        }
    }

}
