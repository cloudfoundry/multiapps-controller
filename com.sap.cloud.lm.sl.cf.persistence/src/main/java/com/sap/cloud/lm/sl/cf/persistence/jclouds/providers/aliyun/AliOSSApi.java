package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.google.common.base.Supplier;
import org.jclouds.domain.Credentials;
import org.jclouds.location.Provider;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AliOSSApi {

    public static final String API_ID = "aliyun-oss";

    private final String identity;
    private final String credential;
    private AliOSSRegion defaultRegion;

    @Inject
    public AliOSSApi(@Provider Supplier<Credentials> credsSupplier, AliOSSRegion defaultRegion) {
        Credentials credentials = credsSupplier.get();
        this.identity = credentials.identity;
        this.credential = credentials.credential;
        this.defaultRegion = defaultRegion;
    }

    public OSS getOSSClient() {
        return new OSSClientBuilder().build(defaultRegion.getEndpoint(), identity, credential);
    }

    public void setDefaultRegion(AliOSSRegion aliOSSRegion) {
        this.defaultRegion = aliOSSRegion;
    }
}
