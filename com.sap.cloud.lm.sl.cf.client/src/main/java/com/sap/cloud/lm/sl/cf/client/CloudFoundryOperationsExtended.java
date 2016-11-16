package com.sap.cloud.lm.sl.cf.client;

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;

public interface CloudFoundryOperationsExtended extends CloudFoundryOperations {

    /**
     * Get list of space manager user id for the space.
     *
     * @param spaceName name of the space
     * @return List of space manager user id
     */
    List<String> getSpaceManagers2(String spaceName);

    /**
     * Get list of space developer user id for the space.
     *
     * @param spaceName name of the space
     * @return List of space developer user id
     */
    List<String> getSpaceDevelopers2(String spaceName);

    /**
     * Get list of space auditor user id for the space.
     *
     * @param spaceName name of the space
     * @return List of space auditor user id
     */
    List<String> getSpaceAuditors2(String spaceName);

    /**
     * Get list of space manager user id for the space.
     *
     * @param orgName name of the organization containing the space
     * @param spaceName name of the space
     * @return List of space manager user id
     */
    List<String> getSpaceManagers2(String orgName, String spaceName);

    /**
     * Get list of space developer user id for the space.
     *
     * @param orgName name of the organization containing the space
     * @param spaceName name of the space
     * @return List of space developer user id
     */
    List<String> getSpaceDevelopers2(String orgName, String spaceName);

    /**
     * Get list of space auditor user id for the space.
     *
     * @param orgName name of the organization containing the space
     * @param spaceName name of the space
     * @return List of space auditor user id
     */
    List<String> getSpaceAuditors2(String orgName, String spaceName);

}
