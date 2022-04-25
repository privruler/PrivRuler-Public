package privruler;

import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import analysisutils.Globals;
import dfa.util.Log;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class CloudManager {
	private static Map<String, CloudManager> singletons;

	public static CloudManager getInstance(String cloudType) {
		if (singletons == null) {
			singletons = new HashMap<String, CloudManager>();
			singletons.put("AWS", new CloudManager("AWS"));
			singletons.put("AZURE", new CloudManager("AZURE"));
			singletons.put("ALIYUN", new CloudManager("ALIYUN"));
		}

		return singletons.get(cloudType);
	}

	private String cloudType;
	private MultiMap<String, String> cloudAPIs; // hostMethod --> cloudAPI
	private MultiMap<String, String> apiCredentials; // name --> value

	private CloudManager(String cloudType) {
		this.cloudType = cloudType;
		this.cloudAPIs = new HashMultiMap<String, String>();
		this.apiCredentials = new HashMultiMap<String, String>();
	}

	public void addCloudAPI(String hostMethod, String cloudApi) {
		this.cloudAPIs.put(hostMethod, cloudApi);
	}
	
	public boolean hasCloudAPI(String cloudApi) {
		return this.cloudAPIs.containsValue(cloudApi);
	}

	public MultiMap<String, String> getCloudAPIs() {
		return cloudAPIs;
	}

	public void addAPICredential(String name, String value) {		
		this.apiCredentials.put(name, value);
	}

	public void dumpJSONLineByLine() {
		Gson gson = new GsonBuilder()/* .setPrettyPrinting() */.disableHtmlEscaping().create();
		Log.dumpln(gson.toJson(this.toJSON()));
	}

	private JsonObject toJSON() {
		JsonObject jObj = new JsonObject();
		jObj.addProperty("appName", Globals.PACKAGE_NAME);
		jObj.addProperty("type", this.cloudType);
		
		JsonArray apiArr = new JsonArray();
		for (String hostMethod : cloudAPIs.keySet()) {
			for (String api : cloudAPIs.get(hostMethod)) {
				JsonObject apiObj = new JsonObject();
				apiObj.addProperty("hostMethod", hostMethod);
				apiObj.addProperty("api", api);
				apiArr.add(apiObj);
			}
		}
		jObj.add("cloudAPIs", apiArr);

		JsonArray credArr = new JsonArray();
		for (String name : apiCredentials.keySet()) {
			for (String value : apiCredentials.get(name)) {
				JsonObject credentialObj = new JsonObject();
				credentialObj.addProperty("name", name);
				credentialObj.addProperty("value", value);
				credArr.add(credentialObj);
			}
		}
		jObj.add("apiCredentials", credArr);
		return jObj;
	}
}
