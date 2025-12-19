package org.cloudfoundry.multiapps.controller.process.security.store;

public interface SecretTokenStore extends SecretTokenStoreDeletion {

    long put(String processInstanceId, String variableName, String plainText);

    String get(String processInstanceId, long id);

}
