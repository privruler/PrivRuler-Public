package com.privruler.alibaba;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.oss.OSSClient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privruler.MainActivity;
import com.privruler.tools.AlibabaCredProfile;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlibabaClientFactory {
    private static AlibabaClientFactory singleton;
    private Map<String, AlibabaCredProfile> creds;
    private Context context;

    private AlibabaClientFactory(Context context) {
        this.creds = new HashMap<String, AlibabaCredProfile>();
        this.context = context;
        initFromAsset();
    }

    public static AlibabaClientFactory getInstance(Context context) {
        if (singleton == null) {
            singleton = new AlibabaClientFactory(context);
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
                    if (!cloudType.equals("ALIYUN")) {
                        continue;
                    }

                    if (!this.creds.containsKey(pkgName)) {
                        this.creds.put(pkgName, new AlibabaCredProfile(pkgName));
                    }
                    AlibabaCredProfile credProfile = this.creds.get(pkgName);

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
                            case "QueueName":
                                credProfile.queueNames.add(value);
                                break;
                            case "Endpoint":
                                credProfile.endpoints.add(value);
                                break;
                            case "AccessKeyId":
                                credProfile.accessKeyIds.add(value);
                                break;
                            case "SecretKeyId":
                                credProfile.secretKeyIds.add(value);
                                break;
                            case "BucketName":
                                credProfile.bucketNames.add(value);
                                break;
                            case "AuthServerUrl":
                                credProfile.authServerUrls.add(value);
                                break;
                            case "SecurityToken":
                                credProfile.securityTokens.add(value);
                                break;
                            case "AccessKeySecret":
                                credProfile.accessKeySecrets.add(value);
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
        String summariesDir = String.format("%s/AlibabaSummaries", root.getAbsolutePath());
        File summariesDirFile = new File(summariesDir);
        if (!summariesDirFile.exists()) {
            summariesDirFile.mkdirs();
        }

        for (String pkgName : this.creds.keySet()) {
            Log.v(MainActivity.LOG_TAG, "handling " + pkgName);
            AlibabaCredProfile credProfile = this.creds.get(pkgName);
            if (credProfile == null) {
                continue;
            }

            if (new File(summariesDirFile.getAbsolutePath() + "/summary_" + pkgName + ".json").exists()) {
                continue;
            }

            try {
                Set<OSSClient> ossClients = credProfile.createOSSClients(this.context);
                for (OSSClient ossClient : ossClients) {
                    OSSTest ossTest = new OSSTest(pkgName, ossClient, credProfile);
                    ossTest.doTest();
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
            AlibabaCredProfile credProfile = this.creds.get(pkgName);
            for (String accessKeyId : credProfile.accessKeyIds) {
                if (!cred2Pkgs.containsKey(accessKeyId)) {
                    cred2Pkgs.put(accessKeyId, new HashSet<String>());
                }
                cred2Pkgs.get(accessKeyId).add(pkgName);
            }

            for (String secretKeyId : credProfile.secretKeyIds) {
                if (!cred2Pkgs.containsKey(secretKeyId)) {
                    cred2Pkgs.put(secretKeyId, new HashSet<String>());
                }
                cred2Pkgs.get(secretKeyId).add(pkgName);
            }
        }

        for (String keyId : cred2Pkgs.keySet()) {
            if (cred2Pkgs.get(keyId).size() > 1) {
                System.out.println(String.format("ShareKey:%s:%s", keyId, String.join(",", cred2Pkgs.get(keyId))));
            }
        }

        int numKeyIds = 0;
        int numAppsWithKeyIds = 0;
        int numKeySecrets = 0;
        int numAppsWithKeySecrets = 0;
        int numBucketNames = 0;
        int numAppsWithBucketNames = 0;
        int numEndpoints = 0;
        int numAppsWithEndpoints = 0;

        for (String pkgName : this.creds.keySet()) {
            AlibabaCredProfile credProfile = this.creds.get(pkgName);

            if (credProfile.secretKeyIds.size() > 0) {
                numKeySecrets += credProfile.secretKeyIds.size();
                numAppsWithKeySecrets += 1;

                if (credProfile.accessKeyIds.size() > 0) {
                    numKeyIds += credProfile.accessKeyIds.size();
                    numAppsWithKeyIds += 1;
                }

                System.out.println("CredIs:" + credProfile.toString());
            }

            if (credProfile.bucketNames.size() > 0) {
                numBucketNames += credProfile.bucketNames.size();
                numAppsWithBucketNames += 1;
            }

            if (credProfile.endpoints.size() > 0) {
                numEndpoints += credProfile.endpoints.size();
                numAppsWithEndpoints += 1;
            }
        }

        System.out.println(String.format(
                "EvalData: \n\tnumKeyIds=%d; " +
                        "\n\tnumAppsWithKeyIds=%d; " +
                        "\n\tnumKeySecrets=%d; " +
                        "\n\tnumAppsWithKeySecrets=%d; " +
                        "\n\tnumBucketNames=%d; " +
                        "\n\tnumAppsWithBucketNames=%d; " +
                        "\n\tnumEndpoints=%d; " +
                        "\n\tnumAppsWithEndpoints=%d;",
                numKeyIds, numAppsWithKeyIds, numKeySecrets,
                numAppsWithKeySecrets, numBucketNames,
                numAppsWithBucketNames, numEndpoints,
                numAppsWithEndpoints));
    }
}