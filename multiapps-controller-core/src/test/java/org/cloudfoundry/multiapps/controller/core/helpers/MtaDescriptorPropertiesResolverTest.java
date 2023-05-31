package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableMtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.model.MtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MtaDescriptorPropertiesResolverTest {

    static Stream<Arguments> testResolve() {
        return Stream.of(Arguments.of("mtad-properties-resolver-test/mtad-with-route.yaml",
                                      new Expectation(Expectation.Type.JSON, "mtad-properties-resolver-test/mtad-with-route-result.json")),
                         Arguments.of("mtad-properties-resolver-test/mtad-with-domain.yaml",
                                      new Expectation(Expectation.Type.JSON, "mtad-properties-resolver-test/mtad-with-domain-result.json")),
                         Arguments.of("mtad-properties-resolver-test/mtad-with-escaped-references.yaml",
                                      new Expectation(Expectation.Type.JSON,
                                                      "mtad-properties-resolver-test/mtad-with-escaped-references.json")),
                         Arguments.of("mtad-properties-resolver-test/mtad-with-service-guid.yaml",
                                      new Expectation(Expectation.Type.JSON,
                                                      "mtad-properties-resolver-test/mtad-with-service-guid-result.json")));
    }

    private final Tester tester = Tester.forClass(getClass());

    private MtaDescriptorPropertiesResolver resolver;

    @Mock
    private BiFunction<String, String, String> spaceIdSupplier;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private ConfigurationEntryService configurationEntryService;

    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(spaceIdSupplier.apply(anyString(), anyString())).thenReturn("");

        resolver = new MtaDescriptorPropertiesResolver(buildMtaDescriptorPropertiesResolverContext());
    }

    private MtaDescriptorPropertiesResolverContext buildMtaDescriptorPropertiesResolverContext() {
        return ImmutableMtaDescriptorPropertiesResolverContext.builder()
                                                              .handlerFactory(CloudHandlerFactory.forSchemaVersion(2))
                                                              .configurationEntryService(configurationEntryService)
                                                              .cloudTarget(new CloudTarget("", ""))
                                                              .currentSpaceId("")
                                                              .applicationConfiguration(configuration)
                                                              .namespace(null)
                                                              .applyNamespace(false)
                                                              .shouldReserveTemporaryRoute(false)
                                                              .build();
    }

    @ParameterizedTest(name = "{index}: \"{1}.\"")
    @MethodSource
    void testResolve(String descriptorFile, Expectation expectation) {
        DeploymentDescriptor descriptor = DescriptorTestUtil.loadDeploymentDescriptor(descriptorFile, getClass());

        tester.test(() -> resolver.resolve(descriptor), expectation);
    }

}
