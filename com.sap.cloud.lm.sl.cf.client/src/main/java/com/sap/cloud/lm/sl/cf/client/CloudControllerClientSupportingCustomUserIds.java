package com.sap.cloud.lm.sl.cf.client;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;

public interface CloudControllerClientSupportingCustomUserIds extends CloudControllerClient {

    List<String> getSpaceAuditorIdsAsStrings(String spaceName);

    List<String> getSpaceManagerIdsAsStrings(String spaceName);

    List<String> getSpaceDeveloperIdsAsStrings(String spaceName);

    List<String> getSpaceAuditorIdsAsStrings(UUID spaceGuid);

    List<String> getSpaceManagerIdsAsStrings(UUID spaceGuid);

    List<String> getSpaceDeveloperIdsAsStrings(UUID spaceGuid);

    List<String> getSpaceAuditorIdsAsStrings(String orgName, String spaceName);

    List<String> getSpaceManagerIdsAsStrings(String orgName, String spaceName);

    List<String> getSpaceDeveloperIdsAsStrings(String orgName, String spaceName);

}
