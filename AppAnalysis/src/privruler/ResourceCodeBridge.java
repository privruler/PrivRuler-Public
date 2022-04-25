package privruler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import analysisutils.AnalysisAPIs;
import analysisutils.Globals;
import dfa.APIGraphNode;
import dfa.GlobalBackwardDataflowAnalysis;
import dfa.MyReachingDefinition;
import dfa.GlobalBackwardDataflowAnalysis.DDGHandler;
import dfa.util.Log;
import privruler.methodsignature.SignatureGenerator;
import soot.Body;
import soot.G;
import soot.Local;
import soot.Modifier;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.scalar.LocalCreation;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class ResourceCodeBridge {
	public static Map<String, String> stringLabelMap;
	public static Map<Integer, String> stringResMap;
	public static MultiMap<String, String> fieldValues;
	public static MultiMap<String, String> stmtValues;
	public static Map<?, ?> awsConfigMap;
	private GlobalBackwardDataflowAnalysis globalBackwardDataflowAnalysis;
	private Map<String, Map<String, String>> cloudFunctionLocationMap = new LinkedHashMap<String, Map<String, String>>();
	public static Map<String, String> obfuscatedMethods;

	static {
		try {
			stringLabelMap = ResourceParser.getInstance().getStringLabelMap();
			stringResMap = ResourceParser.getInstance().getStringResMap();
			awsConfigMap = ResourceParser.getInstance().parseAWSConfig();
		} catch (Exception e) {
			stringLabelMap = new HashMap<String, String>();
		}

		fieldValues = new HashMultiMap<String, String>();
		stmtValues = new HashMultiMap<String, String>();
		obfuscatedMethods = new HashMap<String, String>();
	};

	public void processCloudAPIs() {
		locateCloudAPIsAndLocalDefs();
		if (awsConfigMap != null) {
			modelAWSConfigurations();
		}

		locateCredWithDDG();
	}

	private void locateCloudAPIsAndLocalDefs() {
		for (String methodSig : Globals.REACHABLE_METHODS) {
			Log.dumpln("Reachable: " + methodSig);
		}

		Chain<SootClass> clazzes = Scene.v().getClasses();

		// locate cloud APIs, and local defs
		for (SootClass clazz : clazzes) {
			String clazzName = clazz.getName();

			// ignore internal clazz of cloud sdks
			if (clazzName.startsWith("com.amazonaws.") || clazzName.startsWith("com.aliyun.")
					|| clazzName.startsWith("com.alibaba.") || clazzName.startsWith("com.microsoft.")
					|| clazzName.startsWith("com.azure.")) {
				continue;
			}

			List<SootMethod> methods = clazz.getMethods();
			for (SootMethod method : methods) {
				if (!Globals.REACHABLE_METHODS.contains(method.getSignature())) {
					continue;
				}

				if (!method.hasActiveBody()) {
					continue;
				}

				Body body = method.getActiveBody();
				ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);
				MyReachingDefinition mrd = new MyReachingDefinition(eug);

				Iterator<Unit> iter = body.getUnits().iterator();
				while (iter.hasNext()) {
					Stmt s = (Stmt) iter.next();
					if (!s.containsInvokeExpr()) {
						continue;
					}

					SootMethod m = s.getInvokeExpr().getMethod();
					String mSig = m.getSignature();
					String mHash = SignatureGenerator.getMethodSignatures().get(mSig);

					if (mSig.startsWith("<com.amazonaws.")) {
						if (mHash != null) {
							Log.dumpln(String.format("SIGNATURE: %s -- %s", mSig, mHash));
						}

						if (m.getName().length() > 2 && m.getDeclaringClass().getShortName().length() > 2) {
							CloudManager.getInstance("AWS").addCloudAPI(method.getSignature(), mSig);
							Log.dumpln(String.format("NOT_OBFUSCATED:%s", mSig));
						} else {
							// obfuscated method
							if (mHash != null && SignatureGenerator.interestedSignatures.containsKey(mHash)) {
								CloudManager.getInstance("AWS").addCloudAPI(method.getSignature(),
										SignatureGenerator.interestedSignatures.get(mHash));
								Log.dumpln(String.format("OBFUSCATED:%s -- %s", mSig,
										SignatureGenerator.interestedSignatures.get(mHash)));
								obfuscatedMethods.put(mSig, SignatureGenerator.interestedSignatures.get(mHash));
							}
						}
					}

					if (mSig.startsWith("<com.microsoft.") || mSig.startsWith("<com.azure.")) {
						if (mHash != null) {
							Log.dumpln(String.format("SIGNATURE: %s -- %s", mSig, mHash));
						}

						if (m.getName().length() > 2 && m.getDeclaringClass().getShortName().length() > 2) {
							CloudManager.getInstance("AZURE").addCloudAPI(method.getSignature(), mSig);
							Log.dumpln(String.format("NOT_OBFUSCATED:%s", mSig));
						} else {
							// obfuscated method
							if (mHash != null && SignatureGenerator.interestedSignatures.containsKey(mHash)) {
								CloudManager.getInstance("AZURE").addCloudAPI(method.getSignature(),
										SignatureGenerator.interestedSignatures.get(mHash));
								Log.dumpln(String.format("OBFUSCATED:%s -- %s", mSig,
										SignatureGenerator.interestedSignatures.get(mHash)));
								obfuscatedMethods.put(mSig, SignatureGenerator.interestedSignatures.get(mHash));
							}
						}
					}

					if (mSig.startsWith("<com.alibaba.") || mSig.startsWith("<com.aliyun.")) {
						if (mHash != null) {
							Log.dumpln(String.format("SIGNATURE: %s -- %s", mSig, mHash));
						}

						if (m.getName().length() > 2 && m.getDeclaringClass().getShortName().length() > 2) {
							CloudManager.getInstance("ALIYUN").addCloudAPI(method.getSignature(), mSig);
							Log.dumpln(String.format("NOT_OBFUSCATED:%s", mSig));
						} else {
							// obfuscated method
							if (mHash != null && SignatureGenerator.interestedSignatures.containsKey(mHash)) {
								CloudManager.getInstance("ALIYUN").addCloudAPI(method.getSignature(),
										SignatureGenerator.interestedSignatures.get(mHash));
								Log.dumpln(String.format("OBFUSCATED:%s -- %s", mSig,
										SignatureGenerator.interestedSignatures.get(mHash)));
								obfuscatedMethods.put(mSig, SignatureGenerator.interestedSignatures.get(mHash));
							}
						}
					}
				}

				Iterator<Unit> iter2 = body.getUnits().iterator();
				while (iter2.hasNext()) {
					Stmt s = (Stmt) iter2.next();

					Collection<String> matched = Util.containedKeywords(s.toString(),
							CloudFunctionSummary.KEY_CONSUMPTION_APIS.keySet());

					for (String api : matched) {
						String cloudType = CloudFunctionSummary.KEY_CONSUMPTION_APIS.get(api)
								.get(CloudFunctionSummary.CLOUD_TYPE).getO1();

						if (CloudFunctionSummary.KEY_CONSUMPTION_APIS.get(api)
								.containsKey(CloudFunctionSummary.STMT_MATCH)) {
							String credentialName = CloudFunctionSummary.KEY_CONSUMPTION_APIS.get(api)
									.get(CloudFunctionSummary.STMT_MATCH).getO2();
							CloudManager.getInstance(cloudType).addAPICredential(credentialName,
									s.toString().substring(s.toString().indexOf('<')));
						}
					}

					if (!s.containsInvokeExpr()) {
						continue;
					}

					SootMethod m = s.getInvokeExpr().getMethod();
					String mSig = m.getSignature();
					if (obfuscatedMethods.containsKey(mSig)) {
						matched.addAll(Util.containedKeywords(obfuscatedMethods.get(mSig),
								CloudFunctionSummary.KEY_CONSUMPTION_APIS.keySet()));
					}

					for (String api : matched) {
						String cloudType = CloudFunctionSummary.KEY_CONSUMPTION_APIS.get(api)
								.get(CloudFunctionSummary.CLOUD_TYPE).getO1();
						boolean allArgResolved = true;

						Map<Integer, Pair<String, String>> args = CloudFunctionSummary.KEY_CONSUMPTION_APIS.get(api);
						for (Map.Entry<Integer, Pair<String, String>> argEntry : args.entrySet()) {
							// not an argument, ignore
							if (argEntry.getKey() < 0) {
								continue;
							}

							boolean argResolved = false;
							String argReg = argEntry.getValue().getO1();
							String argName = argEntry.getValue().getO2();

							Set<Stmt> handledStmts = new HashSet<Stmt>();
							Stack<Pair<Stmt, Set<Value>>> workStack = new Stack<Pair<Stmt, Set<Value>>>();
							Set<Value> argSet = new HashSet<Value>();
							argSet.add(s.getInvokeExpr().getArg(argEntry.getKey()));
							workStack.push(new Pair<Stmt, Set<Value>>(s, argSet));

							while (!workStack.isEmpty()) {
								Pair<Stmt, Set<Value>> pair = workStack.pop();
								Stmt curStmt = pair.getO1();
								Set<Value> curArgs = pair.getO2();
								handledStmts.add(curStmt);

								// collect string constants in current stmt
								for (Value vl : curArgs) {
									if (vl instanceof StringConstant) {
										String constant = ((StringConstant) vl).value;
										if (!constant.matches(argReg)) {
											continue;
										}
										CloudManager.getInstance(cloudType).addAPICredential(argName, constant);
										Log.dumpln(String.format("CLOUDPARAM-DIRECT:%s@%s", api, constant));
										argResolved = true;
									}
								}

								if (ResourceCodeBridge.stmtValues.containsKey(curStmt.toString())) {
									for (String constant : ResourceCodeBridge.stmtValues.get(curStmt.toString())) {
										if (!constant.matches(argReg)) {
											continue;
										}

										CloudManager.getInstance(cloudType).addAPICredential(argName, constant);
										Log.dumpln(String.format("CLOUDPARAM-DIRECT:%s@%s", api, constant));
										argResolved = true;
									}
								}

								for (Value argValue : curArgs) {
									if (argValue instanceof Local) {
										List<Unit> defs = mrd.getDefsOfAt((Local) argValue, curStmt);
										for (Unit def : defs) {
											if (handledStmts.contains(def)) {
												continue;
											}

											Stmt defStmt = (Stmt) def;
											Set<Value> defArgSet = new HashSet<Value>();
											for (ValueBox vb : defStmt.getUseBoxes()) {
												if (vb.getValue() instanceof Local) {
													defArgSet.add(vb.getValue());
												}
											}
											workStack.push(new Pair<Stmt, Set<Value>>(defStmt, defArgSet));
										}
									}
								}
							}

							if (!argResolved) {
								allArgResolved = false;
							}
						}

						if (!allArgResolved) {
							ResourceCodeBridge.this.saveCloudFunctionMapInfo(m.getSignature(), method);
						}
					}
				}
			}
		}
	}

	private void saveCloudFunctionMapInfo(String sig, SootMethod hostMethod) {
		String loc = hostMethod.getDeclaringClass().getName() + "|" + hostMethod.getSignature();

		Map<String, String> locations = null;
		if (this.cloudFunctionLocationMap.containsKey(sig)) {
			locations = this.cloudFunctionLocationMap.get(sig);
			locations.put(loc, hostMethod.getSubSignature());
		} else {
			locations = new LinkedHashMap<String, String>();
			locations.put(loc, hostMethod.getSubSignature());
			this.cloudFunctionLocationMap.put(sig, locations);
		}
	}

	private Map<String, Integer> getCloudFunctions() {
		Map<String, Integer> funcs = new LinkedHashMap<String, Integer>();
		int count = 0;
		try {
			Map<String, Map<String, String>> cloudFunctionMap = this.cloudFunctionLocationMap;
			for (String func : cloudFunctionMap.keySet()) {
				if (funcs.containsKey(func)) {
					continue;
				}

				funcs.put(func, new Integer(count));
				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return funcs;
	}

	private Set<String> getConstantsAtStmt(Stmt s) {
		Set<String> constants = new HashSet<String>();

		for (ValueBox ccVB : s.getUseBoxes()) {
			if (ccVB.getValue() instanceof StringConstant) {
				constants.add(((StringConstant) ccVB.getValue()).value);
			}
		}
		return constants;
	}

	public void locateCredWithDDG() {
		long startTime = 0;
		long endTime = 0;
		AnalysisAPIs.setupDefaultEntries();
		startTime = System.currentTimeMillis();
		try {
			doGlobalBackwardDataflow(this.getCloudFunctions(), this.cloudFunctionLocationMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		endTime = System.currentTimeMillis();
		Log.dumpln("doGlobalBackwardDataflow costs " + (endTime - startTime) / 1000 + " seconds");
	}

	private void doGlobalBackwardDataflow(Map<String, Integer> sinks,
			Map<String, Map<String, String>> sinksLocationMap) {
		G.reset();
		soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk);
		soot.options.Options.v().set_process_dir(Collections.singletonList(Globals.APK_PATH));
		soot.options.Options.v().set_android_jars(Globals.FRAMEWORK_DIR);
		soot.options.Options.v().set_whole_program(true);
		soot.options.Options.v().set_allow_phantom_refs(true);
		soot.options.Options.v().setPhaseOption("cg.spark", "on");
		soot.options.Options.v().set_process_multiple_dex(true);
		soot.options.Options.v().set_force_overwrite(true);
		soot.options.Options.v().set_include_all(true); // to ensure the new apk is fully executable.
		// soot.options.Options.v().set_exclude(Globals.EXCLUDED_LIBS);
		// soot.options.Options.v().set_no_bodies_for_excluded(true);
		soot.options.Options.v().set_output_format(soot.options.Options.output_format_dex);
		soot.options.Options.v().set_output_dir(Globals.OUTPUT_DIR);

		List<SootMethod> entryPoints = new ArrayList<SootMethod>();
		for (Pair<String, String> entry_item : Globals.ENTRY_POINTS) {
			try {
				SootClass c = Scene.v().forceResolve(entry_item.getO1(), SootClass.BODIES);
				c.setApplicationClass();
				Scene.v().loadNecessaryClasses();
				SootMethod m = c.getMethod(entry_item.getO2());
				entryPoints.add(m);
			} catch (Exception e) {
				Log.dumpln("fail to locate entry point: " + entry_item.getO1() + ":" + entry_item.getO2() + " "
						+ e.toString());
			}
		}

		if (entryPoints.size() <= 0) {
			Log.dumpln("null entry points, exiting ...");
			return;
		}

		globalBackwardDataflowAnalysis = new GlobalBackwardDataflowAnalysis(sinks, sinksLocationMap);
		globalBackwardDataflowAnalysis.setDDGHandler(new DDGHandler() {

			@SuppressWarnings("unchecked")
			@Override
			public void onDDGAvailable(String nodeSig, Stmt sinkStmt, LinkedHashMap<Stmt, APIGraphNode> stmtToNodeMap) {
				String apiSig = nodeSig.substring(0, nodeSig.indexOf('|'));

				// identify the order of methods in slice
				List<SootMethod> methodOrder = new ArrayList<SootMethod>();
				Map<SootMethod, Set<Stmt>> methodStmts = new HashMap<SootMethod, Set<Stmt>>();
				Map<SootMethod, Set<Local>> methodLocals = new HashMap<SootMethod, Set<Local>>();

				{
					Set<APIGraphNode> handledNodes = new HashSet<APIGraphNode>();
					Stack<APIGraphNode> stack = new Stack<APIGraphNode>();
					stack.add(stmtToNodeMap.get(sinkStmt));

					while (!stack.empty()) {
						APIGraphNode node = stack.pop();
						handledNodes.add(node);
						if (!methodOrder.contains(node.getHostMethod())) {
							methodOrder.add(0, node.getHostMethod());
							methodStmts.put(node.getHostMethod(), new HashSet<Stmt>());
							methodLocals.put(node.getHostMethod(), new HashSet<Local>());
						}
						methodStmts.get(node.getHostMethod()).add(node.getStmt());

						List<ValueBox> vbs = node.getStmt().getUseAndDefBoxes();
						for (ValueBox vb : vbs) {
							if (vb.getValue() instanceof Local) {
								methodLocals.get(node.getHostMethod()).add((Local) vb.getValue());
							}
						}

						List<APIGraphNode> predNodes = node.getPredecessors();
						for (APIGraphNode predNode : predNodes) {
							if (handledNodes.contains(predNode)) {
								continue;
							}
							stack.add(predNode);
						}
					}
				}
				
				// if 'return' is in ddg, we use original method
				for (Map.Entry<SootMethod, Set<Stmt>> methodStmtsEntry : methodStmts.entrySet()) {
					SootMethod sootMethod = methodStmtsEntry.getKey();
					Set<Stmt> sootStmts = methodStmtsEntry.getValue();
					
					boolean usesReturn = false;
					for (Stmt stmt : sootStmts) {
						if (stmt instanceof ReturnStmt) {
							usesReturn = true;
						}
					}
					
					if (sootMethod.getName().contains("sliced_method_") || usesReturn) {
						// avoid any recursion
						methodOrder.remove(sootMethod);
						Log.dumpln(String.format("[Ignore] %s", sootMethod.toString()));
					}
				}
				
				// generate sliced methods
				Map<SootMethod, SootMethod> methodMap = new HashMap<SootMethod, SootMethod>();
				for (SootMethod hostMethod : methodOrder) {
					try {
						// create a new method with the stmts in slice
						SootClass hostClass = hostMethod.getDeclaringClass();
						String newHostMethodName = String.format("sliced_method_%d_%d",
								Math.abs(hostMethod.getSignature().hashCode()), Math.abs(new Random().nextInt()));
						SootMethod newHostMethod = new SootMethod(newHostMethodName, hostMethod.getParameterTypes(),
								hostMethod.getReturnType(),
								hostMethod.isStatic() ? (Modifier.STATIC | Modifier.PUBLIC) : Modifier.PUBLIC);

						hostClass.addMethod(newHostMethod);
						JimpleBody newHostBody = Jimple.v().newBody(newHostMethod);
						newHostMethod.setActiveBody(newHostBody);

						Body hostBody = hostMethod.getActiveBody();
						for (Local l : hostBody.getLocals()) {
							if (methodLocals.get(hostMethod).contains(l)) {
								newHostBody.getLocals().add(l);
							}
						}

						Iterator<Unit> iter = hostBody.getUnits().iterator();
						while (iter.hasNext()) {
							Stmt s = (Stmt) iter.next();

							if (s.toString().contains(apiSig)) {
								// replace cloud api with logging method
								Local logRef = Jimple.v().newLocal("rlogging0", RefType.v("java.io.PrintStream"));
								newHostBody.getLocals().add(logRef);

								// rlogging0 = <java.lang.System: java.io.PrintStream out>;
								AssignStmt assign = Jimple.v().newAssignStmt(logRef, Jimple.v().newStaticFieldRef(
										Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef()));
								newHostBody.getUnits().add(assign);

								SootMethod printlnMethod = Scene.v()
										.getMethod("<java.io.PrintStream: void println(java.lang.String)>");

								newHostBody.getUnits()
										.add(Jimple.v()
												.newInvokeStmt(Jimple.v().newVirtualInvokeExpr(logRef,
														printlnMethod.makeRef(),
														StringConstant.v("AppPackage:" + Globals.PACKAGE_NAME))));
								newHostBody.getUnits()
										.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(logRef,
												printlnMethod.makeRef(), StringConstant.v("SinkMethod:" + apiSig))));

								Map<Integer, Pair<String, String>> args = CloudFunctionSummary.KEY_CONSUMPTION_APIS
										.get(apiSig);

								// obfuscated
								if (args == null) {
									if (ResourceCodeBridge.obfuscatedMethods.containsKey(apiSig)) {
										args = CloudFunctionSummary.KEY_CONSUMPTION_APIS
												.get(ResourceCodeBridge.obfuscatedMethods.get(apiSig));
									}
								}

								for (Map.Entry<Integer, Pair<String, String>> argEntry : args.entrySet()) {
									// not an argument, ignore
									if (argEntry.getKey() < 0) {
										continue;
									}

									int argIdx = argEntry.getKey();
									String argName = argEntry.getValue().getO2();
									Value argVal = s.getInvokeExpr().getArg(argIdx);

									newHostBody.getUnits()
											.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(logRef,
													printlnMethod.makeRef(), StringConstant.v("ArgName:" + argName))));
									newHostBody.getUnits().add(Jimple.v().newInvokeStmt(
											Jimple.v().newVirtualInvokeExpr(logRef, printlnMethod.makeRef(), argVal)));
								}
							} else if (s instanceof IdentityStmt
									&& (s.toString().contains("@this:") || s.toString().contains("@parameter"))) {
								// add identity for "this" and parameters
								newHostBody.getUnits().add(s);

								Value l = ((IdentityStmt) s).getLeftOp();
								if (l instanceof Local) {
									try {
										newHostBody.getLocals().add((Local) l);
									} catch (Exception e) {

									}
								}
							} else if (methodStmts.get(hostMethod).contains(s)) {
								newHostBody.getUnits().add(s);
							} else {
								// ensure necessary java rt objects are initialized
								if (s.containsInvokeExpr() && s.toString().contains("<init>")
										&& s.toString().contains("<java.lang.")) {
									SpecialInvokeExpr sie = (SpecialInvokeExpr) s.getInvokeExpr();
									if (newHostBody.getLocals().contains(sie.getBase())) {
										newHostBody.getUnits().add(s);
									}
								} else if (s instanceof GotoStmt) {
									// we don't instrument control flow, remain it as is.
									newHostBody.getUnits().add(s);
								} else if (s instanceof IfStmt) {
									boolean allConditionsDefined = true;
									for (ValueBox ub : ((IfStmt) s).getCondition().getUseBoxes()) {
										if (ub.getValue() instanceof Local) {
											if (!newHostBody.getLocals().contains(ub.getValue())) {
												allConditionsDefined = false;
												break;
											}
										}
									}

									if (allConditionsDefined) {
										newHostBody.getUnits().add(s);
									}
								}
							}
						}

						// add return within try{...} block
						if (!(newHostBody.getUnits().getLast() instanceof ReturnStmt)) {
							if (newHostMethod.getReturnType() instanceof VoidType) {
								newHostBody.getUnits().add(Jimple.v().newReturnVoidStmt());
							} else {
								newHostBody.getUnits().add(Jimple.v().newReturnStmt(
										MyUntypedConstant.v(0).defineType(newHostMethod.getReturnType())));
							}
						}

						Unit endUnit = newHostBody.getUnits().getLast();

						// add try-catch to each method
						Local catchRefLocal = Jimple.v().newLocal("r" + newHostMethodName,
								RefType.v("java.lang.Throwable"));
						newHostBody.getLocals().add(catchRefLocal);
						CaughtExceptionRef caughtRef = Jimple.v().newCaughtExceptionRef();
						Stmt caughtIdentity = Jimple.v().newIdentityStmt(catchRefLocal, caughtRef);
						newHostBody.getUnits().add(caughtIdentity);

						// trap
						Trap trap = Jimple.v().newTrap(Scene.v().getSootClass("java.lang.Throwable"),
								newHostBody.getUnits().getFirst(), endUnit, caughtIdentity);
						newHostBody.getTraps().add(trap);

						// add return after try-catch
						if (!(newHostBody.getUnits().getLast() instanceof ReturnStmt)) {
							if (newHostMethod.getReturnType() instanceof VoidType) {
								newHostBody.getUnits().add(Jimple.v().newReturnVoidStmt());
							} else {
								newHostBody.getUnits().add(Jimple.v().newReturnStmt(
										MyUntypedConstant.v(0).defineType(newHostMethod.getReturnType())));
							}
						}

						// ensure no stmt points out of UnitChain

						Iterator<Unit> newIter = newHostBody.getUnits().snapshotIterator();
						while (newIter.hasNext()) {
							Stmt s = (Stmt) newIter.next();
							if ((s instanceof GotoStmt && !newHostBody.getUnits().contains(((GotoStmt) s).getTarget()))
									|| (s instanceof IfStmt
											&& !newHostBody.getUnits().contains(((IfStmt) s).getTarget()))) {
								newHostBody.getUnits().remove(s);
							}
						}

						System.out.println(newHostBody);
						newHostBody.validate();
						methodMap.put(hostMethod, newHostMethod);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				// build invokes between sliced methods
				for (SootMethod hostMethod : methodOrder) {
					try {
						Body hostBody = hostMethod.getActiveBody();
						Iterator<Unit> iter = hostBody.getUnits().snapshotIterator();
						while (iter.hasNext()) {
							Stmt s = (Stmt) iter.next();
							
							if (s.containsInvokeExpr()) {
								SootMethod calleeMethod = s.getInvokeExpr().getMethod();
								if (methodMap.containsKey(calleeMethod)) {
									SootMethod replaceMethod = methodMap.get(calleeMethod);
									Log.dumpln(String.format("[Replace] %s --> %s", calleeMethod.toString(), replaceMethod.toString()));
									s.getInvokeExpr().setMethodRef(replaceMethod.makeRef());
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				// invoke the newly created method (in order) in launcher activity
				SootClass launcherClass = Scene.v().forceResolve(Globals.LAUNCHER_ACTIVITY_NAME, SootClass.BODIES);
				launcherClass.setApplicationClass();
				Scene.v().loadNecessaryClasses();

				/*
				 * String instruMethodName = "<init>"; SootMethod instruMethod = null; if
				 * (launcherClass.declaresMethodByName(instruMethodName)) { instruMethod =
				 * launcherClass.getMethodByName(instruMethodName); } else { instruMethod = new
				 * SootMethod(instruMethodName, Arrays.asList(new Type[] {}), VoidType.v(),
				 * Modifier.PUBLIC | Modifier.CONSTRUCTOR);
				 * launcherClass.addMethod(instruMethod); JimpleBody body =
				 * Jimple.v().newBody(instruMethod); instruMethod.setActiveBody(body);
				 * 
				 * Chain units = body.getUnits(); Local r0; r0 = Jimple.v().newLocal("r0",
				 * RefType.v(launcherClass.getName())); body.getLocals().add(r0);
				 * 
				 * units.add(Jimple.v().newIdentityStmt(r0,
				 * Jimple.v().newThisRef(RefType.v(launcherClass.getName()))));
				 * 
				 * { SootMethod toCall =
				 * Scene.v().getMethod("<java.lang.Object: void <init>()>");
				 * units.add(Jimple.v().newInvokeStmt( Jimple.v().newSpecialInvokeExpr(r0,
				 * toCall.makeRef()))); }
				 * 
				 * units.add(Jimple.v().newReturnVoidStmt()); }
				 */

				SootMethod instruMethod = null;
				try {
					instruMethod = launcherClass.getMethodByName("onCreate");
				} catch (Exception e1) {
					try {
						instruMethod = launcherClass.getMethodByName("onStart");
					} catch (Exception e2) {
					}
				}

				if (instruMethod == null) {
					Log.dumpln("Error: unable to find onStart/onCreate of launcher activity!");
					return;
				}

				Body instruBody = instruMethod.getActiveBody();
				Chain instruUnits = instruBody.getUnits();
				Iterator instruStmtIt = instruUnits.snapshotIterator();

				Unit endUnit = null;
				while (instruStmtIt.hasNext()) {
					Stmt instruStmt = (Stmt) instruStmtIt.next();

					// add to end of method
					if (instruStmt instanceof ReturnStmt || instruStmt instanceof ReturnVoidStmt) {
						// insert invoke right before "return"
						endUnit = instruStmt;
						break;
					}
				}

				for (SootMethod hostMethod : methodOrder) {
					SootMethod newHostMethod = methodMap.get(hostMethod);
					if (newHostMethod == null) {
						continue;
					}

					List<Value> params = new ArrayList<Value>();
					for (int idx = 0; idx < newHostMethod.getParameterCount(); ++idx) {
						Type paramType = newHostMethod.getParameterType(idx);
						params.add(MyUntypedConstant.v(0).defineType(paramType));
					}

					List<Unit> newUnits = new ArrayList<Unit>();
					if (newHostMethod.isStatic()) {
						Unit u1 = Jimple.v()
								.newInvokeStmt(Jimple.v().newStaticInvokeExpr(newHostMethod.makeRef(), params));
						newUnits.add(u1);
					} else {
						Local base = null;

						if (newHostMethod.getDeclaringClass().getName().equals(Globals.LAUNCHER_ACTIVITY_NAME)) {
							// use "this"
							base = instruBody.getThisLocal();
						} else {
							// create new "base"
							LocalCreation lc = new LocalCreation(instruBody.getLocals());
							base = lc.newLocal(RefType.v(newHostMethod.getDeclaringClass().getName()));

							Unit u1 = Jimple.v().newAssignStmt(base,
									Jimple.v().newNewExpr(RefType.v(newHostMethod.getDeclaringClass().getName())));
							newUnits.add(u1);

							Unit u2 = Jimple.v()
									.newInvokeStmt(
											Jimple.v()
													.newSpecialInvokeExpr(base,
															Scene.v().makeMethodRef(
																	Scene.v()
																			.getSootClass(newHostMethod
																					.getDeclaringClass().getName()),
																	"<init>", Arrays.asList(new Type[] {}),
																	VoidType.v(), false),
															new ArrayList<Value>()));
							newUnits.add(u2);
						}

						Unit u3 = Jimple.v()
								.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(base, newHostMethod.makeRef(), params));
						newUnits.add(u3);
					}

					if (newUnits.size() > 0) {
						instruUnits.insertBefore(newUnits, endUnit);
					}
				}
			}
		});

		Transform transformBackward = new Transform("wjtp.GlobalBackwardDataflowAnalysis",
				globalBackwardDataflowAnalysis);
		PackManager.v().getPack("wjtp").add(transformBackward);

		List<String> sootArgs = new ArrayList<String>();

		sootArgs.add("-no-bodies-for-excluded");

		String[] soot_args = new String[sootArgs.size()];
		for (int i = 0; i < sootArgs.size(); i++) {
			soot_args[i] = sootArgs.get(i);
		}

		Scene.v().setEntryPoints(entryPoints);
		soot.Main.main(soot_args);
	}

	// ddg is heavy-weight, so we handles straightforward and "local" cases here.
	public void bridgeStmtConstants() {
		Chain<SootClass> clazzes = Scene.v().getClasses();

		// record field values
		for (SootClass clazz : clazzes) {
			List<SootMethod> methods = clazz.getMethods();
			for (SootMethod method : methods) {
				if (!method.hasActiveBody()) {
					continue;
				}

				Body body = method.getActiveBody();
				Iterator<Unit> iter = body.getUnits().iterator();
				while (iter.hasNext()) {
					Stmt s = (Stmt) iter.next();

					if (s instanceof DefinitionStmt) {
						SootField field = null;
						Value lhs = ((DefinitionStmt) s).getLeftOp();

						if (lhs instanceof FieldRef) {
							field = ((FieldRef) lhs).getField();
							for (String strV : getConstantsAtStmt(s)) {
								ResourceCodeBridge.fieldValues.put(field.getSignature(), strV);
							}
						}
					}
				}
			}
		}

		// bridge stmt to resources
		for (SootClass clazz : clazzes) {
			String clazzName = clazz.getName();
			// ignore internal clazz of cloud sdks
			if (clazzName.startsWith("com.amazonaws.") || clazzName.startsWith("com.aliyun.")
					|| clazzName.startsWith("com.alibaba.") || clazzName.startsWith("com.microsoft.")
					|| clazzName.startsWith("com.azure.")) {
				continue;
			}

			List<SootMethod> methods = clazz.getMethods();
			for (SootMethod method : methods) {
				if (!method.hasActiveBody()) {
					continue;
				}

				Body body = method.getActiveBody();
				Iterator<Unit> iter = body.getUnits().iterator();
				while (iter.hasNext()) {
					Stmt s = (Stmt) iter.next();

					if (s instanceof DefinitionStmt) {
						Value rhs = ((DefinitionStmt) s).getRightOp();
						if (rhs instanceof FieldRef) {
							SootField rhsField = ((FieldRef) rhs).getField();

							if (ResourceCodeBridge.fieldValues.containsKey(rhsField.toString())) {
								ResourceCodeBridge.stmtValues.putAll(s.toString(),
										ResourceCodeBridge.fieldValues.get(rhsField.toString()));
							}
						} else if (rhs instanceof StringConstant) {
							ResourceCodeBridge.stmtValues.put(s.toString(), ((StringConstant) rhs).value);
						}
					}

					if (s.toString().contains(".R$string: int ")) {
						String ss = s.toString();
						String label = ss.substring(ss.indexOf(".R$string: int ") + ".R$string: int ".length(),
								ss.lastIndexOf('>'));
						if (ResourceCodeBridge.stringLabelMap.containsKey(label)) {
							String value = ResourceCodeBridge.stringLabelMap.get(label);
							ResourceCodeBridge.stmtValues.put(s.toString(), value);
						}
					}

					if (s.containsInvokeExpr()) {
						if (s.toString().contains("java.lang.String getString(int)")) {
							Value vl0 = s.getInvokeExpr().getArg(0);
							if (vl0 instanceof IntConstant) {
								int resid = ((IntConstant) vl0).value;
								if (ResourceCodeBridge.stringResMap.containsKey(resid)) {
									String value = ResourceCodeBridge.stringResMap.get(resid);
									ResourceCodeBridge.stmtValues.put(s.toString(), value);
								}
							}
						}
					}

					if (method.getSubSignature().equals("java.lang.String getAWSAccessKeyId()")) {
						if (ResourceCodeBridge.stmtValues.containsKey(s.toString())) {
							for (String value : ResourceCodeBridge.stmtValues.get(s.toString())) {
								CloudManager.getInstance("AWS").addAPICredential("AccessKey", value);
							}
						}
					}

					if (method.getSubSignature().equals("java.lang.String getAWSSecretKey()")) {
						if (ResourceCodeBridge.stmtValues.containsKey(s.toString())) {
							for (String value : ResourceCodeBridge.stmtValues.get(s.toString())) {
								CloudManager.getInstance("AWS").addAPICredential("SecretKey", value);
							}
						}
					}
				}
			}
		}
	}

	private void modelAWSConfigurations() {
		// model awsconfiguration.json manually
		MultiMap<String, List<String>> fieldsOfInterest = new HashMultiMap<String, List<String>>();
		fieldsOfInterest.put("IdentityPoolId", Arrays.asList("CredentialsProvider", "CognitoIdentity", "*", "PoolId"));
		fieldsOfInterest.put("Region", Arrays.asList("CredentialsProvider", "CognitoIdentity", "*", "Region"));
		fieldsOfInterest.put("UserPoolId", Arrays.asList("CognitoUserPool", "*", "PoolId"));
		fieldsOfInterest.put("AppClientId", Arrays.asList("CognitoUserPool", "*", "AppClientId"));
		fieldsOfInterest.put("AppClientSecret", Arrays.asList("CognitoUserPool", "*", "AppClientSecret"));
		fieldsOfInterest.put("Region", Arrays.asList("CognitoUserPool", "*", "Region"));
		fieldsOfInterest.put("Bucket", Arrays.asList("S3TransferUtility", "*", "Bucket"));
		fieldsOfInterest.put("Region", Arrays.asList("S3TransferUtility", "*", "Region"));
		fieldsOfInterest.put("Bucket", Arrays.asList("UserFileManager", "*", "Bucket"));
		fieldsOfInterest.put("Region", Arrays.asList("UserFileManager", "*", "Region"));

		for (String name : fieldsOfInterest.keySet()) {
			for (List<String> path : fieldsOfInterest.get(name)) {
				Stack<Object> objects = new Stack<Object>();
				objects.push(awsConfigMap);

				for (String field : path) {
					Stack<Object> newObjects = new Stack<Object>();
					if (!field.equals("*")) {
						while (!objects.empty()) {
							Object object = objects.pop();
							if ((object instanceof Map) && ((Map<?, ?>) object).containsKey(field)) {
								newObjects.push(((Map<?, ?>) object).get(field));
							}
						}
					} else {
						while (!objects.empty()) {
							Object object = objects.pop();
							if (object instanceof Map) {
								newObjects.addAll(((Map<?, ?>) object).values());
							}
						}
					}
					objects = newObjects;

					if (objects.size() < 1) {
						break;
					}
				}

				for (Object object : objects) {
					if (object instanceof String) {
						String str = (String) object;
						if (!str.matches(CloudFunctionSummary.GENERIC_REGEX)) {
							continue;
						}
						CloudManager.getInstance("AWS").addAPICredential(name, str);
						Log.dumpln(String.format("CLOUDPARAM-CONFIG:%s@%s", name, str));
					}
				}
			}
		}
	}
}
