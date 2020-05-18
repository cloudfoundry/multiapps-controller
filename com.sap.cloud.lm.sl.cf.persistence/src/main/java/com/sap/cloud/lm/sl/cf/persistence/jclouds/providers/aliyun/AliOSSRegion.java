package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun;

import java.text.MessageFormat;

public enum AliOSSRegion {

    EU_CENTRAL_1("oss-eu-central-1"),
    CN_HANGZHOU("oss-cn-hangzhou"),
    CN_SHANGHAI("oss-cn-shanghai"),
    CN_QINGDAO("oss-cn-qingdao"),
    CN_BEIJING("oss-cn-beijing"),
    CN_ZHANGJIAKOU("oss-cn-zhangjiakou"),
    CN_HUHEHAOTE("oss-cn-huhehaote"),
    CN_SHENZHEN("oss-cn-shenzhen"),
    CN_HEYUAN("oss-cn-heyuan"),
    CN_CHENGDU("oss-cn-chengdu"),
    CH_HONGKONG("oss-cn-hongkong"),
    US_WEST_1("oss-us-west-1"),
    US_EAST_1("oss-us-east-1"),
    AP_SOUTHEAST_1("oss-ap-southeast-1"),
    AP_SOUTHEAST_2("oss-ap-southeast-2"),
    AP_SOUTHEAST_3("oss-ap-southeast-3"),
    AP_SOUTHEAST_5("oss-ap-southeast-5"),
    AP_NORTHEAST_1("oss-ap-northeast-1"),
    AP_SOUTH_1("oss-ap-south-1"),
    EU_WEST_1("oss-eu-west-1"),
    ME_EAST_1("oss-me-east-1");


    static final String ALI_OSS_DOMAIN = "aliyuncs.com";
    private final String region;

    AliOSSRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return MessageFormat.format("http://{0}.{1}", region, ALI_OSS_DOMAIN);
    }
}
