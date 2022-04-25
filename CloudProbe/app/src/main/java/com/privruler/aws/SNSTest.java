package com.privruler.aws;

import android.util.Log;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AddPermissionRequest;
import com.amazonaws.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest;
import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.Endpoint;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.GetSMSAttributesRequest;
import com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest;
import com.amazonaws.services.sns.model.ListPhoneNumbersOptedOutRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.OptInPhoneNumberRequest;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.RemovePermissionRequest;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.SetSMSAttributesRequest;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.privruler.MainActivity;
import com.privruler.tools.AWSCredProfile;
import com.privruler.tools.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SNSTest {
    private final String pkg;
    private final AmazonSNSClient snsClient;
    private AWSCredProfile credProfile;
    private String randomUUID;

    public SNSTest(String pkg, AmazonSNSClient snsClient, AWSCredProfile credProfile) {
        this.pkg = pkg;
        this.snsClient = snsClient;
        this.credProfile = credProfile;
        this.randomUUID = UUID.nameUUIDFromBytes("test-email-anonymous@gmail.com".getBytes()).toString();
    }

    public AmazonSNSClient getSnsClient() {
        return this.snsClient;
    }

    public void doTest() {
        Log.v(MainActivity.LOG_TAG, "Testing SNS App:" + this.pkg);

        try {
            List<Topic> topics = snsClient.listTopics().getTopics();
            credProfile.capabilityMap.get("listTopics").add("*");
            for (Topic topic : topics) {
                Log.v(MainActivity.LOG_TAG, "\tTopic:" + topic.getTopicArn());
                this.credProfile.topicArns.add(topic.getTopicArn());
            }
        } catch (Exception e) {
            StringUtils.logException(e);
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListTopics on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            // Has permission: no exception
        }

        for (String topicArn : this.credProfile.topicArns) {
            Log.v(MainActivity.LOG_TAG, "\tTopic:" + topicArn);

            try {
                List<Subscription> subs = snsClient.listSubscriptionsByTopic(topicArn).getSubscriptions();
                credProfile.capabilityMap.get("listSubscriptionsByTopic").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListSubscriptionsByTopic on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: no exception
            }
        }

        try {
            List<PlatformApplication> pltApps = snsClient.listPlatformApplications().getPlatformApplications();
            credProfile.capabilityMap.get("listPlatformApplications").add("*");
            for (PlatformApplication app : pltApps) {
                Log.v(MainActivity.LOG_TAG, "\tpltApp:" + app.getPlatformApplicationArn());
                this.credProfile.platformAppArns.add(app.getPlatformApplicationArn());
            }
        } catch (Exception e) {
            StringUtils.logException(e);
            // no permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListPlatformApplications on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            // has permission: no exception
        }

        for (String pltApp : this.credProfile.platformAppArns) {
            Log.v(MainActivity.LOG_TAG, "\tpltApp:" + pltApp);
            if (credProfile.capabilityMap.get("listEndpointsByPlatformApplication").size() > 0) {
                break;
            }
            try {
                List<Endpoint> endPoints = snsClient.listEndpointsByPlatformApplication(
                        new ListEndpointsByPlatformApplicationRequest().
                                withPlatformApplicationArn(pltApp)).getEndpoints();
                credProfile.capabilityMap.get("listEndpointsByPlatformApplication").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListEndpointsByPlatformApplication on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: no exception
            }
        }

        try {
            CreateTopicRequest req = new CreateTopicRequest();
            // maximum allowd topic name is 256; we choose 512 here.
            req.setName(StringUtils.getRandomAlphaNumericString(512));
            snsClient.createTopic(req);
            credProfile.capabilityMap.get("createTopic").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:CreateTopic on resource: arn:aws:sns:us-east-2:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid parameter: Topic Name (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
            if (e.toString().contains("Invalid parameter: Topic Name")) {
                credProfile.capabilityMap.get("createTopic").add("*");
            }
        }

        for (String platformAppArn : this.credProfile.platformAppArns) {
            platformAppArn = platformAppArn.replace(":app/", ":endpoint/");
            String endpointArn = platformAppArn + "/" + randomUUID;

            try {
                DeleteEndpointRequest req = new DeleteEndpointRequest();
                req.setEndpointArn(endpointArn);
                snsClient.deleteEndpoint(req);
                credProfile.capabilityMap.get("deleteEndpoint").add("*");
                // break since deleteEndpoint apply to all resources
                break;
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:DeleteEndpoint on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: no exception
            }
        }

        for (String topicArn : this.credProfile.topicArns) {
            try {
                // note we have to use a correct topicArn as a prefix so as to get the account id, otherwise, we get account error regardless of having permission or not.
                snsClient.deleteTopic(topicArn + randomUUID);
                credProfile.capabilityMap.get("deleteTopic").add("*");
                // deleteTopic applies to each topic, but we only evaluate if deleteTopic is granted to all resources. We therefore break early.
                break;
            } catch (Exception e) {
                StringUtils.logException(e);
                // has deleteTopic permission for all resources: no exception
                // have deleteTopic permission for a specific topic: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:DeleteTopic on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // don't have deleteTopic permission for any topic: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:DeleteTopic on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        try {
            OptInPhoneNumberRequest req = new OptInPhoneNumberRequest();
            req.setPhoneNumber(randomUUID);
            snsClient.optInPhoneNumber(req);
            credProfile.capabilityMap.get("optInPhoneNumber").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            if (e.toString().contains("Invalid parameter: PhoneNumber Reason: input incorrectly formatted")) {
                credProfile.capabilityMap.get("optInPhoneNumber").add("*");
            }
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:OptInPhoneNumber on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid parameter: PhoneNumber Reason: input incorrectly formatted (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
        }

        // TTL doesn't work for types (email, SNS, etc.) other than ADM/APNS/BAIDU/FCM/WNS/APNS_SANDBOX
        for (String topicArn : this.credProfile.topicArns) {
            try {
                Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
                messageAttributes.put("AWS.SNS.MOBILE.ADM.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));
                messageAttributes.put("AWS.SNS.MOBILE.APNS.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));
                messageAttributes.put("AWS.SNS.MOBILE.APNS_SANDBOX.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));
                messageAttributes.put("AWS.SNS.MOBILE.BAIDU.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));
                messageAttributes.put("AWS.SNS.MOBILE.FCM.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));
                messageAttributes.put("AWS.SNS.MOBILE.WNS.TTL", new MessageAttributeValue().withDataType("String").withStringValue("0"));

                PublishRequest req = new PublishRequest();
                req.setMessageAttributes(messageAttributes);
                String message = "{\"title\":\"Test_Title\",\"description\":\"Test_Description\"}";
                req.setMessage(message);
                req.setMessageStructure("json");
                req.setTargetArn(topicArn + ":" + randomUUID);
                snsClient.publish(req);
                credProfile.capabilityMap.get("publish").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Service: AmazonSNS; Status Code: 500; Error Code: InternalFailure;")) {
                    credProfile.capabilityMap.get("publish").add(topicArn);
                }
                // Use a wrong target ARN:
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:Publish on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: com.amazonaws.AmazonServiceException: null (Service: AmazonSNS; Status Code: 500; Error Code: InternalFailure; Request ID: xx)
            }
        }

        for (String platformAppArn : this.credProfile.platformAppArns) {
            // permission applies to all resources.
            if (credProfile.capabilityMap.get("setEndpointAttributes").size() > 0) {
                break;
            }

            String endpointArn = platformAppArn.replace(":app/", ":endpoint/") + "/" + randomUUID;
            try {
                SetEndpointAttributesRequest req = new SetEndpointAttributesRequest();
                req.setEndpointArn(endpointArn);
                Map<String, String> attr = new HashMap<String, String>();
                attr.put("Enabled", "false");
                req.setAttributes(attr);
                snsClient.setEndpointAttributes(req);
                credProfile.capabilityMap.get("setEndpointAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Endpoint does not exist")) {
                    credProfile.capabilityMap.get("setEndpointAttributes").add("*");
                }
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:SetEndpointAttributes on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: Endpoint does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
            }
        }

        for (String platformAppArn : this.credProfile.platformAppArns) {
            // permission applies to all resources.
            if (credProfile.capabilityMap.get("setPlatformApplicationAttributes").size() > 0) {
                break;
            }

            try {
                SetPlatformApplicationAttributesRequest req = new SetPlatformApplicationAttributesRequest();
                req.setPlatformApplicationArn(platformAppArn + randomUUID);
                Map<String, String> attr = new HashMap<String, String>();
                attr.put("Enabled", "false");
                req.setAttributes(attr);
                snsClient.setPlatformApplicationAttributes(req);
                credProfile.capabilityMap.get("setPlatformApplicationAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("PlatformApplication does not exist")) {
                    credProfile.capabilityMap.get("setPlatformApplicationAttributes").add("*");
                }
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: PlatformApplication does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:SetPlatformApplicationAttributes on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        for (String topicArn : this.credProfile.topicArns) {
            // permission applies to all resources.
            if (credProfile.capabilityMap.get("setSubscriptionAttributes").size() > 0) {
                break;
            }

            try {
                SetSubscriptionAttributesRequest req = new SetSubscriptionAttributesRequest();
                req.setAttributeName("FilterPolicy");
                req.setAttributeValue("{}");
                req.setSubscriptionArn(topicArn + ":" + randomUUID);
                snsClient.setSubscriptionAttributes(req);
                credProfile.capabilityMap.get("setSubscriptionAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Subscription does not exist")) {
                    credProfile.capabilityMap.get("setSubscriptionAttributes").add("*");
                }
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: Subscription does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:SetSubscriptionAttributes on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        for (String topicArn : this.credProfile.topicArns) {
            // permission is specific to topics; but we only test whether its granted for all resources.
            if (credProfile.capabilityMap.get("setTopicAttributes").size() > 0) {
                break;
            }

            try {
                SetTopicAttributesRequest req = new SetTopicAttributesRequest();
                req.setAttributeName("DisplayName");
                req.setAttributeValue(topicArn.substring(topicArn.lastIndexOf(':') + 1));
                req.setTopicArn(topicArn + randomUUID);
                snsClient.setTopicAttributes(req);
                credProfile.capabilityMap.get("setTopicAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Topic does not exist")) {
                    credProfile.capabilityMap.get("setTopicAttributes").add("*");
                }
                // This permission is specific to topics.
                // No permission for the topic: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:SetTopicAttributes on resource: xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission for the topic (all resource): com.amazonaws.services.sns.model.NotFoundException: Topic does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
            }
        }

        for (String topicArn : this.credProfile.topicArns) {
            // permission is specific to topics; but we only test whether its granted for all resources.
            if (credProfile.capabilityMap.get("subscribe").size() > 0) {
                break;
            }

            try {
                SubscribeRequest req = new SubscribeRequest();
                req.setEndpoint("test-email-anonymous@gmail.com");
                req.setProtocol("email");
                req.setTopicArn(topicArn + randomUUID);
                snsClient.subscribe(req);
                credProfile.capabilityMap.get("subscribe").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Topic does not exist")) {
                    credProfile.capabilityMap.get("subscribe").add("*");
                }
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: Topic does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:Subscribe on resource: arn:aws:sns:us-east-1:xx:xxCC (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        for (String topicArn : this.credProfile.topicArns) {
            // permission applies to all resources
            if (credProfile.capabilityMap.get("unsubscribe").size() > 0) {
                break;
            }

            try {
                UnsubscribeRequest req = new UnsubscribeRequest();
                req.setSubscriptionArn(topicArn + ":" + randomUUID);
                snsClient.unsubscribe(req);
                credProfile.capabilityMap.get("unsubscribe").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:Unsubscribe on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: no exception
            }
        }

        // permission is specific to topics
        for (String topicArn : this.credProfile.topicArns) {
            try {
                AddPermissionRequest req = new AddPermissionRequest();
                req.setTopicArn(topicArn);
                // no statement will be added
                snsClient.addPermission(req);
                credProfile.capabilityMap.get("addPermission").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("validation errors detected:")) {
                    credProfile.capabilityMap.get("addPermission").add(topicArn);
                }
                // Has permission: com.amazonaws.AmazonServiceException: 3 validation errors detected: Value null at 'aWSAccountId' failed to satisfy constraint: Member must not be null; Value null at 'label' failed to satisfy constraint: Member must not be null; Value null at 'actionName' failed to satisfy constraint: Member must not be null (Service: AmazonSNS; Status Code: 400; Error Code: ValidationError; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:AddPermission on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError;
            }
        }

        // permission is specific to topics
        for (String topicArn : this.credProfile.topicArns) {
            try {
                RemovePermissionRequest req = new RemovePermissionRequest();
                req.setLabel(randomUUID);
                req.setTopicArn(topicArn);
                // no statement will be removed.
                snsClient.removePermission(req);
                credProfile.capabilityMap.get("removePermission").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:RemovePermission on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: no exception
            }
        }

        // permission applies to all resources
        try {
            SetSMSAttributesRequest req = new SetSMSAttributesRequest();
            Map<String, String> attr = new HashMap<String, String>();
            attr.put(randomUUID, randomUUID);
            req.setAttributes(attr);
            snsClient.setSMSAttributes(req);
            credProfile.capabilityMap.get("setSMSAttributes").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            if (e.toString().contains("Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter;")) {
                credProfile.capabilityMap.get("setSMSAttributes").add("*");
            }
            // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid parameter: (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:SetSMSAttributes on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
        }

        // permission applies to all resources
        try {
            CheckIfPhoneNumberIsOptedOutRequest req = new CheckIfPhoneNumberIsOptedOutRequest();
            req.setPhoneNumber(randomUUID);
            snsClient.checkIfPhoneNumberIsOptedOut(req);
            credProfile.capabilityMap.get("checkIfPhoneNumberIsOptedOut").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            if (e.toString().contains("PhoneNumber Reason: input incorrectly formatted")) {
                credProfile.capabilityMap.get("checkIfPhoneNumberIsOptedOut").add("*");
            }
            // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid parameter: PhoneNumber Reason: input incorrectly formatted (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:CheckIfPhoneNumberIsOptedOut on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
        }

        // permission applies to all resources
        for (String platformAppArn : this.credProfile.platformAppArns) {
            if (credProfile.capabilityMap.get("getEndpointAttributes").size() > 0) {
                break;
            }

            try {
                GetEndpointAttributesRequest req = new GetEndpointAttributesRequest();
                req.setEndpointArn(platformAppArn + "/" + randomUUID);
                snsClient.getEndpointAttributes(req);
                credProfile.capabilityMap.get("getEndpointAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Endpoint does not exist")) {
                    credProfile.capabilityMap.get("getEndpointAttributes").add("*");
                }
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: Endpoint does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:GetEndpointAttributes on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        // permission applies to all resources
        for (String platformAppArn : this.credProfile.platformAppArns) {
            if (credProfile.capabilityMap.get("getPlatformApplicationAttributes").size() > 0) {
                break;
            }

            try {
                GetPlatformApplicationAttributesRequest applicationAttributesRequest = new GetPlatformApplicationAttributesRequest();
                applicationAttributesRequest.setPlatformApplicationArn(platformAppArn + randomUUID);
                GetPlatformApplicationAttributesResult getAttributesResult = snsClient
                        .getPlatformApplicationAttributes(applicationAttributesRequest);
                credProfile.capabilityMap.get("getPlatformApplicationAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("PlatformApplication does not exist")) {
                    credProfile.capabilityMap.get("getPlatformApplicationAttributes").add("*");
                }
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: PlatformApplication does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:GetPlatformApplicationAttributes on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        // permission applies to all resources
        try {
            GetSMSAttributesRequest req = new GetSMSAttributesRequest();
            snsClient.getSMSAttributes(req);
            credProfile.capabilityMap.get("getSMSAttributes").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            // Has permission: no exception
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:GetSMSAttributes on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
        }

        // permission applies to all resources
        for (String topicArn : this.credProfile.topicArns) {
            if (credProfile.capabilityMap.get("getSubscriptionAttributes").size() > 0) {
                break;
            }

            try {
                snsClient.getSubscriptionAttributes(topicArn + ":" + randomUUID);
                credProfile.capabilityMap.get("getSubscriptionAttributes").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Subscription does not exist")) {
                    credProfile.capabilityMap.get("getSubscriptionAttributes").add("*");
                }
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:GetSubscriptionAttributes on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: Subscription does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
            }
        }

        // permission is specific to topics
        for (String topicArn : this.credProfile.topicArns) {
            try {
                snsClient.getTopicAttributes(topicArn);
                credProfile.capabilityMap.get("getTopicAttributes").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                // Has permission: no exception
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:GetTopicAttributes on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }

        // permission applies to all resources
        try {
            ListPhoneNumbersOptedOutRequest req = new ListPhoneNumbersOptedOutRequest();
            snsClient.listPhoneNumbersOptedOut(req);
            credProfile.capabilityMap.get("listPhoneNumbersOptedOut").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            // Has permission: no exception
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListPhoneNumbersOptedOut on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
        }

        // permission is specific to topics
        for (String topicArn : this.credProfile.topicArns) {
            try {
                snsClient.confirmSubscription(topicArn, StringUtils.getRandomAlphaNumericString(152));
                credProfile.capabilityMap.get("confirmSubscription").add(topicArn);
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("Invalid token")) {
                    credProfile.capabilityMap.get("confirmSubscription").add(topicArn);
                }
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ConfirmSubscription on resource: arn:aws:sns:us-east-1:xx:xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid token (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
            }
        }

        // permission applies to all resources
        try {
            CreatePlatformApplicationRequest req = new CreatePlatformApplicationRequest();
            req.setPlatform("GCM");
            req.setName("DemoPlatformApplication");
            Map<String, String> attributes = new HashMap<>();
            attributes.put("PlatformPrincipal", "");
            attributes.put("PlatformCredential", randomUUID);
            req.setAttributes(attributes);
            snsClient.createPlatformApplication(req);
            credProfile.capabilityMap.get("createPlatformApplication").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            if (e.toString().contains("Platform credentials are invalid")) {
                credProfile.capabilityMap.get("createPlatformApplication").add("*");
            }
            // Has permission: com.amazonaws.services.sns.model.InvalidParameterException: Invalid parameter: Attributes Reason: Platform credentials are invalid (Service: AmazonSNS; Status Code: 400; Error Code: InvalidParameter; Request ID: xx)
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:CreatePlatformApplication on resource: arn:aws:sns:us-east-1:xx:app/ (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
        }

        // permission applies to all resources
        try {
            ListSubscriptionsResult lresult = snsClient.listSubscriptions();
            credProfile.capabilityMap.get("listSubscriptions").add("*");
        } catch (Exception e) {
            StringUtils.logException(e);
            // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:ListSubscriptions on resource: arn:aws:sns:us-east-1:xx:* (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            // Has permission: no exception
        }

        // permission applies to all resources
        for (String platformArn : this.credProfile.platformAppArns) {
            if (credProfile.capabilityMap.get("createPlatformEndpoint").size() > 0) {
                break;
            }

            try {
                CreatePlatformEndpointRequest req = new CreatePlatformEndpointRequest();
                req.setPlatformApplicationArn(platformArn + randomUUID);
                req.setCustomUserData("test-email-anonymous@gmail.com");
                req.setToken(StringUtils.getRandomAlphaNumericString(152));
                snsClient.createPlatformEndpoint(req);
                credProfile.capabilityMap.get("createPlatformEndpoint").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                if (e.toString().contains("PlatformApplication does not exist")) {
                    credProfile.capabilityMap.get("createPlatformEndpoint").add("*");
                }
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:CreatePlatformEndpoint on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
                // Has permission: com.amazonaws.services.sns.model.NotFoundException: PlatformApplication does not exist (Service: AmazonSNS; Status Code: 404; Error Code: NotFound; Request ID: xx)
            }
        }

        // permission applies to all resources
        for (String platformAppArn : this.credProfile.platformAppArns) {
            if (credProfile.capabilityMap.get("deletePlatformApplication").size() > 0) {
                break;
            }

            try {
                DeletePlatformApplicationRequest req = new DeletePlatformApplicationRequest();
                req.setPlatformApplicationArn(platformAppArn + randomUUID);
                snsClient.deletePlatformApplication(req);
                credProfile.capabilityMap.get("deletePlatformApplication").add("*");
            } catch (Exception e) {
                StringUtils.logException(e);
                // Has permission: no exception
                // No permission: com.amazonaws.services.sns.model.AuthorizationErrorException: User: arn:aws:iam::xx:user/xx is not authorized to perform: SNS:DeletePlatformApplication on resource: arn:aws:sns:us-east-1:xx:app/GCM/xx (Service: AmazonSNS; Status Code: 403; Error Code: AuthorizationError; Request ID: xx)
            }
        }
    }
}