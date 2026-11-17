package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;

public class CloudControllerRootDocumentClient {

    private CloudControllerRootDocumentClient() {
    }

    public static Map<String, Object> getRootDocumentLinks(URL controllerUrl, Map<String, String> requestTags) {
        try (HttpClient httpClient = createHttpClient()) {
            HttpRequest request = buildRootDocumentRequest(controllerUrl, requestTags);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return extractRootDocumentLinks(controllerUrl, response);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();

            throw new CloudException(MessageFormat.format(Messages.INTERRUPTED_WHILE_CALLING_THE_CF_ROOT_URL_AT_0, controllerUrl), e);
        } catch (Exception e) {
            throw new CloudException(MessageFormat.format(Messages.FAILED_TO_CALL_THE_CF_ROOT_AT_0_WITH_1, controllerUrl, e.getMessage()),
                                     e);
        }
    }

    private static HttpRequest buildRootDocumentRequest(URL controllerUrl, Map<String, String> requestTags) throws URISyntaxException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                        .GET()
                                                        .uri(controllerUrl.toURI())
                                                        .timeout(Constants.DEFAULT_CONNECT_TIMEOUT);

        requestTags.forEach(requestBuilder::header);

        return requestBuilder.build();
    }

    private static Map<String, Object> extractRootDocumentLinks(URL controllerUrl, HttpResponse<String> response) {
        if (!isSuccessful(response.statusCode())) {
            throw new CloudException(
                MessageFormat.format(Messages.CF_ROOT_DOCUMENT_REQUEST_TO_0_RETURNED_1, controllerUrl, response.statusCode()));
        }

        return MiscUtil.cast(JsonUtil.convertJsonToMap(response.body())
                                     .get(Constants.ROOT_DOCUMENT_LINKS_LIST));
    }

    private static boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                         .connectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT)
                         .build();
    }

}


