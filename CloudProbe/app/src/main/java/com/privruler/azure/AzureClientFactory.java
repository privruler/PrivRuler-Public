package com.privruler.azure;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.windowsazure.messaging.NotificationHub;
import com.privruler.MainActivity;
import com.privruler.tools.AzureCredProfile;

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
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;

public class AzureClientFactory {

    private static AzureClientFactory singleton;
    private Map<String, AzureCredProfile> creds;
    private Context context;

    private AzureClientFactory(Context context) {
        this.creds = new HashMap<String, AzureCredProfile>();
        this.context = context;
        importFromAsset();
    }

    public static AzureClientFactory getInstance(Context context) {
        if (singleton == null) {
            singleton = new AzureClientFactory(context);
        }
        return singleton;
    }

    private void importFromAsset() {
        //information extracted from bytecode
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream("/sdcard/cloudassets/summary"), "UTF-8"));

            JSONParser parser = new JSONParser();
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                JSONObject json = (JSONObject) parser.parse(mLine);

                String pkgName = (String) json.get("appName");
                String cloudType = (String) json.get("type");
                if (!cloudType.equals("AZURE")) {
                    continue;
                }

                if (!this.creds.containsKey(pkgName)) {
                    this.creds.put(pkgName, new AzureCredProfile(pkgName));
                }
                AzureCredProfile credProfile = this.creds.get(pkgName);

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
                        case "BlobConnectionString":
                            credProfile.blobConnectString = value;
                            break;
                        case "ContainerName":
                            credProfile.containerNames.add(value);
                            break;
                        case "NotificationConnectionString":
                            credProfile.notificationConnectString = value;
                            break;
                        case "HubName":
                            credProfile.notificationHubNames.add(value);
                            break;
                        default:
                            break;
                    }
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void evaluateCredCapabilities() {
        File root = android.os.Environment.getExternalStorageDirectory();
        String summariesDir = String.format("%s/AzureSummaries", root.getAbsolutePath());
        File summariesDirFile = new File(summariesDir);
        if (!summariesDirFile.exists()) {
            summariesDirFile.mkdirs();
        }

        int start = 0;
        for (String pkgName : this.creds.keySet()) {
            AzureCredProfile credProfile = this.creds.get(pkgName);

            Log.v(MainActivity.LOG_TAG, "Handling " + pkgName);
            if (credProfile == null) {
                continue;
            }

            if (new File(summariesDirFile.getAbsolutePath() + "/summary_" + pkgName + ".json").exists()) {
                continue;
            }

            try {
                CloudBlobClient blobClient = credProfile.createBlobClient();
                if (blobClient != null) {
                    BlobTest blobTest = new BlobTest(pkgName, blobClient, credProfile.containerNames);
                    blobTest.doTest();
                    credProfile.addCapabilityList(blobTest.toCapabilityList());
                    credProfile.accessibleContainerNames.addAll(blobTest.accessibleContainerNames);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                List<NotificationHub> notificationHubs = getNotificationHubClients(pkgName);
                for (NotificationHub notificationHub : notificationHubs) {
                    NotificationHubTest notificationHubTest = new NotificationHubTest(pkgName, notificationHub);
                    notificationHubTest.doTest();
                    credProfile.addCapabilityList(notificationHubTest.toCapabilityList());

                    if (notificationHubTest.accessible) {
                        credProfile.accessibleNotificationHubNames.add(notificationHub.getNotificationHubPath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

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
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        boolean showStatistics = true;
        if (showStatistics) {
            int numConnectString = 0;
            int numAppsWithConnectString = 0;
            int numContainerNames = 0;
            int numAppsWithContainerNames = 0;
            int numAccessibleContainerNames = 0;
            int numNotificationHubNames = 0;
            int numAccessibleNotificationHubNames = 0;
            int numAppsWithNotificationHubNames = 0;

            for (String pkgName : this.creds.keySet()) {
                AzureCredProfile credProfile = this.creds.get(pkgName);

                Log.v(MainActivity.LOG_TAG, "EvalData:" + credProfile.toString());

                if (credProfile.blobConnectString != null) {
                    numConnectString += 1;
                }

                if (credProfile.notificationConnectString != null) {
                    numConnectString += 1;
                }

                if (credProfile.notificationConnectString != null || credProfile.blobConnectString != null) {
                    numAppsWithConnectString += 1;
                }

                if (credProfile.containerNames.size() > 0) {
                    numContainerNames += credProfile.containerNames.size();
                    numAppsWithContainerNames += 1;
                    numAccessibleContainerNames += credProfile.accessibleContainerNames.size();
                }

                if (credProfile.notificationHubNames.size() > 0) {
                    numNotificationHubNames += credProfile.notificationHubNames.size();
                    numAppsWithNotificationHubNames += 1;
                    numAccessibleNotificationHubNames += credProfile.accessibleNotificationHubNames.size();
                }
            }

            Log.v(MainActivity.LOG_TAG, String.format("EvalData: \n\tnumConnectString=%d; \n\tnumAppsWithConnectString=%d; \n\tnumContainerNames=%d; \n\tnumAppsWithContainerNames=%d; \n\tnumNotificationHubNames=%d; \n\tnumAppsWithNotificationHubNames=%d; \n\taccessibleContainerNames=%d; \n\taccessibleNotificationHubNames=%d;",
                    numConnectString, numAppsWithConnectString, numContainerNames, numAppsWithContainerNames,
                    numNotificationHubNames, numAppsWithNotificationHubNames, numAccessibleContainerNames, numAccessibleNotificationHubNames));
        }

        Log.v(MainActivity.LOG_TAG, "End processing all packages");
    }

    public List<NotificationHub> getNotificationHubClients(String pkg) {
        if (this.creds.containsKey(pkg)) {
            return this.creds.get(pkg).createNotificationHubClient(this.context);
        } else {
            return new ArrayList<NotificationHub>();
        }
    }
}