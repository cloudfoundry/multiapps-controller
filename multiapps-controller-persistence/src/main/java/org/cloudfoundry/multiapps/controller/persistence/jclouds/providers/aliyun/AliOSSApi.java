package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.google.common.base.Supplier;
import org.jclouds.domain.Credentials;
import org.jclouds.location.Provider;
import org.jclouds.location.suppliers.ProviderURISupplier;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AliOSSApi {

    public static final String API_ID = "aliyun-oss";

    private final String identity;
    private final String credential;
    private final String endpoint;

    @Inject
    public AliOSSApi(@Provider Supplier<Credentials> credsSupplier, ProviderURISupplier providerURISupplier) {
        Credentials credentials = credsSupplier.get();
        this.identity = credentials.identity;
        this.credential = credentials.credential;
        this.endpoint = providerURISupplier.get()
                                           .toString();
    }

    public OSS getOSSClient() {
        return new OSSClientBuilder().build(endpoint, identity, credential);
    }
}
