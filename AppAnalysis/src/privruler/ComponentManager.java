package privruler;

import analysisutils.AnalysisAPIs;
import analysisutils.Globals;
import dfa.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestActivity;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestBroadcastReceiver;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestContentProvider;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestService;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class ComponentManager {
	private static ComponentManager singleton;
	private Map<String, ComponentModel> componentMap;
	private MultiMap<String, String> category_to_api;

	enum COMPONENT_TYPE {
		ACTIVITY, SERVICE, RECEIVER, PROVIDER;
	}

	private ComponentManager() {
		this.componentMap = new HashMap<>();
		this.category_to_api = new HashMultiMap<String, String>();
		this.importAPIConstants();
	}

	public static ComponentManager getInstance() {
		if (singleton == null) {
			singleton = new ComponentManager();
		}
		return singleton;
	}

	private void importAPIConstants() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(Globals.CONFIG_DIR + "APIConstants.txt"));
			String line = reader.readLine();
			while (line != null) {
				String[] splits = line.split("#");
				if (splits.length >= 2) {
					this.category_to_api.put(splits[0], splits[1]);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ComponentModel getComponent(String name) {
		return this.componentMap.get(name);
	}

	public boolean hasComponent(String name) {
		return this.componentMap.containsKey(name);
	}

	public void dumpJSON() {
		Gson gson = (new GsonBuilder()).disableHtmlEscaping().create();

		for (ComponentModel cb : this.componentMap.values()) {
			Log.dumpln(gson.toJson((JsonElement) cb.toJSON()));
		}
	}

	public boolean isClosedComponent(String clazzName) {
		if (this.componentMap.containsKey(clazzName) && !((ComponentModel) this.componentMap.get(clazzName)).isExported()) {
			return true;
		}

		return false;
	}

	private void parseComponent(AXmlNode nd, ComponentModel comp) {
		Map<String, AXmlAttribute<?>> attributes = nd.getAttributes();
		for (String k : attributes.keySet()) {
			if ("name".equals(k)) {
				String componentName = (String) ((AXmlAttribute<?>) attributes.get(k)).getValue();
				if (componentName.startsWith(".")) {
					componentName = String.valueOf(Globals.PACKAGE_NAME) + componentName;
				} else if (!componentName.contains(".")) {
					componentName = String.valueOf(Globals.PACKAGE_NAME) + "." + componentName;
				}
				comp.setName(componentName);
			}
			if ("exported".equals(k)) {
				comp.setExported(((Boolean) ((AXmlAttribute<?>) attributes.get(k)).getValue()).booleanValue());
			}

			if ("permission".equals(k)) {
				comp.setPermission((String) ((AXmlAttribute<?>) attributes.get(k)).getValue());
			}
		}
		List<AXmlNode> children = nd.getChildren();
		for (AXmlNode node : children) {
			if ("intent-filter".equals(node.getTag())) {
				for (AXmlNode child : node.getChildren()) {
					if ("action".equals(child.getTag())) {
						for (String actionName : child.getAttributes().keySet()) {
							if ("name".equals(actionName)) {
								comp.addAction((String) ((AXmlAttribute<?>) child.getAttributes().get(actionName)).getValue());
							}
						}
					}
				}
			}
		}
	}

	public void parseApkForComponents(String apkPath) {
		String fileName = apkPath.substring(apkPath.lastIndexOf('/') + 1, apkPath.lastIndexOf(".apk"));
		try {
			ProcessManifest processManifest = new ProcessManifest(apkPath);
			
			Iterator<BinaryManifestActivity> activityIterator = processManifest.getActivities().iterator();
			while (activityIterator.hasNext()) {
				AXmlNode nd = activityIterator.next().getAXmlNode();
			//for (AXmlNode nd : processManifest.getActivities()) {
				ComponentModel comp = new ComponentModel(fileName, Globals.PACKAGE_NAME, COMPONENT_TYPE.ACTIVITY);
				parseComponent(nd, comp);
				if (comp.getName() != null) {
					this.componentMap.put(comp.getName(), comp);
				}
			}
			
			
			Iterator<BinaryManifestService> serviceIterator = processManifest.getServices().iterator();
			while (serviceIterator.hasNext()) {
				AXmlNode nd = serviceIterator.next().getAXmlNode();
			//for (AXmlNode nd : processManifest.getServices()) {
				ComponentModel comp = new ComponentModel(fileName, Globals.PACKAGE_NAME, COMPONENT_TYPE.SERVICE);
				parseComponent(nd, comp);
				if (comp.getName() != null) {
					this.componentMap.put(comp.getName(), comp);
				}
			}
			
			Iterator<BinaryManifestBroadcastReceiver> receiverIterator = processManifest.getBroadcastReceivers().iterator();
			while (receiverIterator.hasNext()) {
				AXmlNode nd = receiverIterator.next().getAXmlNode();
			//for (AXmlNode nd : processManifest.getReceivers()) {
				ComponentModel comp = new ComponentModel(fileName, Globals.PACKAGE_NAME, COMPONENT_TYPE.RECEIVER);
				parseComponent(nd, comp);
				if (comp.getName() != null) {
					this.componentMap.put(comp.getName(), comp);
				}
			}
			
			Iterator<BinaryManifestContentProvider> providerIterator = processManifest.getContentProviders().iterator();
			while (providerIterator.hasNext()) {
				AXmlNode nd = providerIterator.next().getAXmlNode();
			//for (AXmlNode nd : processManifest.getProviders()) {
				ComponentModel comp = new ComponentModel(fileName, Globals.PACKAGE_NAME, COMPONENT_TYPE.PROVIDER);
				parseComponent(nd, comp);
				if (comp.getName() != null) {
					this.componentMap.put(comp.getName(), comp);
				}
			}
			processManifest.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		AnalysisAPIs.runCustomPack("jtp", new Transform[] { new Transform("jtp.componentExtractor", new BodyTransformer() {
			@Override
			protected void internalTransform(Body body, String string, Map map) {
				SootMethod method = body.getMethod();
				SootClass clazz = method.getDeclaringClass();

				if (!ComponentManager.getInstance().hasComponent(clazz.getName())) {
					return;
				}

				ComponentModel component = ComponentManager.getInstance().getComponent(clazz.getName());

				Iterator<Unit> iter = body.getUnits().iterator();
				while (iter.hasNext()) {
					Stmt s = (Stmt) iter.next();

					List<ValueBox> vbs = s.getUseBoxes();
					for (ValueBox vb : vbs) {
						if (vb.getValue() instanceof StringConstant) {
							component.addConstant(((StringConstant) vb.getValue()).value);
						}
					}

					if (!s.containsInvokeExpr()) {
						continue;
					}
					SootMethod m = s.getInvokeExpr().getMethod();
					component.addCalleeMethod(m.getDeclaringClass().getName(), m.getSignature());
				}
			}
		}) });
	}

	public static boolean isFrameworkClazz(String clazzName) {
		return clazzName.startsWith("amazon.") || clazzName.startsWith("android.") || clazzName.startsWith("com.amazon.");
	}

	public class ComponentModel {
		private String app;
		private String pkg;
		private ComponentManager.COMPONENT_TYPE type;
		private String name;
		private Boolean exported;
		private String permission;
		private Set<String> actions;
		private Set<String> calleeMethods;
		private Set<String> constants;

		public ComponentModel(String app, String pkg, ComponentManager.COMPONENT_TYPE type) {
			this.app = app;
			this.pkg = pkg;
			this.type = type;
			this.exported = null;
			this.actions = new HashSet<>();
			this.calleeMethods = new HashSet<>();
			this.constants = new HashSet<>();
		}

		public JsonObject toJSON() {
			JsonObject jObj = new JsonObject();
			jObj.addProperty("category", "component");
			jObj.addProperty("app", this.app);
			jObj.addProperty("pkg", this.pkg);
			jObj.addProperty("type", this.type.name());
			jObj.addProperty("name", this.name);
			jObj.addProperty("exported", this.exported);
			jObj.addProperty("iexported", Boolean.valueOf(isExported()));
			jObj.addProperty("permission", this.permission);
			jObj.addProperty("actions", Util.legacyJoin(", ", (Iterable<String>) this.actions));
			jObj.addProperty("callees", Util.legacyJoin(", ", (Iterable<String>) this.calleeMethods));
			jObj.addProperty("constants", Util.legacyJoin(", ", (Iterable<String>) this.constants));
			return jObj;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setExported(boolean exported) {
			this.exported = Boolean.valueOf(exported);
		}

		public void setPermission(String permission) {
			this.permission = permission;
		}

		public void addAction(String action) {
			this.actions.add(action);
		}

		public void addCalleeMethod(String ivkClass, String ivkMethod) {
			if (ComponentManager.isFrameworkClazz(ivkClass)) {
				this.calleeMethods.add(ivkMethod);
			}
		}

		public void addConstant(String constant) {
			this.constants.add(constant);
		}

		public boolean isExported() {
			if (this.permission != null && !this.permission.startsWith("android.permission")) {
				return false;
			}

			if (this.exported != null && !this.exported.booleanValue()) {
				return false;
			}

			if (this.actions.size() < 1) {
				if (this.exported != null && this.exported.booleanValue()) {
					return true;
				}
				return false;
			}
			if (this.type == ComponentManager.COMPONENT_TYPE.RECEIVER) {
				boolean allProtected = true;
				for (String action : this.actions) {
					if (!ComponentManager.this.category_to_api.get("PROTECTED_BROADCAST").contains(action)) {
						allProtected = false;
					}
				}
				if (allProtected) {
					return false;
				}
			}
			return true;
		}
	}
}
