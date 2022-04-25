package com.privruler.tools;

import android.content.Context;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AWSCredProfile {
    public String pkgName;
    public Set<String> identityPoolIds;
    public String accountId;
    public String unauthRoleArn;
    public String authRoleArn;
    public Set<Regions> regions;
    public Set<String> accessKeys;
    public Set<String> secretKeys;
    public Set<String> bucketNames;
    public Set<String> platformAppArns;
    public Set<String> topicArns;
    public Set<String> cloudAPIs;

    public Map<String, Set<String>> capabilityMap;
    public List<String> extraCapability;

    private static List<String> CAPABILITES = Arrays.asList(new String[]{
            "listBuckets",
            "listObjects",
            "getObject",
            "putObject",
            "deleteObject",
            "listPlatformApplications",
            "listEndpointsByPlatformApplication",
            "listTopics",
            "listSubscriptionsByTopic",
            "getTopicAttributes",
            "getPlatformApplicationAttributes",
            "getSubscriptionAttributes",
            "createTopic",
            "deleteTopic",
            "listSubscriptions",
            "checkIfPhoneNumberIsOptedOut",
            "getEndpointAttributes",
            "getSMSAttributes",
            "listPhoneNumbersOptedOut",
            "confirmSubscription",
            "createPlatformApplication",
            "createPlatformEndpoint",
            "deleteEndpoint",
            "optInPhoneNumber",
            "publish",
            "setEndpointAttributes",
            "setPlatformApplicationAttributes",
            "setSMSAttributes",
            "setSubscriptionAttributes",
            "setTopicAttributes",
            "subscribe",
            "unsubscribe",
            "addPermission",
            "removePermission",
            "deletePlatformApplication"
    });

    public AWSCredProfile(String pkgName) {
        this.pkgName = pkgName;
        this.identityPoolIds = new HashSet<String>();
        this.accessKeys = new HashSet<String>();
        this.secretKeys = new HashSet<String>();
        this.extraCapability = new ArrayList<String>();
        this.cloudAPIs = new HashSet<String>();
        this.bucketNames = new HashSet<String>();
        this.platformAppArns = new HashSet<String>();
        this.topicArns = new HashSet<String>();
        this.regions = new HashSet<Regions>();

        this.capabilityMap = new HashMap<String, Set<String>>();
        // initialize capability for S3 and SNS
        for (String capability : CAPABILITES) {
            this.capabilityMap.put(capability, new HashSet<String>());
        }
    }

    public static Regions toRegions(String capi, String cconst) {
        if (capi.contains("US_EAST_1") || cconst.contains("us-east-1")) {
            return Regions.US_EAST_1;
        }

        if (capi.contains("US_EAST_2") || cconst.contains("us-east-2")) {
            return Regions.US_EAST_2;
        }

        if (capi.contains("SA_EAST_1") || cconst.contains("sa-east-1")) {
            return Regions.SA_EAST_1;
        }

        if (capi.contains("ME_SOUTH_1") || cconst.contains("me-source-1")) {
            return Regions.ME_SOUTH_1;
        }

        if (capi.contains("GovCloud")) {
            return Regions.GovCloud;
        }

        if (capi.contains("EU_CENTRAL_1") || cconst.contains("eu-central-1")) {
            return Regions.EU_CENTRAL_1;
        }

        if (capi.contains("DEFAULT_REGION")) {
            return Regions.DEFAULT_REGION;
        }

        if (capi.contains("CN_NORTH_1") || cconst.contains("cn-north-1")) {
            return Regions.CN_NORTH_1;
        }

        if (capi.contains("CA_CENTRAL_1") || cconst.contains("ca-central-1")) {
            return Regions.CA_CENTRAL_1;
        }

        if (capi.contains("US_GOV_EAST_1") || cconst.contains("us-gov-east-1")) {
            return Regions.US_GOV_EAST_1;
        }

        if (capi.contains("US_WEST_1") || cconst.contains("us-west-1")) {
            return Regions.US_WEST_1;
        }

        if (capi.contains("US_WEST_2") || cconst.contains("us-west-2")) {
            return Regions.US_WEST_2;
        }

        if (capi.contains("EU_NORTH_1") || cconst.contains("eu-north-1")) {
            return Regions.EU_NORTH_1;
        }

        if (capi.contains("EU_WEST_1") || cconst.contains("eu-west-1")) {
            return Regions.EU_WEST_1;
        }

        if (capi.contains("EU_WEST_2") || cconst.contains("eu-west-2")) {
            return Regions.EU_WEST_2;
        }

        if (capi.contains("EU_WEST_3") || cconst.contains("eu-west-3")) {
            return Regions.EU_WEST_3;
        }

        if (capi.contains("CN_NORTHWEST_1") || cconst.contains("cn-northwest-1")) {
            return Regions.CN_NORTHWEST_1;
        }

        if (capi.contains("AP_EAST_1") || cconst.contains("ap-east-1")) {
            return Regions.AP_EAST_1;
        }

        if (capi.contains("AP_SOUTH_1") || cconst.contains("ap-south-1")) {
            return Regions.AP_SOUTH_1;
        }

        if (capi.contains("AP_NORTHEAST_1") || cconst.contains("ap-northeast-1")) {
            return Regions.AP_NORTHEAST_1;
        }

        if (capi.contains("AP_NORTHEAST_2") || cconst.contains("ap-northeast-2")) {
            return Regions.AP_NORTHEAST_2;
        }

        if (capi.contains("AP_SOUTHEAST_1") || cconst.contains("ap-southeast-1")) {
            return Regions.AP_SOUTHEAST_1;
        }

        if (capi.contains("AP_SOUTHEAST_2") || cconst.contains("ap-southeast-2")) {
            return Regions.AP_SOUTHEAST_2;
        }

        return null;
    }

    public static boolean isIdentityPoolId(String str) {
        Pattern p = Pattern.compile("[\\w-]+:[0-9a-f-]+");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }

        // sometimes the identity pool comes without region
        p = Pattern.compile("[0-9a-f-]{36}");
        m = p.matcher(str);
        if (m.find()) {
            return true;
        }

        return false;
    }

    public static boolean isAccountId(String str) {
        Pattern p = Pattern.compile("\\d{12}");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isUnAuthRoleArn(String str) {
        return str.startsWith("arn:aws:iam::") && str.contains(":role/") && str.contains("Unauth");
    }

    public static boolean isAuthRoleArn(String str) {
        return str.startsWith("arn:aws:iam::") && str.contains(":role/") && str.contains("Auth") && !str.contains("Unauth");
    }

    public static boolean isAccessKey(String str) {
        Pattern p = Pattern.compile("AKIA[0-9A-Z]{16}");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isSecretKey(String str) {
        Pattern p = Pattern.compile("[0-9a-zA-Z/+]{40}");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
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

                if (capability.contains("getObject") && api.contains("TransferUtility:") && api.contains("download(")) {
                    capabilityUsed = true;
                    break;
                }

                if (capability.contains("putObject") && api.contains("TransferUtility:") && api.contains("upload(")) {
                    capabilityUsed = true;
                    break;
                }
            }

            if (!capabilityUsed) {
                this.extraCapability.add(capability);
            }
        }
    }

    public Set<AmazonS3Client> createS3Clients(Context context) {
        Set<AmazonS3Client> s3Clients = new HashSet<AmazonS3Client>();
        try {
            // cognito identity service
            for (String identityPoolId : this.identityPoolIds) {
                if (this.accountId != null && this.unauthRoleArn != null && this.authRoleArn != null) {
                    if (this.regions.size() < 1) {
                        AmazonS3Client s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(context,
                                this.accountId,
                                identityPoolId,
                                this.unauthRoleArn,
                                this.authRoleArn,
                                Regions.DEFAULT_REGION));
                        s3Clients.add(s3Client);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonS3Client s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(context,
                                    this.accountId,
                                    identityPoolId,
                                    this.unauthRoleArn,
                                    this.authRoleArn,
                                    region));
                            s3Clients.add(s3Client);
                        }
                    }
                } else {
                    if (this.regions.size() < 1) {
                        AmazonS3Client s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(context,
                                identityPoolId, Regions.DEFAULT_REGION));
                        s3Clients.add(s3Client);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonS3Client s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(context,
                                    identityPoolId, region));
                            s3Clients.add(s3Client);
                        }
                    }
                }
            }

            // ak, sk
            for (String accessKey : this.accessKeys) {
                for (String secretKey : this.secretKeys) {
                    if (this.regions.size() < 1) {
                        AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
                        s3Client.setRegion(Region.getRegion(Regions.DEFAULT_REGION));
                        s3Clients.add(s3Client);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
                            s3Client.setRegion(Region.getRegion(region));
                            s3Clients.add(s3Client);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s3Clients;
    }

    public Set<AmazonSNSClient> createSNSClients(Context context) {
        Set<AmazonSNSClient> snsClients = new HashSet<AmazonSNSClient>();
        try {
            // cognito identity service
            for (String identityPoolId : this.identityPoolIds) {
                if (this.accountId != null && this.unauthRoleArn != null && this.authRoleArn != null) {
                    if (this.regions.size() < 1) {
                        AmazonSNSClient snsClient = new AmazonSNSClient(new CognitoCachingCredentialsProvider(context,
                                this.accountId,
                                identityPoolId,
                                this.unauthRoleArn,
                                this.authRoleArn,
                                Regions.DEFAULT_REGION));
                        snsClients.add(snsClient);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonSNSClient snsClient = new AmazonSNSClient(new CognitoCachingCredentialsProvider(context,
                                    this.accountId,
                                    identityPoolId,
                                    this.unauthRoleArn,
                                    this.authRoleArn,
                                    region));
                            snsClients.add(snsClient);
                        }
                    }
                } else {
                    if (this.regions.size() < 1) {
                        AmazonSNSClient snsClient = new AmazonSNSClient(new CognitoCachingCredentialsProvider(context,
                                identityPoolId, Regions.DEFAULT_REGION));
                        snsClients.add(snsClient);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonSNSClient snsClient = new AmazonSNSClient(new CognitoCachingCredentialsProvider(context,
                                    identityPoolId, region));
                            snsClients.add(snsClient);
                        }
                    }
                }
            }

            // ak, sk
            for (String accessKey : this.accessKeys) {
                for (String secretKey : this.secretKeys) {
                    if (this.regions.size() < 1) {
                        AmazonSNSClient snsClient = new AmazonSNSClient(new BasicAWSCredentials(accessKey, secretKey));
                        snsClient.setRegion(Region.getRegion(Regions.DEFAULT_REGION));
                        snsClients.add(snsClient);
                    } else {
                        for (Regions region : this.regions) {
                            AmazonSNSClient snsClient = new AmazonSNSClient(new BasicAWSCredentials(accessKey, secretKey));
                            snsClient.setRegion(Region.getRegion(region));
                            snsClients.add(snsClient);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return snsClients;
    }
}