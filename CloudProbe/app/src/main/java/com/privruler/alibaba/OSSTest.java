package com.privruler.alibaba;

import android.util.Log;

import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest;
import com.alibaba.sdk.android.oss.model.DeleteObjectResult;
import com.alibaba.sdk.android.oss.model.GetBucketInfoRequest;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.ListBucketsRequest;
import com.alibaba.sdk.android.oss.model.ListBucketsResult;
import com.alibaba.sdk.android.oss.model.OSSBucketSummary;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.privruler.MainActivity;
import com.privruler.tools.AlibabaCredProfile;
import com.privruler.tools.StringUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OSSTest {
	private String pkg;
	private AlibabaCredProfile credProfile;
	private OSSClient ossClient;
	private String randomUUID;

	public OSSTest(String pkg, OSSClient ossClient, AlibabaCredProfile credProfile) {
		this.pkg = pkg;
		this.ossClient = ossClient;
		this.credProfile = credProfile;
		this.randomUUID = UUID.nameUUIDFromBytes("test-email-anonymous@gmail.com".getBytes()).toString();
	}

	public void doTest() {
		long startTime = 0;
		long endTime = 0;
		startTime = System.currentTimeMillis();

		Log.v(MainActivity.LOG_TAG,"Testing OSS App:" + this.pkg);

		Set<String> localBucketNames = new HashSet<String>();
		localBucketNames.addAll(this.credProfile.bucketNames);

		try {
			ListBucketsResult result = ossClient.listBuckets(new ListBucketsRequest());
			this.credProfile.capabilityMap.get("listBuckets").add("*");
			for (OSSBucketSummary bucket : result.getBuckets()) {
				Log.v(MainActivity.LOG_TAG,  "\tBucket:" + bucket.name);
				localBucketNames.add(bucket.name);
			}
		} catch (Exception e) {
			StringUtils.logException(e);
		}

		for (String bucketName : localBucketNames) {
			Log.v(MainActivity.LOG_TAG,"Testing Bucket:" + bucketName);
			
			try {
				GetBucketInfoRequest request = new GetBucketInfoRequest(bucketName);
				ossClient.getBucketInfo(request);
				this.credProfile.capabilityMap.get("listObjects").add(bucketName);
			} catch (Exception e) {
				StringUtils.logException(e);
			}
			
			try {
				PutObjectRequest request = new PutObjectRequest(bucketName, this.randomUUID, "".getBytes());
				ossClient.putObject(request);
				this.credProfile.capabilityMap.get("putObject").add(bucketName);
			} catch (Exception e) {
				StringUtils.logException(e);
			}

			try {
				GetObjectRequest request = new GetObjectRequest(bucketName, this.randomUUID);
				GetObjectResult result = ossClient.getObject(request);
				this.credProfile.capabilityMap.get("getObject").add(bucketName);
			} catch (Exception e) {
				StringUtils.logException(e);
			}

			try {
				DeleteObjectRequest request = new DeleteObjectRequest(bucketName, this.randomUUID);
				DeleteObjectResult result = ossClient.deleteObject(request);
				this.credProfile.capabilityMap.get("deleteObject").add(bucketName);
			} catch (Exception e) {
				StringUtils.logException(e);
			}
		}

		endTime = System.currentTimeMillis();
		System.out.println("oss costs " + (endTime - startTime) / 1000 + " seconds");
	}
}
