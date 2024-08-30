package org.cloudfoundry.multiapps.controller.process.util;

import java.io.InputStream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NamespaceGlobalParametersTest {

    public DeploymentDescriptor descriptor;
    private NamespaceGlobalParameters namespaceGlobalParameters;

    @Test
    void testApplyNamespaceParametersWhenAllTruePassed() {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor("namespace-global-parameters-with-all-true-values.yaml");
        namespaceGlobalParameters = new NamespaceGlobalParameters(deploymentDescriptor);
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceAppNamesParameter());
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceServiceNamesParameter());
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceAppRoutesParameter());
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceAsSuffix());
    }

    @Test
    void testApplyNamespaceParametersWhenAllFalsePassed() {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor("namespace-global-parameters-with-all-false-values.yaml");
        namespaceGlobalParameters = new NamespaceGlobalParameters(deploymentDescriptor);
        Assertions.assertFalse(namespaceGlobalParameters.getApplyNamespaceAppNamesParameter());
        Assertions.assertFalse(namespaceGlobalParameters.getApplyNamespaceServiceNamesParameter());
        Assertions.assertFalse(namespaceGlobalParameters.getApplyNamespaceAppRoutesParameter());
        Assertions.assertFalse(namespaceGlobalParameters.getApplyNamespaceAsSuffix());
    }

    @Test
    void testApplyNamespaceParametersWithDefaultParameters() {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor("namespace-global-parameters-with-default-values.yaml");
        namespaceGlobalParameters = new NamespaceGlobalParameters(deploymentDescriptor);
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceServiceNamesParameter());
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceAppNamesParameter());
        Assertions.assertTrue(namespaceGlobalParameters.getApplyNamespaceAppRoutesParameter());
        Assertions.assertFalse(namespaceGlobalParameters.getApplyNamespaceAsSuffix());
    }

    @Test
    void testApplyNamespaceParametersWithoutParameters() {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor("namespace-global-parameters-with-incorrect-values.yaml");
        namespaceGlobalParameters = new NamespaceGlobalParameters(deploymentDescriptor);
        Assertions.assertThrows(ContentException.class, () -> namespaceGlobalParameters.getApplyNamespaceServiceNamesParameter());
        Assertions.assertThrows(ContentException.class, () -> namespaceGlobalParameters.getApplyNamespaceAppNamesParameter());
        Assertions.assertThrows(ContentException.class, () -> namespaceGlobalParameters.getApplyNamespaceAppRoutesParameter());
        Assertions.assertThrows(ContentException.class, () -> namespaceGlobalParameters.getApplyNamespaceAsSuffix());
    }

    private DeploymentDescriptor parseDeploymentDescriptor(String descriptorLocation) {
        DescriptorParserFacade parser = new DescriptorParserFacade();
        InputStream inputStreamDescriptor = getClass().getResourceAsStream(descriptorLocation);
        return parser.parseDeploymentDescriptor(inputStreamDescriptor);
    }
}
