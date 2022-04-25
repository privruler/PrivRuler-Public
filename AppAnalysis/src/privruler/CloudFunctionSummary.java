package privruler;

import java.util.*;

import soot.toolkits.scalar.Pair;

public class CloudFunctionSummary {
	public static int STMT_MATCH = -1000;
	public static int RETURNS = -1001;
	public static int CLOUD_TYPE = -1002;
	public static int IMPLICIT_CONFIG_FIELD = -1; // take care of "PoolId", "Bucket", and "Region"
	
	//TODO: add more specific regexes
	public static String GENERIC_REGEX = "^[^\\s]{3,}$";
	public static String TOPIC_ARN_REGEX = "^arn:aws:sns:[a-z0-9-]+:[0-9]+:[a-zA-Z0-9-_]+$";
	public static String PLATFORM_APP_ARN_REGEX = "^arn:aws:sns:[a-z0-9-]+:[0-9]+:app/[A-Z]+/[a-zA-Z0-9-_]+$";
	
	
	// <api, map<arg_index, <regex, implicit_config_field>>
	public static Map<String, Map<Integer, Pair<String, String>>> KEY_CONSUMPTION_APIS;
	static {
		KEY_CONSUMPTION_APIS = new HashMap<String, Map<Integer, Pair<String, String>>>();
		
		Map<Integer, Pair<String, String>> keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider: void <init>(java.lang.String,java.lang.String>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider: void <init>(java.lang.String,java.lang.String,com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider: void <init>(java.lang.String,java.lang.String,com.amazonaws.ClientConfiguration>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider: void <init>(java.lang.String,java.lang.String,com.amazonaws.ClientConfiguration,com.amazonaws.regions.Regions>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider: void <init>(java.lang.String,java.lang.String,com.amazonaws.regions.Regions>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKey")); // accessKey
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKey")); // secretKey
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.BasicAWSCredentials: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKey")); // accessKey
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKey")); // secretKey
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "SessionToken")); // sessionToken
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.BasicSessionCredentials: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(IMPLICIT_CONFIG_FIELD, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobile.client.AWSMobileClient: com.amazonaws.mobile.client.AWSMobileClient getInstance()>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(IMPLICIT_CONFIG_FIELD, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobile.client.AWSMobileClient: com.amazonaws.auth.AWSCredentialsProvider getCredentialsProvider()>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,com.amazonaws.auth.AWSCognitoIdentityProvider,com.amazonaws.regions.Regions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,com.amazonaws.auth.AWSCognitoIdentityProvider,com.amazonaws.regions.Regions,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "UnauthArn")); // unauthArn
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AuthArn")); // authArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,com.amazonaws.auth.AWSCognitoIdentityProvider,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "UnauthArn")); // unauthArn
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AuthArn")); // authArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,com.amazonaws.auth.AWSCognitoIdentityProvider,java.lang.String,java.lang.String,com.amazonaws.services.securitytoken.AWSSecurityTokenService)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,com.amazonaws.mobile.config.AWSConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,java.lang.String,com.amazonaws.regions.Regions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,java.lang.String,com.amazonaws.regions.Regions,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "UnauthArn")); // unauthArn
		keyMap.put(4, new Pair<String, String>(GENERIC_REGEX, "AuthArn")); // authArn
		keyMap.put(5, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.regions.Regions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "UnauthArn")); // unauthArn
		keyMap.put(4, new Pair<String, String>(GENERIC_REGEX, "AuthArn")); // authArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient,com.amazonaws.services.securitytoken.AWSSecurityTokenService)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "AccountId")); // accountId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "IdentityPoolId")); // identityPoolId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "UnauthArn")); // unauthArn
		keyMap.put(4, new Pair<String, String>(GENERIC_REGEX, "AuthArn")); // authArn
		keyMap.put(5, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.auth.CognitoCachingCredentialsProvider: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.regions.Regions,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		keyMap.put(5, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.ClientConfiguration,com.amazonaws.regions.Regions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		keyMap.put(5, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.ClientConfiguration,com.amazonaws.regions.Regions,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		keyMap.put(4, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.regions.Regions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "UserPoolId")); // userPoolId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AppClientId")); // AppClientId
		keyMap.put(3, new Pair<String, String>(GENERIC_REGEX, "AppClientSecret")); // AppClientSecret
		keyMap.put(4, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool: void <init>(android.content.Context,java.lang.String,java.lang.String,java.lang.String,com.amazonaws.regions.Regions,java.lang.String)>", keyMap);
		
		/*
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "MqttClientId")); // mqttClientId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.iot.AWSIotMqttManager: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "MqttClientId")); // mqttClientId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Region")); // Region
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "AccountEndpointPrefix")); // accountEndpointPrefix
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.iot.AWSIotMqttManager: void <init>(java.lang.String,com.amazonaws.regions.Region,java.lang.String)>", keyMap);
		*/
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver download(java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver download(java.lang.String,java.io.File,com.amazonaws.mobileconnectors.s3.transferutility.TransferListener)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver download(java.lang.String,java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver download(java.lang.String,java.lang.String,java.io.File,com.amazonaws.mobileconnectors.s3.transferutility.TransferListener)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.File,com.amazonaws.services.s3.model.CannedAccessControlList)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata,com.amazonaws.services.s3.model.CannedAccessControlList)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata,com.amazonaws.services.s3.model.CannedAccessControlList,com.amazonaws.mobileconnectors.s3.transferutility.TransferListener)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.InputStream)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.io.InputStream,com.amazonaws.mobileconnectors.s3.transferutility.UploadOptions)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.lang.String,java.io.File,com.amazonaws.services.s3.model.CannedAccessControlList)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata,com.amazonaws.services.s3.model.CannedAccessControlList)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility: com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver upload(java.lang.String,java.lang.String,java.io.File,com.amazonaws.services.s3.model.ObjectMetadata,com.amazonaws.services.s3.model.CannedAccessControlList,com.amazonaws.mobileconnectors.s3.transferutility.TransferListener)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Region")); // region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Region: com.amazonaws.regions.Region getRegion(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions AP", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions CA", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions CN", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions DEFAULT", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions EU", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions Go", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions ME", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions SA", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(STMT_MATCH, new Pair<String, String>(GENERIC_REGEX, "Region"));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions US", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Region")); // region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.regions.Regions: com.amazonaws.regions.Regions fromName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentialsProvider)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentialsProvider,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentialsProvider,com.amazonaws.ClientConfiguration,com.amazonaws.http.HttpClient)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentialsProvider,com.amazonaws.ClientConfiguration,com.amazonaws.metrics.RequestMetricCollector)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentials)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.auth.AWSCredentials,com.amazonaws.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>()>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void <init>(com.amazonaws.ClientConfiguration)>", keyMap);		
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.PutObjectResult putObject(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.PutObjectResult putObject(java.lang.String,java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.PutObjectResult putObject(java.lang.String,java.lang.String,java.io.InputStream,com.amazonaws.services.s3.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.PutObjectResult putObject(com.amazonaws.services.s3.model.PutObjectRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,java.io.InputStream,com.amazonaws.services.s3.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.PutObjectRequest: com.amazonaws.services.s3.model.PutObjectRequest withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.AbstractPutObjectRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.S3Object getObject(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.S3Object getObject(com.amazonaws.services.s3.model.GetObjectRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ObjectMetadata getObject(com.amazonaws.services.s3.model.GetObjectRequest,java.io.File)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: void <init>(com.amazonaws.services.s3.model.S3ObjectId)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: void <init>(java.lang.String,java.lang.String,boolean)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.GetObjectRequest: com.amazonaws.services.s3.model.GetObjectRequest withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.S3ObjectId: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.S3ObjectId: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.S3ObjectId: void <init>(com.amazonaws.services.s3.model.S3ObjectIdBuilder)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.S3ObjectIdBuilder: com.amazonaws.services.s3.model.S3ObjectIdBuilder withBucket(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.S3ObjectIdBuilder: void setBucket(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void deleteObject(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void deleteObject(com.amazonaws.services.s3.model.DeleteObjectRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.DeleteObjectsResult deleteObjects(com.amazonaws.services.s3.model.DeleteObjectsRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectRequest: com.amazonaws.services.s3.model.DeleteObjectRequest withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectsRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectsRequest: com.amazonaws.services.s3.model.DeleteObjectsRequest withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteObjectsRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.Bucket createBucket(com.amazonaws.services.s3.model.CreateBucketRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.Bucket createBucket(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Region")); // region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.Bucket createBucket(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.Bucket createBucket(java.lang.String,com.amazonaws.services.s3.model.Region)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.CreateBucketRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Region")); // region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.CreateBucketRequest: void setRegion(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Region")); // region
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.CreateBucketRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.CreateBucketRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.CreateBucketRequest: void <init>(java.lang.String,com.amazonaws.services.s3.model.Region)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void deleteBucket(com.amazonaws.services.s3.model.DeleteBucketRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: void deleteBucket(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteBucketRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.DeleteBucketRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: boolean doesBucketExist(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: boolean doesBucketExist(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: boolean doesBucketExistV2(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: java.net.URL getUrl(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: java.lang.String getResourceUrl(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ObjectListing listObjects(com.amazonaws.services.s3.model.ListObjectsRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ObjectListing listObjects(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ObjectListing listObjects(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ListObjectsV2Result listObjectsV2(com.amazonaws.services.s3.model.ListObjectsV2Request)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ListObjectsV2Result listObjectsV2(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.AmazonS3Client: com.amazonaws.services.s3.model.ListObjectsV2Result listObjectsV2(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.ListObjectsRequest: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.Integer)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.ListObjectsRequest: com.amazonaws.services.s3.model.ListObjectsRequest withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.ListObjectsRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.ListObjectsV2Result: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Bucket")); // bucket
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.s3.model.ListObjectsV2Result: com.amazonaws.services.s3.model.ListObjectsV2Result withBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ConfirmSubscriptionResult confirmSubscription(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ConfirmSubscriptionResult confirmSubscription(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ConfirmSubscriptionResult confirmSubscription(com.amazonaws.services.sns.model.ConfirmSubscriptionRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ConfirmSubscriptionRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ConfirmSubscriptionRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ConfirmSubscriptionRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ConfirmSubscriptionRequest: com.amazonaws.services.sns.model.ConfirmSubscriptionRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.CreatePlatformApplicationResult createPlatformApplication(com.amazonaws.services.sns.model.CreatePlatformApplicationRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.CreatePlatformEndpointRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.CreatePlatformEndpointRequest: com.amazonaws.services.sns.model.CreatePlatformEndpointRequest withPlatformApplicationArn(java.lang.String)>", keyMap);

		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Topic")); // topic
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.CreateTopicResult createTopic(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.CreateTopicResult createTopic(com.amazonaws.services.sns.model.CreateTopicRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Topic")); // topic
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.CreateTopicRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Topic")); // topic
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.CreateTopicRequest: void setName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "Topic")); // topic
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.CreateTopicRequest: com.amazonaws.services.sns.model.CreateTopicRequest withName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void deleteEndpoint(com.amazonaws.services.sns.model.DeleteEndpointRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "EndpointArn")); // endpointArn including topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeleteEndpointRequest: void setEndpointArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "EndpointArn")); // endpointArn including topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeleteEndpointRequest: com.amazonaws.services.sns.model.DeleteEndpointRequest withEndpointArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void deletePlatformApplication(com.amazonaws.services.sns.model.DeletePlatformApplicationRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeletePlatformApplicationRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeletePlatformApplicationRequest: com.amazonaws.services.sns.model.DeletePlatformApplicationRequest withPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void deleteTopic(com.amazonaws.services.sns.model.DeleteTopicRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeleteTopicRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeleteTopicRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.DeleteTopicRequest: com.amazonaws.services.sns.model.DeleteTopicRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void deleteTopic(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationResult listEndpointsByPlatformApplication(com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest: com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest withPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult listSubscriptionsByTopic(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult listSubscriptionsByTopic(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult listSubscriptionsByTopic(com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest: com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.PublishResult publish(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.PublishResult publish(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.PublishResult publish(com.amazonaws.services.sns.model.PublishRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.PublishRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.PublishRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.PublishRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.PublishRequest: com.amazonaws.services.sns.model.PublishRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.SubscribeResult subscribe(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.SubscribeResult subscribe(com.amazonaws.services.sns.model.SubscribeRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SubscribeRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SubscribeRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SubscribeRequest: com.amazonaws.services.sns.model.SubscribeRequest withTopicArn(java.lang.String)>", keyMap);
	
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.GetTopicAttributesResult getTopicAttributes(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: com.amazonaws.services.sns.model.GetTopicAttributesResult getTopicAttributes(com.amazonaws.services.sns.model.GetTopicAttributesRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.GetTopicAttributesRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.GetTopicAttributesRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.GetTopicAttributesRequest: com.amazonaws.services.sns.model.GetTopicAttributesRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void setPlatformApplicationAttributes(com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest: com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest withPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void addPermission(com.amazonaws.services.sns.model.AddPermissionRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void addPermission(java.lang.String,java.lang.String,java.util.List,java.util.List)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.AddPermissionRequest: void <init>(java.lang.String,java.lang.String,java.util.List,java.util.List)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.AddPermissionRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.AddPermissionRequest: com.amazonaws.services.sns.model.AddPermissionRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest: com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest withPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void removePermission(com.amazonaws.services.sns.model.RemovePermissionRequest)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.AmazonSNSClient: void removePermission(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.RemovePermissionRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.RemovePermissionRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.RemovePermissionRequest: com.amazonaws.services.sns.model.RemovePermissionRequest withTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest: void setPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(PLATFORM_APP_ARN_REGEX, "PlatformApplicationArn")); // platformApplicationArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest: com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest withPlatformApplicationArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetTopicAttributesRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetTopicAttributesRequest: void setTopicArn(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(0, new Pair<String, String>(TOPIC_ARN_REGEX, "TopicArn")); // topicArn
		KEY_CONSUMPTION_APIS.put("<com.amazonaws.services.sns.model.SetTopicAttributesRequest: com.amazonaws.services.sns.model.SetTopicAttributesRequest withTopicArn(java.lang.String)>", keyMap);
	
		/*
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(RETURNS, new Pair<String, String>(GENERIC_REGEX, ""));
		KEY_CONSUMPTION_APIS.put("java.lang.String getAWSAccessKeyId()", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AWS", ""));
		keyMap.put(RETURNS, new Pair<String, String>(GENERIC_REGEX, ""));
		KEY_CONSUMPTION_APIS.put("java.lang.String getAWSSecretKey()", keyMap);
		*/
	
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BlobConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.microsoft.azure.storage.CloudStorageAccount: com.microsoft.azure.storage.CloudStorageAccount parse(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.microsoft.azure.storage.blob.CloudBlobClient: com.microsoft.azure.storage.blob.CloudBlobContainer getContainerReference(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BlobConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobServiceClientBuilder: com.azure.storage.blob.BlobServiceClientBuilder connectionString(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobServiceClient: com.azure.storage.blob.BlobContainerClient createBlobContainer(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BlobConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobClientBuilder: com.azure.storage.blob.BlobClientBuilder connectionString(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobClientBuilder: com.azure.storage.blob.BlobClientBuilder containerName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BlobConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobContainerClientBuilder: com.azure.storage.blob.BlobContainerClientBuilder connectionString(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobContainerClientBuilder: com.azure.storage.blob.BlobContainerClientBuilder containerName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobContainerClient: com.azure.storage.blob.BlobContainerClient createBlobContainer(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobContainerClient: void deleteBlobContainer(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "ContainerName")); // container name
		KEY_CONSUMPTION_APIS.put("<com.azure.storage.blob.BlobContainerClient: com.azure.storage.blob.BlobContainerClient getBlobContainerClient(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "HubName")); // notificationhub path
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "NotificationConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.microsoft.windowsazure.messaging.NotificationHub: void <init>(java.lang.String,java.lang.String,android.content.Context)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "NotificationConnectionString")); // connection string
		KEY_CONSUMPTION_APIS.put("<com.microsoft.windowsazure.messaging.NotificationHub: void setConnectionString(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "HubName")); // notificationhub path
		KEY_CONSUMPTION_APIS.put("<com.microsoft.windowsazure.messaging.NotificationHub: void setNotificationHubPath(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "HubName")); // hubName
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "NotificationConnectionString")); // connectionString
		KEY_CONSUMPTION_APIS.put("<com.microsoft.windowsazure.messaging.notificationhubs.NotificationHub: void start(android.app.Application,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("AZURE", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "HubName")); // hubName
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "NotificationConnectionString")); // connectionString
		KEY_CONSUMPTION_APIS.put("<com.microsoft.windowsazure.messaging.notificationhubs.NotificationHub: void initialize(android.app.Application,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.CreateQueueRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.CreateQueueRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.DeleteQueueRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.DeleteQueueRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.SendMessageRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.SendMessageRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.PeekMessageRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.PeekMessageRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.ReceiveMessageRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.ReceiveMessageRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.DeleteMessageRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "QueueName")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.model.request.DeleteMessageRequest: void setQueueName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.MNSClient: void <init>(android.content.Context,java.lang.String,com.alibaba.sdk.android.common.auth.CredentialProvider)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.mns.MNSClient: void <init>(android.content.Context,java.lang.String,com.alibaba.sdk.android.common.auth.CredentialProvider,com.alibaba.sdk.android.common.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // accountEndpoint
		KEY_CONSUMPTION_APIS.put("<com.aliyun.mns.client.CloudAccount: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // accountEndpoint
		KEY_CONSUMPTION_APIS.put("<com.aliyun.mns.client.CloudAccount: void <init>(java.lang.String,java.lang.String,java.lang.String,com.aliyun.mns.common.http.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.OSSClient: void <init>(android.content.Context,java.lang.String,com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider,com.alibaba.sdk.android.oss.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "Endpoint")); // endpoint
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.OSSClient: void <init>(android.content.Context,java.lang.String,com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider)>", keyMap);

		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.OSSClient: void <init>(android.content.Context,com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider,com.alibaba.sdk.android.oss.ClientConfiguration)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,java.lang.String,com.alibaba.sdk.android.oss.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,byte[])>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,byte[],com.alibaba.sdk.android.oss.model.ObjectMetadata)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,android.net.Uri)>", keyMap);

		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void <init>(java.lang.String,java.lang.String,android.net.Uri,com.alibaba.sdk.android.oss.model.ObjectMetadata)>", keyMap);

		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.PutObjectRequest: void setBucketName(java.lang.String)>", keyMap);

		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.GetObjectRequest: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.GetObjectRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.ListObjectsRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.ListObjectsRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.ListObjectsRequest: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,Integer)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.DeleteObjectRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.DeleteObjectRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.DeleteBucketRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.DeleteBucketRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.CreateBucketRequest: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "BucketName")); // bucketName
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.model.CreateBucketRequest: void setBucketName(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "AccessKeySecret")); // accessKeySecret
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider: void <init>(java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider: void setAccessKeyId(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeySecret")); // accessKeySecret
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider: void setAccessKeySecret(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider: void setSecretKeyId(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider: void setAccessKeyId(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider: void setSecurityToken(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // tempAK
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // tempSK
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSFederationToken: void <init>(java.lang.String,java.lang.String,java.lang.String,long)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // tempAK
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // tempSK
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSFederationToken: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // tempAK
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSFederationToken: void setTempAk(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // tempSK
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSFederationToken: void setTempSk(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.auth.OSSFederationToken: void setSecurityToken(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		keyMap.put(2, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.StsTokenCredentialProvider: void <init>(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKeyId
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.StsTokenCredentialProvider: void setSecretKeyId(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKeyId
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.StsTokenCredentialProvider: void setAccessKeyId(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "SecurityToken")); // securityToken
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.StsTokenCredentialProvider: void setSecurityToken(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AuthServerUrl")); // authServerUrl
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.OSSAuthCredentialsProvider: void <init>(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AuthServerUrl")); // authServerUrl
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.common.auth.OSSAuthCredentialsProvider: void setAuthServerUrl(java.lang.String)>", keyMap);
		
		keyMap = new HashMap<Integer, Pair<String, String>>();
		keyMap.put(CLOUD_TYPE, new Pair<String, String>("ALIYUN", ""));
		keyMap.put(0, new Pair<String, String>(GENERIC_REGEX, "AccessKeyId")); // accessKey
		keyMap.put(1, new Pair<String, String>(GENERIC_REGEX, "SecretKeyId")); // secretKey
		KEY_CONSUMPTION_APIS.put("<com.alibaba.sdk.android.oss.common.utils.OSSUtils: java.lang.String sign(java.lang.String,java.lang.String,java.lang.String)>", keyMap);
	};
}
