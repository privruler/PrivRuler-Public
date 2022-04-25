package com.privruler.tools;

import android.content.Context;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSConstants;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlibabaCredProfile {
    public String pkgName;

    public Set<String> accessKeyIds;
    public Set<String> secretKeyIds;
    public Set<String> bucketNames;
    public Set<String> endpoints;
    public Set<String> queueNames;
    public Set<String> authServerUrls;
    public Set<String> securityTokens;
    public Set<String> accessKeySecrets;

    public Set<String> cloudAPIs;
    public Map<String, Set<String>> capabilityMap;
    public List<String> extraCapability;

    private static List<String> CAPABILITES = Arrays.asList(new String[]{
            "listBuckets",
            "listObjects",
            "getObject",
            "putObject",
            "deleteObject"
    });

    public AlibabaCredProfile(String pkgName) {
        this.pkgName = pkgName;
        this.accessKeyIds = new HashSet<String>();
        this.secretKeyIds = new HashSet<String>();
        this.bucketNames = new HashSet<String>();
        this.endpoints = new HashSet<String>();
        this.queueNames = new HashSet<String>();
        this.authServerUrls = new HashSet<String>();
        this.securityTokens = new HashSet<String>();
        this.accessKeySecrets = new HashSet<String>();
        this.cloudAPIs = new HashSet<String>();

        this.extraCapability = new ArrayList<String>();
        this.capabilityMap = new HashMap<String, Set<String>>();
        // initialize capability for S3 and SNS
        for (String capability : CAPABILITES) {
            this.capabilityMap.put(capability, new HashSet<String>());
        }
    }

    public static List<String> ALIYUN_REGIONS = Arrays.asList(new String[] { OSSConstants.DEFAULT_OSS_ENDPOINT,
            "http://oss-cn-qingdao.aliyuncs.com", "http://oss-cn-beijing.aliyuncs.com",
            "http://oss-cn-chengdu.aliyuncs.com", "http://oss-cn-hangzhou.aliyuncs.com",
            "http://oss-cn-shanghai.aliyuncs.com", "http://oss-cn-shenzhen.aliyuncs.com",
            "http://oss-cn-heyuan.aliyuncs.com", "http://oss-cn-wulanchabu.aliyuncs.com",
            "http://oss-cn-hongkong.aliyuncs.com", "http://oss-ap-southeast-1.aliyuncs.com",
            "http://oss-ap-northeast-1.aliyuncs.com", "http://oss-eu-west-1.aliyuncs.com",
            "http://oss-us-west-1.aliyuncs.com", "http://oss-me-east-1.aliyuncs.com",
            "http://oss-ap-south-1.aliyuncs.com", "http://oss-cn-hzjbp-b-console.aliyuncs.com",
            "http://oss-cn-shanghai-finance-1-internal.aliyuncs.com",
            "http://oss-cn-shenzhen-finance-1-internal.aliyuncs.com" });

    public static boolean isAliyunKeyId16(String str) {
        Pattern p = Pattern.compile("^[0-9a-zA-Z/+]{16}$");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isAliyunKeyId24(String str) {
        Pattern p = Pattern.compile("^[0-9a-zA-Z/+]{24}$");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isAliyunKeySecret(String str) {
        Pattern p = Pattern.compile("^[0-9a-zA-Z/+]{30}$");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isAliyunEndpoint(String str) {
        return str.contains("aliyuncs.com");
    }

    public void getExtraCapabilities() {
        for (String capability : CAPABILITES) {
            if (this.capabilityMap.get(capability).size() <= 0) {
                continue;
            }

            boolean capabilityUsed = false;
            for (String api : this.cloudAPIs) {
                String apiSubSig = " " + capability + "(";
                if (api.contains(apiSubSig)) {
                    capabilityUsed = true;
                    break;
                }
            }

            if (!capabilityUsed) {
                this.extraCapability.add(capability);
            }
        }
    }

    public Set<OSSClient> createOSSClients(Context context) {
        Set<OSSClient> ossClients = new HashSet<OSSClient>();
        if (this.accessKeyIds.size() < 1 || this.secretKeyIds.size() < 1) {
            return ossClients;
        }

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // connction time out default 15s
        conf.setSocketTimeout(15 * 1000); // socket timeout，default 15s
        conf.setMaxConcurrentRequest(5); // synchronous request number，default 5
        conf.setMaxErrorRetry(2); // retry，default 2
        OSSLog.enableLog(); //write local log file, path is SDCard_path\OSSLog\logs.csv

        for (String accessKeyId : this.accessKeyIds) {
            for (String secretKeyId : this.secretKeyIds) {
                Set<String> endpoints = new HashSet<String>();
                if (this.endpoints.size() > 0) {
                    endpoints.addAll(this.endpoints);
                } else {
                    endpoints.addAll(ALIYUN_REGIONS);
                }

                for (String endpoint : endpoints) {
                    try {
                        OSSPlainTextAKSKCredentialProvider provider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, secretKeyId);
                        OSSClient client = new OSSClient(context, endpoint, provider, conf);
                        ossClients.add(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return ossClients;
    }
}