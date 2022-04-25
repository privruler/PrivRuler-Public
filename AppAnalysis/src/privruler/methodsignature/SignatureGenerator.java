package privruler.methodsignature;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import analysisutils.Globals;
import privruler.Util;
import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class SignatureGenerator extends BodyTransformer {
	private static Set<String> focusPkgs = new HashSet<String>();
	
	//method signatures for current app
	private static Map<String, String> methodSignatures = new ConcurrentHashMap<String, String>();
	
	// interested method signatures
	public static Map<String, String> interestedSignatures = new HashMap<String, String>();
	static {
		String fileName = Globals.CONFIG_DIR + "interested_method_signatures.txt";
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(fileName));

			while (true) {
				String line = dis.readLine();
				if (line == null) {
					break;
				}
				
				if (!line.contains("#")) {
					continue;
				}
				
				String signature = line.substring(0, line.indexOf('#'));
				String methodStr = line.substring(line.indexOf('#') + 1);
				interestedSignatures.put(signature, methodStr);
			}
			
			dis.close();
		} catch (Exception e) {
			throw new RuntimeException("Unexpected IO error on " + fileName, e);
		}
	}
	
	public static Map<String, String> getMethodSignatures() {
		return methodSignatures;
	}

	public static void addFocusPkg(String pkg) {
		SignatureGenerator.focusPkgs.add(pkg);
	}
	
	private String generateMethodSignature(SootMethod method, Body body) {		
		SootClass clazz = method.getDeclaringClass();
		// System APIs
		if (AndroidSystemConstants.CLASSES.contains(clazz.getName())) {
			String signature = this.sha256(method.getSignature());
			SignatureGenerator.methodSignatures.put(method.getSignature(), signature);
			return signature;
		}
		
		List<String> sigRaw = new ArrayList<String>();
		sigRaw.add(Integer.toString(Util.countMatches(clazz.getName(), ".")));
		sigRaw.add(Integer.toString(method.getModifiers()));
		sigRaw.add(getTypeSignature(method.getReturnType()));
		
		for (Type ptype : method.getParameterTypes()) {
			sigRaw.add(getTypeSignature(ptype));
		}
		
		if (body != null) {
			// string constants
			Iterator<Unit> iter = body.getUnits().iterator();
			while (iter.hasNext()) {
				Stmt s = (Stmt) iter.next();
				List<ValueBox> vbs = s.getUseBoxes();
				for (ValueBox vb : vbs) {
					if (vb.getValue() instanceof StringConstant) {
						sigRaw.add(((StringConstant) vb.getValue()).value);
					}
				}
			}
			
			// method invocations
			iter = body.getUnits().iterator();
			while (iter.hasNext()) {
				Stmt s = (Stmt) iter.next();
				
				if (!s.containsInvokeExpr()) {
					continue;
				}
				
				SootMethod calleeMethod = s.getInvokeExpr().getMethod();
				sigRaw.add(generateMethodSignature(calleeMethod, null));
			}
			
			// method locals
			for (Local local : body.getLocals()) {
				sigRaw.add(getTypeSignature(local.getType()));
			}
		}
		
		String signature = this.sha256(Util.legacyJoin(",", sigRaw));
		if (body != null) {
			SignatureGenerator.methodSignatures.put(method.getSignature(), signature);
		}
		
		return signature;
	}
	
	private String getTypeSignature(Type type) {
		Type baseType = type;
		boolean isArrayType = (type instanceof ArrayType);
		if (isArrayType) {
			baseType = ((ArrayType) type).baseType;
		}
		
		if (AndroidSystemConstants.CLASSES.contains(baseType.toString())) {
			return type.toString();
		}
		
		if (baseType instanceof RefType) {
			SootClass clazz = ((RefType) baseType).getSootClass();
			
			while (true) {
				if (AndroidSystemConstants.CLASSES.contains(clazz.getName())) {
					String signature = type.toString().replace(baseType.toString(), clazz.getName());
					return signature;
				}
				
				if (clazz.hasSuperclass() == false) {
					break;
				}
				
				clazz = clazz.getSuperclass();
			}
		}
		
		return "Y";
	}

	private String bytesToHex(byte[] hash) {
	    StringBuilder hexString = new StringBuilder(2 * hash.length);
	    for (int i = 0; i < hash.length; i++) {
	        String hex = Integer.toHexString(0xff & hash[i]);
	        if(hex.length() == 1) {
	            hexString.append('0');
	        }
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}
	
	private String sha256(String message) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
			String sha256hex = new String(bytesToHex(hash));
			return sha256hex;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		SootMethod method = body.getMethod();
		try {
			generateMethodSignature(method, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
