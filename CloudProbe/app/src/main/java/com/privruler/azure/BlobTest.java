package com.privruler.azure;

import android.util.Log;
import com.microsoft.azure.storage.blob.BlobOutputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.privruler.MainActivity;
import com.privruler.tools.StringUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlobTest {
    public Set<String> accessibleContainerNames;
    private boolean listContainers;
    private boolean listBlobs;
    private boolean download;
    private boolean upload;
    private boolean delete;
    private String pkg;
    private CloudBlobClient blobClient;
    private Set<String> containerNames;
    private String randomUUID;

    public BlobTest(String pkg, CloudBlobClient blobClient, Set<String> containerNames) {
        this.pkg = pkg;
        this.blobClient = blobClient;

        this.listContainers = false;
        this.listBlobs = false;
        this.download = false;
        this.upload = false;
        this.delete = false;

        this.containerNames = containerNames;
        this.accessibleContainerNames = new HashSet<String>();

        this.randomUUID = UUID.nameUUIDFromBytes("test-email-anonymous@gmail.com".getBytes()).toString();
    }

    public void doTest() throws IOException {
        Log.v(MainActivity.LOG_TAG, "Testing Blob App:" + this.pkg);

        Set<String> localContainerNames = new HashSet<String>();
        localContainerNames.addAll(this.containerNames);
        try {
            Iterable<CloudBlobContainer> containers = this.blobClient.listContainers();

            for (CloudBlobContainer container : containers) {
                Log.v(MainActivity.LOG_TAG, "\tContainer:" + container.getName());
                localContainerNames.add(container.getName());
            }
            this.listContainers = true;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.v(MainActivity.LOG_TAG, sw.toString());
        }

        for (String containerName : localContainerNames) {
            CloudBlobContainer container = null;
            try {
                container = this.blobClient.getContainerReference(containerName);
            } catch (Exception e) {
                // unlikely to happen, except for network issue.
                StringUtils.logException(e);
                continue;
            }

            try {
                // listBlobs does not download blobs
                Iterable<ListBlobItem> blobs = container.listBlobs();
                for (ListBlobItem blob : blobs) {
                    break;
                }
                this.listBlobs = true;
                this.accessibleContainerNames.add(containerName);
            } catch (Exception e) {
                StringUtils.logException(e);
            }

            CloudBlockBlob blob = null;
            try {
                blob = container.getBlockBlobReference(randomUUID);
                blob.delete();
                this.delete = true;
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("The specified blob does not exist.")) {
                    this.delete = true;
                }
            }

            try {
                blob = container.getBlockBlobReference(randomUUID);
                String downloaded = blob.downloadText();
                this.download = true;
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("The specified blob does not exist.")) {
                    this.download = true;
                }
            }

            try {
                blob = container.getBlockBlobReference(randomUUID);
                BlobOutputStream bos = blob.openOutputStream();
                // create a 0B file when permission granted, otherwise, trigger an exception.
                bos.close();
                this.upload = true;
            } catch (Exception e) {
                StringUtils.logException(e);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("S3[%s] = {%s,%s,%s,%s,%s}",
                this.pkg,
                this.listContainers,
                this.listBlobs,
                this.download,
                this.upload,
                this.delete
        );
    }

    public List<String> toCapabilityList() {
        List<String> ret = new ArrayList<String>();
        if (this.listContainers) {
            ret.add(" listContainers");
        }

        if (this.listBlobs) {
            ret.add(" listBlobs");
        }

        if (this.download) {
            ret.add(" download");
        }

        if (this.upload) {
            ret.add(" upload");
        }

        if (this.delete) {
            ret.add(" delete");
        }

        return ret;
    }
}
