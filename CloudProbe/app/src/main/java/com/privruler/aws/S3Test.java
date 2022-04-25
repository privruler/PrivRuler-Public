package com.privruler.aws;

import android.util.Log;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.S3Object;
import com.privruler.MainActivity;
import com.privruler.tools.AWSCredProfile;
import com.privruler.tools.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class S3Test {
    private String pkg;
    private AmazonS3Client s3Client;
    private AWSCredProfile credProfile;
    private String randomUUID;

    public S3Test(String pkg, AmazonS3Client s3Client, AWSCredProfile credProfile) {
        this.pkg = pkg;
        this.s3Client = s3Client;
        this.credProfile = credProfile;
        this.randomUUID = UUID.nameUUIDFromBytes("test-email-anonymous@gmail.com".getBytes()).toString();
    }

    public void doTest() {
        long startTime = 0;
        long endTime = 0;
        startTime = System.currentTimeMillis();

        Log.v(MainActivity.LOG_TAG, "Testing S3 App:" + this.pkg);
        Set<String> localBucketNames = new HashSet<String>();
        localBucketNames.addAll(this.credProfile.bucketNames);

        try {
            List<Bucket> buckets = s3Client.listBuckets();
            this.credProfile.capabilityMap.get("listBuckets").add("*");
            for (Bucket bucket : buckets) {
                Log.v(MainActivity.LOG_TAG, "\tBucket:" + bucket.getName());
                localBucketNames.add(bucket.getName());
            }
        } catch (Exception e) {
            StringUtils.logException(e);
            // No permission: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: xx), S3 Extended Request ID: xx
            // Has permission: No exception
        }

        for (String bucketName : localBucketNames) {
            try {
                HeadBucketRequest req = new HeadBucketRequest(bucketName);
                s3Client.headBucket(req);
                this.credProfile.capabilityMap.get("listObjects").add(bucketName);
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.s3.model.AmazonS3Exception: Forbidden (Service: Amazon S3; Status Code: 403; Error Code: 403 Forbidden; Request ID: xx), S3 Extended Request ID: xx
                // Has permission: No exception
            }

            try {
                InitiateMultipartUploadRequest initRequest =
                        new InitiateMultipartUploadRequest(bucketName, randomUUID);
                InitiateMultipartUploadResult initResponse =
                        s3Client.initiateMultipartUpload(initRequest);
                this.credProfile.capabilityMap.get("putObject").add(bucketName);
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: xx), S3 Extended Request ID: xx
                // Has permission: No exception
            }

            try {
                S3Object obj = s3Client.getObject(bucketName, randomUUID);
                System.out.println(obj.toString());
                this.credProfile.capabilityMap.get("getObject").add(bucketName);
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("The specified key does not exist.")) {
                    this.credProfile.capabilityMap.get("getObject").add(bucketName);
                }
                // No permission: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: xx), S3 Extended Request ID: xx
                // Has permission: com.amazonaws.services.s3.model.AmazonS3Exception: The specified key does not exist. (Service: Amazon S3; Status Code: 404; Error Code: NoSuchKey; Request ID: xx), S3 Extended Request ID: xx
            }

            try {
                s3Client.deleteObject(bucketName, randomUUID);
                this.credProfile.capabilityMap.get("deleteObject").add(bucketName);
                // will success no matter the file name is correct or not.
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.s3.model.AmazonS3Exception: Access Denied (Service: Amazon S3; Status Code: 403; Error Code: AccessDenied; Request ID: xx), S3 Extended Request ID: xx
                // Has permission: No exception
            }
        }

        s3Client.shutdown();
        endTime = System.currentTimeMillis();
        Log.v(MainActivity.LOG_TAG, "s3 costs " + (endTime - startTime) / 1000 + " seconds");
    }
}