package privruler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.Gson;

import analysisutils.Globals;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResPackage;

public class ResourceParser {
	private String apkFilePath;
	private HashMap<Integer, String> stringResMap;
	private HashMap<String, String> stringLabelMap;
	private File apkFile;
	private List<String> layoutReserveStr;
	private static ResourceParser singleton;

	public static ResourceParser getInstance() throws IOException, XmlPullParserException {
		if (ResourceParser.singleton == null) {
			ResourceParser.singleton = new ResourceParser();
		}

		return ResourceParser.singleton;
	}

	private ResourceParser() throws IOException, XmlPullParserException {
		this.apkFilePath = Globals.APK_PATH;
		this.apkFile = new File(this.apkFilePath);
		if (!apkFile.exists())
			throw new RuntimeException(String.format("The given APK file %s does not exist", apkFile.getCanonicalPath()));
		this.stringResMap = getStringResourceMap();
		this.stringLabelMap = getStringResourceLabels();
		this.layoutReserveStr = Util.readAllLinesFromFile(Globals.CONFIG_DIR + "string_layout_reserve.txt");
	}

	public HashMap<Integer, String> getStringResMap() {
		return stringResMap;
	}

	public HashMap<String, String> getStringLabelMap() {
		return stringLabelMap;
	}

	private HashMap<String, String> getStringResourceLabels() {
		HashMap<String, String> stringLabelMap = new HashMap<String, String>();
		ARSCFileParser parser = new ARSCFileParser();

		try {
			parser.parse(apkFilePath);
			List<ResPackage> packages = parser.getPackages();
			if (!packages.isEmpty()) {
				for (ARSCFileParser.ResType resType : parser.getPackages().get(0).getDeclaredTypes()) {
					for (ARSCFileParser.AbstractResource resource : resType.getAllResources()) {
						if (resource instanceof ARSCFileParser.StringResource) {
							ARSCFileParser.StringResource stringResource = (ARSCFileParser.StringResource) resource;
							stringLabelMap.put(resource.getResourceName(), stringResource.getValue());
						} else {
							stringLabelMap.put(resource.getResourceName(), resource.getResourceName());
						}
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringLabelMap;
	}

	private HashMap<Integer, String> getStringResourceMap() {
		HashMap<Integer, String> stringResMap = new HashMap<Integer, String>();
		ARSCFileParser parser = new ARSCFileParser();

		try {
			parser.parse(apkFilePath);
			List<ResPackage> packages = parser.getPackages();
			if (!packages.isEmpty()) {
				for (ARSCFileParser.ResType resType : parser.getPackages().get(0).getDeclaredTypes()) {
					for (ARSCFileParser.AbstractResource resource : resType.getAllResources()) {
						if (resource instanceof ARSCFileParser.StringResource) {
							ARSCFileParser.StringResource stringResource = (ARSCFileParser.StringResource) resource;
							stringResMap.put(stringResource.getResourceID(), stringResource.getValue());
						} else {
							stringResMap.put(resource.getResourceID(), resource.getResourceName());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringResMap;
	}

	public Map<String, List<String>> parseLayout() {
		Map<String, List<String>> layoutStrMap = new HashMap<String, List<String>>();

		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(this.apkFile);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();

					if (entryName.startsWith("res/layout") && entryName.endsWith("xml")) {

						InputStream is = null;
						try {
							is = archive.getInputStream(entry);
							List<String> strInViewList = parseLayoutByView(entryName, is);

							if (strInViewList.size() > 0) {
								layoutStrMap.put(entryName, strInViewList);
							}
						} finally {
							if (is != null)
								is.close();
						}
					}
				}
			} finally {
				if (archive != null)
					archive.close();
			}
		} catch (Exception e) {
			System.err.println("Error when looking for XML resource files in apk" + apkFile.getAbsolutePath() + ": " + e);
			e.printStackTrace();
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}

		return layoutStrMap;
	}

	public Map<?, ?> parseAWSConfig() {
		Gson gson = new Gson();
		Map<?, ?> map = null;

		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(this.apkFile);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();

					if (entryName.endsWith("awsconfiguration.json")) {
						InputStream is = null;
						try {
							is = archive.getInputStream(entry);
							Reader reader = new InputStreamReader(is);
							map = gson.fromJson(reader, Map.class);

						} finally {
							if (is != null)
								is.close();
						}
					}
				}
			} finally {
				if (archive != null)
					archive.close();
			}
		} catch (Exception e) {
			System.err.println("Error when reading awsconfiguration.json file in apk" + apkFile.getAbsolutePath() + ": " + e);
			e.printStackTrace();
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}

		return map;
	}

	public List<String> parseLayoutByView(String viewName, InputStream stream) {
		List<String> strInViewList = new ArrayList<String>();

		try {
			AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
			parseLayoutNode(viewName, handler.getDocument().getRootNode(), strInViewList);

		} catch (Exception ex) {
			System.err.println("Could not read binary XML file:  " + ex.getMessage());
			ex.printStackTrace();
		}

		return strInViewList;
	}

	private void parseLayoutNode(String viewName, AXmlNode rootNode, List<String> strInViewList) {
		for (AXmlNode childNode : rootNode.getChildren()) {
			AXmlAttribute<?> idAttr = childNode.getAttribute("id");
			AXmlAttribute<?> textAttr = childNode.getAttribute("text");
			if (textAttr != null) {
				Object attrValue = textAttr.getValue();
				String realAttrValue = "";
				if (attrValue instanceof Integer) {
					if (stringResMap.containsKey(attrValue)) {
						realAttrValue = stringResMap.get(attrValue);
					} else {
						realAttrValue = attrValue.toString();
					}
				} else {
					realAttrValue = attrValue.toString();
				}
				// add strings to strViewList
				strInViewList.add(realAttrValue);
			}

			// If there's no string in text attribute, we parse semantics in ID strings
			else {
				if (idAttr != null) {
					Object attrValue = idAttr.getValue();
					if (stringResMap.containsKey(attrValue)) {
						String realAttrValue = this.filterReserveStrForIdAttr(stringResMap.get(attrValue));
						strInViewList.add(realAttrValue);
					}
				}
			}
			// recursively analyze child nodes
			this.parseLayoutNode(viewName, childNode, strInViewList);
		}
	}

	private String filterReserveStrForIdAttr(String origStr) {
		String transformedStr = origStr;
		for (String item : this.layoutReserveStr) {
			if (origStr.toLowerCase().contains(item)) {

				int beginIndex = origStr.toLowerCase().indexOf(item);
				int endIndex = beginIndex + item.length();
				String subStr = origStr.substring(beginIndex, endIndex);
				transformedStr = origStr.replace(subStr, "");
				return transformedStr;
			}
		}
		return transformedStr;
	}
}