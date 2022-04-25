package com.privruler.tools;

/*
 * Copyright 2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.CreatePlatformApplicationResult;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.privruler.tools.SNSSampleMessageGenerator.Platform;

import java.util.HashMap;
import java.util.Map;

public class AmazonSNSClientWrapper {

    private final AmazonSNS snsClient;

    public AmazonSNSClientWrapper(AmazonSNS client) {
        this.snsClient = client;
    }

    public static Map<String, MessageAttributeValue> getValidNotificationAttributes(
            Map<String, MessageAttributeValue> notificationAttributes) {
        Map<String, MessageAttributeValue> validAttributes = new HashMap<String, MessageAttributeValue>();

        if (notificationAttributes == null) return validAttributes;

        for (Map.Entry<String, MessageAttributeValue> entry : notificationAttributes
                .entrySet()) {
            if (!StringUtils.isBlank(entry.getValue().getStringValue())) {
                validAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        return validAttributes;
    }

    private CreatePlatformApplicationResult createPlatformApplication(
            String applicationName, Platform platform, String principal,
            String credential) {
        CreatePlatformApplicationRequest platformApplicationRequest = new CreatePlatformApplicationRequest();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("PlatformPrincipal", principal);
        attributes.put("PlatformCredential", credential);
        platformApplicationRequest.setAttributes(attributes);
        platformApplicationRequest.setName(applicationName);
        platformApplicationRequest.setPlatform(platform.name());
        return snsClient.createPlatformApplication(platformApplicationRequest);
    }

    private CreatePlatformEndpointResult createPlatformEndpoint(
            Platform platform, String customData, String platformToken,
            String applicationArn) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setCustomUserData(customData);
        String token = platformToken;
        String userId = null;
        if (platform == Platform.BAIDU) {
            String[] tokenBits = platformToken.split("\\|");
            token = tokenBits[0];
            userId = tokenBits[1];
            Map<String, String> endpointAttributes = new HashMap<String, String>();
            endpointAttributes.put("UserId", userId);
            endpointAttributes.put("ChannelId", token);
            platformEndpointRequest.setAttributes(endpointAttributes);
        }
        platformEndpointRequest.setToken(token);
        platformEndpointRequest.setPlatformApplicationArn(applicationArn);
        return snsClient.createPlatformEndpoint(platformEndpointRequest);
    }

    private void deletePlatformApplication(String applicationArn) {
        DeletePlatformApplicationRequest request = new DeletePlatformApplicationRequest();
        request.setPlatformApplicationArn(applicationArn);
        snsClient.deletePlatformApplication(request);
    }

    public PublishResult publish(String endpointArn, Platform platform,
                                 Map<Platform, Map<String, MessageAttributeValue>> attributesMap) {
        PublishRequest publishRequest = new PublishRequest();
        Map<String, MessageAttributeValue> notificationAttributes = getValidNotificationAttributes(attributesMap
                .get(platform));
        if (notificationAttributes != null && !notificationAttributes.isEmpty()) {
            publishRequest.setMessageAttributes(notificationAttributes);
        }
        publishRequest.setMessageStructure("json");
        // If the message attributes are not set in the requisite method,
        // notification is sent with default attributes
        String message = getPlatformSampleMessage(platform);
        Map<String, String> messageMap = new HashMap<String, String>();
        messageMap.put(platform.name(), message);
        message = SNSSampleMessageGenerator.jsonify(messageMap);
        // For direct publish to mobile end points, topicArn is not relevant.
        publishRequest.setTargetArn(endpointArn);

        // Display the message that will be sent to the endpoint/
        System.out.println("{Message Body: " + message + "}");
        StringBuilder builder = new StringBuilder();
        builder.append("{Message Attributes: ");
        for (Map.Entry<String, MessageAttributeValue> entry : notificationAttributes
                .entrySet()) {
            builder.append("(\"" + entry.getKey() + "\": \""
                    + entry.getValue().getStringValue() + "\"),");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("}");
        System.out.println(builder.toString());

        publishRequest.setMessage(message);
        return snsClient.publish(publishRequest);
    }

    public void demoNotification(Platform platform, String principal,
                                 String credential, String platformToken, String applicationName,
                                 Map<Platform, Map<String, MessageAttributeValue>> attrsMap) {
        // Create Platform Application. This corresponds to an app on a
        // platform.
        CreatePlatformApplicationResult platformApplicationResult = createPlatformApplication(
                applicationName, platform, principal, credential);
        System.out.println(platformApplicationResult);

        // The Platform Application Arn can be used to uniquely identify the
        // Platform Application.
        String platformApplicationArn = platformApplicationResult
                .getPlatformApplicationArn();

        // Create an Endpoint. This corresponds to an app on a device.
        CreatePlatformEndpointResult platformEndpointResult = createPlatformEndpoint(
                platform,
                "CustomData - Useful to store endpoint specific data",
                platformToken, platformApplicationArn);
        System.out.println(platformEndpointResult);

        // Publish a push notification to an Endpoint.
        PublishResult publishResult = publish(
                platformEndpointResult.getEndpointArn(), platform, attrsMap);
        System.out.println("Published! \n{MessageId="
                + publishResult.getMessageId() + "}");
        // Delete the Platform Application since we will no longer be using it.
        deletePlatformApplication(platformApplicationArn);
    }

    private String getPlatformSampleMessage(Platform platform) {
        switch (platform) {
            case APNS:
                return SNSSampleMessageGenerator.getSampleAppleMessage();
            case APNS_SANDBOX:
                return SNSSampleMessageGenerator.getSampleAppleMessage();
            case GCM:
                return SNSSampleMessageGenerator.getSampleAndroidMessage();
            case ADM:
                return SNSSampleMessageGenerator.getSampleKindleMessage();
            case BAIDU:
                return SNSSampleMessageGenerator.getSampleBaiduMessage();
            case WNS:
                return SNSSampleMessageGenerator.getSampleWNSMessage();
            case MPNS:
                return SNSSampleMessageGenerator.getSampleMPNSMessage();
            default:
                throw new IllegalArgumentException("Platform not supported : "
                        + platform.name());
        }
    }
}
