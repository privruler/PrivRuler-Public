package com.privruler.tools;

import android.content.Context;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.windowsazure.messaging.NotificationHub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AzureCredProfile {
    public String pkgName;

    public String blobConnectString;
    public Set<String> containerNames;
    public Set<String> accessibleContainerNames;

    public String notificationConnectString;
    public Set<String> notificationHubNames;
    public Set<String> accessibleNotificationHubNames;

    public Set<String> cloudAPIs;
    public List<String> capabilityList;
    public List<String> extraCapability;

    public AzureCredProfile(String pkgName) {
        this.pkgName = pkgName;
        this.containerNames = new HashSet<String>();
        this.notificationHubNames = new HashSet<String>();
        this.cloudAPIs = new HashSet<String>();
        this.capabilityList = new ArrayList<String>();
        this.extraCapability = new ArrayList<String>();

        this.accessibleContainerNames = new HashSet<String>();
        this.accessibleNotificationHubNames = new HashSet<String>();
    }

    // This is for Azure Account Key and Account SAS
    public static boolean isAzureStorageKey(String str) {
        return (str.startsWith("DefaultEndpointsProtocol=") &&
                str.contains(";AccountName=") &&
                str.contains(";AccountKey=")) ||
                str.contains("BlobEndpoint");
    }

    public static boolean isAzureNotificationKey(String str) {
        return str.startsWith("Endpoint=") &&
                str.contains(";SharedAccessKeyName=") &&
                str.contains(";SharedAccessKey=");
    }

    public String toString() {
        return String.format("pkg=%s;" +
                        "containerNames=%s;" +
                        "notificationHubNames=%s;" +
                        "blobConnectString=%s;" +
                        "notificationConnectString=%s",
                this.pkgName,
                String.join(",", this.containerNames),
                String.join(",", this.notificationHubNames),
                this.blobConnectString, this.notificationConnectString);
    }

    public void addCapabilityList(List<String> capabilityList) {
        this.capabilityList.addAll(capabilityList);

        for (String cap : capabilityList) {
            boolean usedCap = false;

            for (String api : this.cloudAPIs) {
                if (api.toLowerCase().contains(cap.toLowerCase())) {
                    usedCap = true;
                    break;
                }
            }

            if (!usedCap) {
                this.extraCapability.add(cap);
            }
        }
    }

    public CloudBlobClient createBlobClient() {
        CloudBlobClient blobClient = null;

        if (this.blobConnectString != null) {
            try {
                CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(this.blobConnectString);
                blobClient = cloudStorageAccount.createCloudBlobClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return blobClient;
    }

    public List<NotificationHub> createNotificationHubClient(Context context) {
        List<NotificationHub> hubs = new ArrayList<NotificationHub>();
        if (this.notificationConnectString != null) {
            for (String hubName : this.notificationHubNames) {
                try {
                    NotificationHub hub = new NotificationHub(hubName, this.notificationConnectString, context);
                    hubs.add(hub);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return hubs;
    }
}