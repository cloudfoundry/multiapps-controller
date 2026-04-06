package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

abstract class CustomControllerClientBaseTest {

    private static final Pattern URI_VARIABLE_PATTERN = Pattern.compile("\\{[^}]+}");

    @Mock
    protected WebClientFactory webClientFactory;
    @Mock
    protected ApplicationConfiguration applicationConfiguration;
    @Mock
    protected WebClient webClient;

    @SuppressWarnings("rawtypes")
    protected final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
    @SuppressWarnings("rawtypes")
    protected final WebClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
    @Mock
    protected WebClient.ResponseSpec responseSpec;

    protected final List<String> capturedResolvedUris = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected void stubWebClientToReturnResponse(String... responses) {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenAnswer(this::resolveUriTemplateAndReturn);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString(), Mockito.<Object[]> any()))
               .thenAnswer(this::resolveUriTemplateAndReturn);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);

        if (responses.length == 0) {
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just("{\"resources\":[],\"pagination\":{\"next\":null}}"));
            return;
        }

        if (responses.length == 1) {
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just(responses[0]));
            return;
        }
        @SuppressWarnings("rawtypes") Mono[] remaining = new Mono[responses.length - 1];
        for (int i = 1; i < responses.length; i++) {
            remaining[i - 1] = Mono.just(responses[i]);
        }
        Mockito.when(responseSpec.bodyToMono(String.class))
               .thenReturn(Mono.just(responses[0]), remaining);
    }

    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec resolveUriTemplateAndReturn(InvocationOnMock invocation) {
        String uriTemplate = invocation.getArgument(0);
        Object[] allArgs = invocation.getArguments();
        Object[] uriVariables;
        if (allArgs.length > 1 && allArgs[1] instanceof Object[]) {
            uriVariables = (Object[]) allArgs[1];
        } else {
            uriVariables = new Object[allArgs.length - 1];
            System.arraycopy(allArgs, 1, uriVariables, 0, allArgs.length - 1);
        }
        String resolved = resolveTemplate(uriTemplate, uriVariables);
        capturedResolvedUris.add(resolved);
        return requestHeadersSpec;
    }

    private String resolveTemplate(String uriTemplate, Object[] uriVariables) {
        if (uriVariables == null || uriVariables.length == 0) {
            return uriTemplate;
        }
        Matcher matcher = URI_VARIABLE_PATTERN.matcher(uriTemplate);
        StringBuilder sb = new StringBuilder();
        int varIndex = 0;
        while (matcher.find() && varIndex < uriVariables.length) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(uriVariables[varIndex++])));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
