package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.util.JsonUtil;

class ConfigurationEntryDynamicParameterResolverTest {

    private final static String TARGET_SPACE = "space";
    private final static String TARGET_ORG = "org";
    private final static String PROVIDER_ID = "provider-id";
    private final static String PROVIDER_NAMESPACE = "providerNamespace";
    private final static String SPACE_ID = "spaceId";
    private final static String CONTENT_ID = "contentId";

    static Stream<Arguments> testDynamicParameterResolve() {
        return Stream.of(Arguments.of(List.of(createEntry(1, JsonUtil.convertToJson(Map.of("dynamicGuid", "{ds/service-1/service-guid}")))),
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .relationshipEntityName("service-1")
                                                                                .parameterName("service-guid")
                                                                                .value("resolved-guid")
                                                                                .build()),
                                      List.of(createEntry(1, JsonUtil.convertToJson(Map.of("dynamicGuid", "resolved-guid"))))),

                         Arguments.of(List.of(createEntry(1, JsonUtil.convertToJson(Map.of("dynamicGuid", "{ds/service-1/service-guid}"))),
                                              createEntry(2, JsonUtil.convertToJson(Map.of("test-property", "should-not-be-resolved")))),
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .relationshipEntityName("service-1")
                                                                                .parameterName("service-guid")
                                                                                .value("resolved-guid")
                                                                                .build()),
                                      List.of(createEntry(1, JsonUtil.convertToJson(Map.of("dynamicGuid", "resolved-guid"))),
                                              createEntry(2, JsonUtil.convertToJson(Map.of("test-property", "should-not-be-resolved"))))),
                         Arguments.of(List.of(createEntry(1,
                                                          JsonUtil.convertToJson(Map.of("dynamicGuid", "{ds/service-1/service-guid}",
                                                                                        "test-property", "test-value")))),
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .relationshipEntityName("service-1")
                                                                                .parameterName("service-guid")
                                                                                .value("resolved-guid")
                                                                                .build()),
                                      List.of(createEntry(1, JsonUtil.convertToJson(Map.of("test-property", "test-value", "dynamicGuid",
                                                                                           "resolved-guid"))))));
    }

    @ParameterizedTest
    @MethodSource
    void testDynamicParameterResolve(List<ConfigurationEntry> configurationEntriesToResolve,
                                     Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                     List<ConfigurationEntry> expectedConfigurationEntries) {
        ConfigurationEntryDynamicParameterResolver dynamicParameterResolver = new ConfigurationEntryDynamicParameterResolver();
        List<ConfigurationEntry> resolvedEntries = dynamicParameterResolver.resolveDynamicParametersOfConfigurationEntries(configurationEntriesToResolve,
                                                                                                                           dynamicResolvableParameters);
        assertEquals(expectedConfigurationEntries.size(), resolvedEntries.size());
        for (ConfigurationEntry expectedEntry : expectedConfigurationEntries) {
            ConfigurationEntry resolvedEntry = resolvedEntries.stream()
                                                              .filter(entry -> entry.getId() == expectedEntry.getId())
                                                              .findFirst()
                                                              .orElseThrow(() -> new NoSuchElementException(MessageFormat.format("Configuration entry \"{0}\" is not found after resolve",
                                                                                                                                 expectedEntry.getConfigurationName())));

            Map<String, Object> expectedEntriesMap = JsonUtil.convertJsonToMap(expectedEntry.getContent());
            Map<String, Object> resovledEntriesMap = JsonUtil.convertJsonToMap(resolvedEntry.getContent());

            assertEquals(expectedEntriesMap, resovledEntriesMap);
        }

    }

    private static ConfigurationEntry createEntry(long id, String content) {
        return new ConfigurationEntry(id,
                                      ConfigurationEntriesUtil.PROVIDER_NID,
                                      PROVIDER_ID,
                                      Version.parseVersion("3.0.0"),
                                      PROVIDER_NAMESPACE,
                                      new CloudTarget(TARGET_ORG, TARGET_SPACE),
                                      content,
                                      Collections.emptyList(),
                                      SPACE_ID,
                                      CONTENT_ID);
    }

}
