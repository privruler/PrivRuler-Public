package com.privruler.aws;

import android.content.Context;
import android.util.Log;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privruler.MainActivity;
import com.privruler.tools.AWSCredProfile;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AWSClientFactory {
    private static AWSClientFactory singleton;
    private Map<String, AWSCredProfile> creds;
    private Context context;

    private AWSClientFactory(Context context) {
        this.creds = new HashMap<String, AWSCredProfile>();
        this.context = context;
        initFromAsset();
    }

    public static AWSClientFactory getInstance(Context context) {
        if (singleton == null) {
            singleton = new AWSClientFactory(context);
        }
        return singleton;
    }

    private void initFromAsset() {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/sdcard/cloudassets/summary"), "UTF-8"));

            JSONParser parser = new JSONParser();
            String mLine;
            while (true) {
                mLine = reader.readLine();
                if (mLine == null) {
                    break;
                }

                try {
                    Log.v(MainActivity.LOG_TAG, "Read Line" + mLine);
                    JSONObject json = (JSONObject) parser.parse(mLine);

                    String pkgName = (String) json.get("appName");
                    String cloudType = (String) json.get("type");
                    if (!cloudType.equals("AWS")) {
                        continue;
                    }

                    if (!this.creds.containsKey(pkgName)) {
                        this.creds.put(pkgName, new AWSCredProfile(pkgName));
                    }
                    AWSCredProfile credProfile = this.creds.get(pkgName);

                    JSONArray cloudAPIArr = (JSONArray) json.get("cloudAPIs");
                    for (Object cloudAPIObj : cloudAPIArr) {
                        String cloudAPI = (String) ((JSONObject) cloudAPIObj).get("api");
                        credProfile.cloudAPIs.add(cloudAPI);
                    }

                    JSONArray credArr = (JSONArray) json.get("apiCredentials");
                    for (Object cred : credArr) {
                        JSONObject credObj = (JSONObject) cred;
                        String name = (String) credObj.get("name");
                        String value = (String) credObj.get("value");

                        switch (name) {
                            case "AccessKey":
                                credProfile.accessKeys.add(value);
                                break;
                            case "AccountId":
                                credProfile.accountId = value;
                                break;
                            case "AppClientId":
                                break;
                            case "AppClientSecret":
                                break;
                            case "AuthArn":
                                credProfile.authRoleArn = value;
                                break;
                            case "Bucket":
                                credProfile.bucketNames.add(value);
                                break;
                            case "EndpointArn":
                                break;
                            case "IdentityPoolId":
                                credProfile.identityPoolIds.add(value);
                                break;
                            case "PlatformApplicationArn":
                                credProfile.platformAppArns.add(value);
                                break;
                            case "SecretKey":
                                credProfile.secretKeys.add(value);
                                break;
                            case "SessionToken":
                                break;
                            case "Topic":
                                break;
                            case "TopicArn":
                                credProfile.topicArns.add(value);
                                break;
                            case "UnauthArn":
                                credProfile.unauthRoleArn = value;
                                break;
                            case "UserPoolId":
                                break;
                            case "Region":
                                Regions region = null;
                                if (value.contains("<")) {
                                    region = AWSCredProfile.toRegions(value, "");
                                } else {
                                    region = AWSCredProfile.toRegions("", value);
                                }
                                if (region != null) {
                                    credProfile.regions.add(region);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            //log the exception
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }

    public void evaluateCredCapabilities() {
        this.measureData();

        File root = android.os.Environment.getExternalStorageDirectory();
        String summariesDir = String.format("%s/AWSSummaries", root.getAbsolutePath());
        File summariesDirFile = new File(summariesDir);
        if (!summariesDirFile.exists()) {
            summariesDirFile.mkdirs();
        }

        for (String pkgName : this.creds.keySet()) {
            Log.v(MainActivity.LOG_TAG, "handling " + pkgName);
            AWSCredProfile credProfile = this.creds.get(pkgName);
            if (credProfile == null) {
                continue;
            }

            if (new File(summariesDirFile.getAbsolutePath() + "/summary_" + pkgName + ".json").exists()) {
                continue;
            }

            try {
                Set<AmazonS3Client> s3Clients = credProfile.createS3Clients(this.context);
                for (AmazonS3Client s3Client : s3Clients) {
                    S3Test s3Test = new S3Test(pkgName, s3Client, credProfile);
                    s3Test.doTest();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Set<AmazonSNSClient> snsClients = credProfile.createSNSClients(this.context);
                for (AmazonSNSClient snsClient : snsClients) {
                    SNSTest snsTest = new SNSTest(pkgName, snsClient, credProfile);
                    snsTest.doTest();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            credProfile.getExtraCapabilities();

            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            try {
                JsonGenerator g = mapper.getFactory().createGenerator(
                        new FileOutputStream(new File(summariesDirFile.getAbsolutePath() + "/summary_" + pkgName + ".json")));
                mapper.writeValue(g, credProfile);
                g.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.v(MainActivity.LOG_TAG, "End processing all packages!");
    }

    private void measureData() {
        Map<String, Set<String>> cred2Pkgs = new HashMap<String, Set<String>>();
        for (String pkgName : this.creds.keySet()) {
            AWSCredProfile credProfile = this.creds.get(pkgName);
            for (String accessKey : credProfile.accessKeys) {
                if (!cred2Pkgs.containsKey(accessKey)) {
                    cred2Pkgs.put(accessKey, new HashSet<String>());
                }
                cred2Pkgs.get(accessKey).add(pkgName);
            }

            for (String identityPoolId : credProfile.identityPoolIds) {
                if (!cred2Pkgs.containsKey(identityPoolId)) {
                    cred2Pkgs.put(identityPoolId, new HashSet<String>());
                }
                cred2Pkgs.get(identityPoolId).add(pkgName);
            }
        }

        for (String keyId : cred2Pkgs.keySet()) {
            if (cred2Pkgs.get(keyId).size() > 1) {
                System.out.println(String.format("ShareKey:%s:%s", keyId, String.join(",", cred2Pkgs.get(keyId))));
            }
        }

        int numIdentityPoolId = 0;
        int numAppsWithIdentityPoolId = 0;

        int numAccountId = 0;
        int numAppsWithAccountId = 0;

        int numUnauthRoleArn = 0;
        int numAppsWithUnauthRoleArn = 0;

        int numAuthRoleArn = 0;
        int numAppsWithAuthRoleArn = 0;

        int numAccessKey = 0;
        int numAppsWithAccessKey = 0;

        int numSecretKey = 0;
        int numAppsWithSecretKey = 0;

        int numBucketNames = 0;
        int numAppsWithBucketNames = 0;

        int numPlatformAppArns = 0;
        int numAppsWithPlatformAppArns = 0;

        int numTopicArns = 0;
        int numAppsWithTopicArns = 0;

        for (String pkgName : this.creds.keySet()) {
            AWSCredProfile credProfile = this.creds.get(pkgName);

            if (credProfile.identityPoolIds.size() > 0) {
                numIdentityPoolId += credProfile.identityPoolIds.size();
                numAppsWithIdentityPoolId += 1;
                Log.v(MainActivity.LOG_TAG, "IdentityPoolIds:" + credProfile.identityPoolIds.toString());
            }

            if (credProfile.accountId != null && credProfile.accountId.length() > 0) {
                numAccountId += 1;
                numAppsWithAccountId += 1;
            }

            if (credProfile.unauthRoleArn != null && credProfile.unauthRoleArn.length() > 0) {
                numUnauthRoleArn += 1;
                numAppsWithUnauthRoleArn += 1;
            }

            if (credProfile.authRoleArn != null && credProfile.authRoleArn.length() > 0) {
                numAuthRoleArn += 1;
                numAppsWithAuthRoleArn += 1;
            }

            if (credProfile.accessKeys.size() > 0) {
                numAccessKey += 1;
                numAppsWithAccessKey += 1;
                Log.v(MainActivity.LOG_TAG, "AccessKey:" + credProfile.accessKeys.toString());
            }

            if (credProfile.secretKeys.size() > 0) {
                numSecretKey += credProfile.secretKeys.size();
                numAppsWithSecretKey += 1;
                Log.v(MainActivity.LOG_TAG, "SecretKey:" + credProfile.secretKeys.toString());
            }

            if (credProfile.bucketNames.size() > 0) {
                numBucketNames += credProfile.bucketNames.size();
                numAppsWithBucketNames += 1;
            }

            if (credProfile.platformAppArns.size() > 0) {
                numPlatformAppArns += credProfile.platformAppArns.size();
                numAppsWithPlatformAppArns += 1;
            }

            if (credProfile.topicArns.size() > 0) {
                numTopicArns += credProfile.topicArns.size();
                numAppsWithTopicArns += 1;
            }
        }

        Log.v(MainActivity.LOG_TAG, String.format("EvalData: numIdentityPoolId=%d; " +
                        "\n\tnumAppsWithIdentityPoolId=%d; " +
                        "\n\tnumAccountId=%d; " +
                        "\n\tnumAppsWithAccountId=%d; " +
                        "\n\tnumUnauthRoleArn=%d; " +
                        "\n\tnumAppsWithUnauthRoleArn=%d;" +
                        "\n\tnumAuthRoleArn=%d; " +
                        "\n\tnumAppsWithAuthRoleArn=%d; " +
                        "\n\tnumAccessKey=%d; " +
                        "\n\tnumAppsWithAccessKey=%d; " +
                        "\n\tnumSecretKey=%d; " +
                        "\n\tnumAppsWithSecretKey=%d; " +
                        "\n\tnumBucketNames=%d; " +
                        "\n\tnumAppsWithBucketNames=%d; " +
                        "\n\tnumPlatformAppArns=%d; " +
                        "\n\tnumAppsWithPlatformAppArns=%d; " +
                        "\n\tnumTopicArns=%d; " +
                        "\n\tnumAppsWithTopicArns=%d",
                numIdentityPoolId, numAppsWithIdentityPoolId, numAccountId, numAppsWithAccountId,
                numUnauthRoleArn, numAppsWithUnauthRoleArn, numAuthRoleArn, numAppsWithAuthRoleArn,
                numAccessKey, numAppsWithAccessKey, numSecretKey,
                numAppsWithSecretKey, numBucketNames, numAppsWithBucketNames, numPlatformAppArns,
                numAppsWithPlatformAppArns, numTopicArns, numAppsWithTopicArns));
    }
}