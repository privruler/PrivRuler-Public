package dfa;

import dfa.util.Log;
import dfa.util.RandomIterator;
import dfa.util.UseWithScope;
import privruler.CloudFunctionSummary;

import java.util.*;
import soot.jimple.toolkits.callgraph.*;
import soot.util.Chain;
import soot.*;
import soot.jimple.*;
import soot.tagkit.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.Pair;

public class GlobalBackwardDataflowAnalysis extends SceneTransformer {
	public interface DDGHandler {
		void onDDGAvailable(String nodeSig, Stmt sinkStmt, LinkedHashMap<Stmt, APIGraphNode> stmtToNodeMap);
	}

	private DDGHandler ddgHandler;
	private CallGraph callGraph;
	private PointsToAnalysis pta;

	static LinkedHashMap<String, TaintTag> taintTagMap = new LinkedHashMap<String, TaintTag>();
	static LinkedHashMap<String, TaintTag> extraDefTagMap = new LinkedHashMap<String, TaintTag>();

	static LinkedHashMap<TaintTag, String> taintTagReverseMap = new LinkedHashMap<TaintTag, String>();
	static LinkedHashMap<TaintTag, String> extraDefTagReverseMap = new LinkedHashMap<TaintTag, String>();

	public static TaintTag API_TAG = new TaintTag(0xffff, "API_TAG");
	public static TaintTag STRING_CONST_TAG = new TaintTag(0xfffe, "STRING_CONST_TAG");
	private static LinkedHashMap<SootField, Vector<Integer>> usedStaticFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();
	private static LinkedHashMap<SootField, Vector<Integer>> usedInstanceFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();
	private int apiNodeCount = 0;

	private static List<SootField> taintedFieldsInCallee = new ArrayList<SootField>();
	private static List<SootField> taintedFieldsInCaller = new ArrayList<SootField>();

	private static Stack<SootMethod> callString = new Stack<SootMethod>();

	private static LinkedHashMap<String, List<Integer>> propagationHistory = new LinkedHashMap<String, List<Integer>>();

	private LinkedHashMap<SootMethod, List<APIGraphNode>> methodToDDGMap = new LinkedHashMap<SootMethod, List<APIGraphNode>>();

	private LinkedHashMap<Stmt, APIGraphNode> stmtToNodeMap = new LinkedHashMap<Stmt, APIGraphNode>();

	private LinkedHashMap<SootField, List<Stmt>> fieldToUsesMap = new LinkedHashMap<SootField, List<Stmt>>();
	private LinkedHashMap<SootField, List<Stmt>> fieldToDefsMap = new LinkedHashMap<SootField, List<Stmt>>();
	private Map<String, Map<String, String>> sinksLocationMap = null;

	public GlobalBackwardDataflowAnalysis(Map<String, Integer> sinks,
			Map<String, Map<String, String>> sinksLocationMap) {
		Set<String> sinkKeySet = sinks.keySet();
		this.sinksLocationMap = sinksLocationMap;

		Iterator<String> sinkIter = sinkKeySet.iterator();
		while (sinkIter.hasNext()) {
			String sink = sinkIter.next();
			if (!taintTagMap.containsKey(sink)) {
				TaintTag tag = new TaintTag(sinks.get(sink).intValue(), "taint_sink" + sinks.get(sink).intValue());
				taintTagMap.put(sink, tag);
				taintTagReverseMap.put(tag, sink);
			}

			if (!extraDefTagMap.containsKey(sink)) {
				TaintTag tag = new TaintTag(sinks.get(sink).intValue(), "extra_def_sink" + sinks.get(sink).intValue());
				extraDefTagMap.put(sink, tag);
				extraDefTagReverseMap.put(tag, sink);
			}
		}
	}

	public void setDDGHandler(DDGHandler ddgHandler) {
		this.ddgHandler = ddgHandler;
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void internalTransform(String string, Map map) {
		this.callGraph = Scene.v().getCallGraph();
		this.pta = Scene.v().getPointsToAnalysis();
		doDataFlowAnalysis();
		clearDataStructures();
	}

	private void clearDataStructures() {
		usedStaticFieldMap.clear();
		usedInstanceFieldMap.clear();
		taintedFieldsInCallee.clear();
		taintedFieldsInCaller.clear();
		callString.clear();
		propagationHistory.clear();
		methodToDDGMap.clear();
		stmtToNodeMap.clear();
		fieldToUsesMap.clear();
		fieldToDefsMap.clear();
	}

	private boolean isDefWithParam(Stmt s) {
		return (s instanceof IdentityStmt);
	}

	@SuppressWarnings("rawtypes")
	private void doDataFlowAnalysis() {
		System.out.println("starting backward dataflow analysis...");

		Set<String> sinkKey = this.sinksLocationMap.keySet();
		Iterator<String> sinkIter = new RandomIterator<String>(sinkKey.iterator());
		while (sinkIter.hasNext()) {
			Set<SootMethod> checkedUpwards = new LinkedHashSet<SootMethod>();
			String flowSink = sinkIter.next();

			TaintTag taintTag = taintTagMap.get(flowSink);
			TaintTag extraDefTag = extraDefTagMap.get(flowSink);

			LinkedHashMap<SootField, Vector<Integer>> instanceFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();
			LinkedHashMap<SootField, Vector<Integer>> staticFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();

			Map<String, String> entryPointsString = this.sinksLocationMap.get(flowSink);
			List<SootMethod> entryPoints = new ArrayList<SootMethod>();
			List<SootMethod> upwardMethods = new ArrayList<SootMethod>();

			Set<String> keySet = entryPointsString.keySet();
			String stmtMethod = "";
			String entrySig = "";

			Iterator<String> keyIterator = new RandomIterator<String>(keySet.iterator());
			while (keyIterator.hasNext()) {
				String hostSig = keyIterator.next();
				String hostSubsig = entryPointsString.get(hostSig);
				entrySig = flowSink + "|" + hostSig;

				stmtMethod = hostSig.substring(hostSig.indexOf("|") + 1);
				hostSig = hostSig.substring(0, hostSig.indexOf("|"));

				try {
					SootClass entryClass = Scene.v().forceResolve(hostSig, SootClass.BODIES);
					entryClass.setApplicationClass();
					Scene.v().loadNecessaryClasses();
					SootMethod entryMethod = entryClass.getMethod(hostSubsig);
					entryPoints.add(entryMethod);
					upwardMethods.add(entryMethod);

					if (MyConstants.DEBUG_INFO) {
						System.out.println("doDataFlowAnalysis for " + entrySig + " in " + entryMethod.toString());
					}
				} catch (Exception e) {
					System.out.println("doDataFlowAnalysis for " + entrySig + "Exception " + e.toString());
				}

				Queue<Edge> pendingIntoEdges = new LinkedList<Edge>();
				do {
					if (!pendingIntoEdges.isEmpty()) {
						entryPoints.clear();
						upwardMethods.clear();

						Edge pendingTop = pendingIntoEdges.element();
						Edge pendingCur = null;

						// set flowSink as last processed method, and entry point the upstream method
						// invoke it.
						flowSink = pendingTop.srcStmt().toString();
						while (!pendingIntoEdges.isEmpty()) {
							pendingCur = pendingIntoEdges.element();
							if (pendingCur.getTgt() == pendingTop.getTgt()) {
								pendingCur = pendingIntoEdges.remove();
								SootMethod srcMethod = pendingCur.getSrc().method();
								SootMethod dstMethod = pendingCur.getTgt().method();

								if (checkedUpwards.contains(srcMethod)) {
									continue;
								}

								entryPoints.add(srcMethod);
								upwardMethods.add(srcMethod);

								if (MyConstants.DEBUG_INFO) {
									System.out.println("doDataFlowAnalysis for edge " + srcMethod.toString() + "==>"
											+ dstMethod.toString());
								}

								APIGraphNode callerNode = null;
								JimpleBody srcBody = (JimpleBody) srcMethod.retrieveActiveBody();

								if (MyConstants.DEBUG_INFO) {
									System.out.println("doDataFlowAnalysis, matching flowSink " + flowSink);
								}

								// match method signature first
								Iterator<Unit> srcIt = srcBody.getUnits().iterator();
								while (srcIt.hasNext()) {
									Stmt s = (Stmt) srcIt.next();
									Iterator<ValueBox> useIt = s.getUseBoxes().iterator();

									while (useIt.hasNext()) {
										ValueBox vBox = useIt.next();
										if (vBox.getValue() instanceof InvokeExpr) {
											if (flowSink
													.equals(((InvokeExpr) vBox.getValue()).getMethod().getSignature())
													|| flowSink.equals(((InvokeExpr) vBox.getValue()).getMethodRef()
															.getSignature())) {
												callerNode = CreateOrGetExistingNode(s, srcMethod);
												if (MyConstants.DEBUG_INFO) {
													System.out.println("doDataFlowAnalysis, found flowSink " + s);
												}
												break;
											}
										}
									}

									if (callerNode != null) {
										break;
									}
								}

								// approximate matching of method subsignature
								if (callerNode == null) {
									Iterator<Unit> secondSrcIt = srcBody.getUnits().iterator();
									while (secondSrcIt.hasNext()) {
										Stmt s = (Stmt) secondSrcIt.next();
										Iterator<ValueBox> useIt = s.getUseBoxes().iterator();

										while (useIt.hasNext()) {
											ValueBox vBox = useIt.next();
											if (vBox.getValue() instanceof InvokeExpr) {
												// approximate matching of method signature, handles inheritance
												if (flowSink.contains(
														((InvokeExpr) vBox.getValue()).getMethod().getSubSignature())) {
													callerNode = CreateOrGetExistingNode(s, srcMethod);
													flowSink = ((InvokeExpr) vBox.getValue()).getMethod()
															.getSignature();

													if (MyConstants.DEBUG_INFO) {
														System.out.println(
																"doDataFlowAnalysis, found approximate flowSink " + s);
													}
													break;
												}
											}
										}

										if (callerNode != null) {
											break;
										}
									}
								}

								if (callerNode != null) {
									Set<Stmt> ddgKeySet = this.stmtToNodeMap.keySet();
									Iterator<Stmt> ddgIter = ddgKeySet.iterator();

									while (ddgIter.hasNext()) {
										Stmt s = ddgIter.next();
										APIGraphNode ddgNode = this.stmtToNodeMap.get(s);
										if (isDefWithParam(ddgNode.getStmt()) && ddgNode.getHostMethod().toString()
												.equals(pendingCur.getTgt().toString())) {
											ddgNode.addPred(callerNode);
											callerNode.addSucc(ddgNode);
										}
									}
								}
							} else {
								break;
							}
						}
					}

					Queue<SootMethod> worklist = new LinkedList<SootMethod>();
					List<SootMethod> fullWorklist = new LinkedList<SootMethod>();
					List<SootField> taintedFields = new ArrayList<SootField>();

					worklist.addAll(entryPoints);
					fullWorklist.addAll(entryPoints);
					checkedUpwards.addAll(entryPoints);

					// If dataflow reaches a "Identity" statement, we put caller name
					// into sourceMethods. Further we track the dataflow in caller,
					// starting from such function call.
					List<SootMethod> sourceMethods = new ArrayList<SootMethod>();

					boolean breakAnalysis = false;
					// dataflow analysis phase one
					while (!worklist.isEmpty()) {
						if (apiNodeCount > MyConstants.MAX_APINODES_CONSIDERED) {
							apiNodeCount = 0;
							breakAnalysis = true;

							if (MyConstants.DEBUG_INFO) {
								System.out.println("breakout#1:" + flowSink + " API_NODE_COUNT = " + apiNodeCount);
							}

							break;
						}

						SootMethod sMethod = worklist.remove();
						if (!methodToDDGMap.containsKey(sMethod)) {
							List<APIGraphNode> apiGraph = new ArrayList<APIGraphNode>();
							methodToDDGMap.put(sMethod, apiGraph);
						}

						JimpleBody body = (JimpleBody) sMethod.retrieveActiveBody();
						ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);
						MyReachingDefinition mrd = new MyReachingDefinition(eug);
						Stack<UseWithScope> usesStack = new Stack<UseWithScope>();
						LinkedHashMap<Stmt, Vector<Stmt>> uses = new LinkedHashMap<Stmt, Vector<Stmt>>();

						Stmt sink = null;
						{
							Iterator<Unit> it = body.getUnits().iterator();
							while (it.hasNext()) {
								Stmt s = (Stmt) it.next();
								Iterator<ValueBox> useIt = s.getUseBoxes().iterator();

								while (useIt.hasNext()) {
									ValueBox vBox = useIt.next();
									if (vBox.getValue() instanceof InvokeExpr) {
										if (((InvokeExpr) vBox.getValue()).getMethod().getSignature().equals(flowSink)
												|| sourceMethods.contains(((InvokeExpr) vBox.getValue()).getMethod())) {
											UseWithScope sWS = new UseWithScope(s, s);
											if (!uses.containsKey(s)) {
												uses.put(s, new Vector<Stmt>());
												usesStack.push(sWS);
												sink = s;
											}
										}
									}
								}
							}
						}

						while (!usesStack.isEmpty()) {
							UseWithScope useWS = usesStack.pop();
							if (MyConstants.DEBUG_INFO)
								System.out.println("POP from use stack: " + useWS.dump());

							// use-def analysis
							Stmt s = useWS.getUse();
							Stmt sScope = useWS.getScopeEnd();

							if (s.containsInvokeExpr()) {
								if (!s.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()) {
									AddTags(s, API_TAG);
								}
							}

							if (s instanceof DefinitionStmt) {
								boolean usesConstant = false;
								List<ValueBox> checkConstUseBoxes = s.getUseBoxes();
								for (ValueBox ccVB : checkConstUseBoxes) {
									if (ccVB.getValue() instanceof StringConstant) {
										if (!((StringConstant) ccVB.getValue()).value.equals("")) {
											usesConstant = true;
											break;
										}
									}
								}
								if (usesConstant) {
									AddTags(s, STRING_CONST_TAG);
								}
							}

							APIGraphNode sNode = CreateOrGetExistingNode(s, sMethod);
							if (!methodToDDGMap.get(sMethod).contains(sNode)) {
								methodToDDGMap.get(sMethod).add(sNode);
							}

							if (s instanceof InvokeStmt) {
								String sInvokeSignature = s.getInvokeExpr().getMethod().getSignature();
								if (sInvokeSignature.equals(flowSink)
										|| sourceMethods.contains(s.getInvokeExpr().getMethod())) {
									// decide if we need to check backward data flow for all use boxes, or selected
									// arguments
									List<Value> usesValues = new ArrayList<Value>();
									if (CloudFunctionSummary.KEY_CONSUMPTION_APIS.containsKey(sInvokeSignature)) {
										// track selected arguments for cloud APIs

										Map<Integer, Pair<String, String>> args = CloudFunctionSummary.KEY_CONSUMPTION_APIS
												.get(sInvokeSignature);
										for (Map.Entry<Integer, Pair<String, String>> argEntry : args.entrySet()) {
											// not an argument, ignore
											if (argEntry.getKey() < 0) {
												continue;
											}

											usesValues.add(s.getInvokeExpr().getArg(argEntry.getKey()));
										}
									} else {
										// track all use boxes
										List<ValueBox> usesBoxes = s.getUseBoxes();
										Iterator usesIter = usesBoxes.iterator();
										while (usesIter.hasNext()) {
											ValueBox usesBox = (ValueBox) usesIter.next();
											usesValues.add(usesBox.getValue());
										}
									}

									for (Value value : usesValues) {
										if (value instanceof Local) {
											List<Unit> defs = mrd.getDefsOfAt((Local) value, s);
											for (Unit def : defs) {

												APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
												;

												sNode.addPred(defNode);
												defNode.addSucc(sNode);
												if (!methodToDDGMap.get(sMethod).contains(defNode)) {
													methodToDDGMap.get(sMethod).add(defNode);
												}

												UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
												if (!uses.containsKey(def)) {
													Vector<Stmt> scopes = new Vector<Stmt>();
													scopes.add(s);
													uses.put((Stmt) def, scopes);
													usesStack.push(defofuseWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																"use stack: " + defofuseWS.dump() + ". Push it.");
												} else {
													Vector<Stmt> scopes = uses.get(def);
													if (!scopes.contains(s)) {
														scopes.add(s);
														usesStack.push(defofuseWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println(
																	"use stack: " + defofuseWS.dump() + ". Push it.");
													}
												}
											}
										}
									}
								}

							}
							// added for NLP project
							else if ((s instanceof IfStmt || s instanceof TableSwitchStmt
									|| s instanceof LookupSwitchStmt) && s.toString().contains(flowSink)
									&& sMethod.getSignature().equals(stmtMethod)) {
								// System.err.println("location2: " + s);
								List<ValueBox> usesBoxes = s.getUseBoxes();
								Iterator usesIter = usesBoxes.iterator();
								while (usesIter.hasNext()) {
									ValueBox usesBox = (ValueBox) usesIter.next();
									// System.err.println("use box: " +
									// usesBox.toString());
									if (usesBox.getValue() instanceof Local) {
										List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
										for (Unit def : defs) {
											// System.err.println("def: " +
											// def.toString());
											APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
											;

											sNode.addPred(defNode);
											defNode.addSucc(sNode);
											if (!methodToDDGMap.get(sMethod).contains(defNode)) {
												methodToDDGMap.get(sMethod).add(defNode);
											}

											UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
											if (!uses.containsKey(def)) {
												Vector<Stmt> scopes = new Vector<Stmt>();
												scopes.add(s);
												uses.put((Stmt) def, scopes);
												usesStack.push(defofuseWS);
												if (MyConstants.DEBUG_INFO)
													System.out
															.println("use stack: " + defofuseWS.dump() + ". Push it.");
											} else {
												Vector<Stmt> scopes = uses.get(def);
												if (!scopes.contains(s)) {
													scopes.add(s);
													usesStack.push(defofuseWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																"use stack: " + defofuseWS.dump() + ". Push it.");
												}
											}
										}
									}
								}
							} else {
								boolean isInvoke = false;

								Iterator iUse = s.getUseBoxes().iterator();
								while (iUse.hasNext()) {
									ValueBox vB = (ValueBox) iUse.next();
									if (vB.getValue() instanceof InvokeExpr) {
										isInvoke = true;
									}
								}

								// rhs is invoke, lhs is ret
								if (isInvoke) {

									if (MyConstants.DEBUG_INFO) {
										System.out.println("invoke sig: " + s.getInvokeExpr().getMethod().getSignature()
												+ " flowSink:" + flowSink);
									}

									if (s.getInvokeExpr().getMethod().getSignature().equals(flowSink)
											|| sourceMethods.contains(s.getInvokeExpr().getMethod())) {

										List<ValueBox> usesBoxes = s.getUseBoxes();
										Iterator usesIter = usesBoxes.iterator();
										while (usesIter.hasNext()) {
											ValueBox usesBox = (ValueBox) usesIter.next();
											if (usesBox.getValue() instanceof Local) {
												List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
												for (Unit def : defs) {

													APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
													;

													sNode.addPred(defNode);
													defNode.addSucc(sNode);
													if (!methodToDDGMap.get(sMethod).contains(defNode)) {
														methodToDDGMap.get(sMethod).add(defNode);
													}

													UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
													if (!uses.containsKey(def)) {
														Vector<Stmt> scopes = new Vector<Stmt>();
														scopes.add(s);
														uses.put((Stmt) def, scopes);
														usesStack.push(defofuseWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println(
																	"use stack: " + defofuseWS.dump() + ". Push it.");
													} else {
														Vector<Stmt> scopes = uses.get(def);
														if (!scopes.contains(s)) {
															scopes.add(s);
															usesStack.push(defofuseWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack: " + defofuseWS.dump()
																		+ ". Push it.");
														}
													}
												}
											}
										}
									}

									else {

										if (s instanceof DefinitionStmt) {
											Value lhs = ((DefinitionStmt) s).getLeftOp();

											if (MyConstants.CONSIDER_REDEFINE && lhs.getType() instanceof RefLikeType) {

												if (MyConstants.DEBUG_INFO)
													System.out.println("#1:looking for redefine:" + s);

												Iterator itForRedefine = body.getUnits().iterator();

												while (itForRedefine.hasNext()) {
													Stmt stmt = (Stmt) itForRedefine.next();

													if (!isInScope(eug, stmt, sScope)) {
														if (MyConstants.DEBUG_INFO)
															System.out.println(
																	stmt + " is NOT in scope[<--" + sScope + "]");
														continue;
													}

													boolean isStmtUsingS = false;
													List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
													for (ValueBox useBox : useBoxesofStmt) {
														if (useBox.getValue() instanceof Local) {
															if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt)
																	.contains(s)) {
																isStmtUsingS = true;
																break;
															}
														}
													}

													if (isStmtUsingS) {
														if (MyConstants.DEBUG_INFO)
															System.out.println(stmt + " IS using " + s);

														if (stmt.containsInvokeExpr()) {
															if (!stmt.getInvokeExpr().getMethod().getDeclaringClass()
																	.isApplicationClass()) {
																AddTags(stmt, API_TAG);
															}
														}

														if (stmt instanceof DefinitionStmt) {
															// if(!stmt.containsInvokeExpr()){
															boolean usesConstant = false;
															List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
															for (ValueBox ccVB : checkConstUseBoxes) {
																if (ccVB.getValue() instanceof StringConstant) {
																	if (!((StringConstant) ccVB.getValue()).value
																			.equals("")) {
																		usesConstant = true;
																		break;
																	}
																}
															}
															if (usesConstant) {
																AddTags(stmt, STRING_CONST_TAG);
															}
															// }
														}

														APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);

														if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
															methodToDDGMap.get(sMethod).add(stmtNode);
														}

														APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope,
																sMethod);

														if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
															methodToDDGMap.get(sMethod).add(sScopeNode);
														}

														sNode.removeSucc(sScopeNode);
														sScopeNode.removePred(sNode);

														sNode.addSucc(stmtNode);
														stmtNode.addPred(sNode);

														stmtNode.addSucc(sScopeNode);
														sScopeNode.addPred(stmtNode);

														if (stmt instanceof InvokeStmt) {

															Vector<Integer> taintVector = new Vector<Integer>();

															Iterator defIt2 = s.getDefBoxes().iterator();
															while (defIt2.hasNext()) {
																ValueBox vbox2 = (ValueBox) defIt2.next();
																if (vbox2.getValue() instanceof Local) {
																	// System.out.println(vbox2.getValue());
																	InvokeExpr invokeEx = stmt.getInvokeExpr();
																	int argCount = invokeEx.getArgCount();
																	for (int i = 0; i < argCount; i++) {
																		if (invokeEx.getArg(i) == vbox2.getValue()) {
																			taintVector.add(i);
																		}
																	}

																	// for instance
																	// invoke, consider
																	// this reference
																	// too.
																	if (invokeEx instanceof InstanceInvokeExpr) {
																		if (((InstanceInvokeExpr) invokeEx)
																				.getBase() == vbox2.getValue()) {
																			taintVector.add(new Integer(
																					MyConstants.thisObject));
																		}
																	}
																}
															}

															Iterator targets = null;
															if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(stmt + " calls CONCRETE method: "
																			+ stmt.getInvokeExpr().getMethod());
																List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																defaultTargets.add(stmt.getInvokeExpr().getMethod());
																targets = defaultTargets.iterator();
															} else {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(
																			stmt + " calls NON-CONCRETE method: "
																					+ stmt.getInvokeExpr().getMethod());
																targets = new Targets(this.callGraph.edgesOutOf(stmt));

																if (!targets.hasNext()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " does NOT have a target. add a DEFAULT one");
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																}
															}

															if (targets == null) {
																continue;
															}

															while (targets.hasNext()) {
																SootMethod target = (SootMethod) targets.next();

																boolean noNewTaint = true;

																String propKey = sMethod.getSignature() + "|"
																		+ stmt.toString() + "|" + Integer.toHexString(
																				System.identityHashCode(stmt));

																if (!propagationHistory.containsKey(propKey)) {
																	noNewTaint = false;
																	List<Integer> sinks = new ArrayList<Integer>();
																	sinks.addAll(taintVector);
																	propagationHistory.put(propKey, sinks);
																} else {
																	List<Integer> sinks = propagationHistory
																			.get(propKey);

																	for (Integer taint : taintVector) {
																		if (!sinks.contains(taint)) {
																			noNewTaint = false;
																			sinks.add(taint);
																		}
																	}
																}

																if (noNewTaint) {
																	break;
																}

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(
																			"PROPAGATING from METHOD: " + sMethod);
																	System.out.println(
																			"PROPAGATING from STATEMENT: " + stmt);
																}
																taintedFieldsInCaller.addAll(taintedFields);
																Vector<Integer> tainted = propagate(target, taintVector,
																		flowSink, stmt, sMethod);
																for (SootField sf : taintedFieldsInCallee) {
																	if (!taintedFields.contains(sf)) {
																		taintedFields.add(sf);
																	}
																}
																taintedFieldsInCallee.clear();

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(stmt + " |taint:" + taintVector
																			+ "| PROPAGATION result: " + tainted);
																}
																if ((tainted != null) && (!tainted.isEmpty())) {
																	for (Integer i : tainted) {
																		int index = i.intValue();

																		if (index == MyConstants.thisObject) {
																			if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																				Value taintedThisRef = ((InstanceInvokeExpr) stmt
																						.getInvokeExpr()).getBase();

																				if (taintedThisRef instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedThisRef,
																							stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);

																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}

																			}

																		} else if (index >= 0) {

																			Value taintedArg = stmt.getInvokeExpr()
																					.getArg(index);

																			if (taintedArg instanceof Local) {
																				List<Unit> defs0 = mrd.getDefsOfAt(
																						(Local) taintedArg, stmt);

																				for (Unit defn : defs0) {

																					APIGraphNode defNode = CreateOrGetExistingNode(
																							(Stmt) defn, sMethod);

																					stmtNode.addPred(defNode);
																					defNode.addSucc(stmtNode);
																					if (!methodToDDGMap.get(sMethod)
																							.contains(defNode)) {
																						methodToDDGMap.get(sMethod)
																								.add(defNode);
																					}

																					UseWithScope defnWS = new UseWithScope(
																							(Stmt) defn, stmt);
																					if (!uses.containsKey(defn)) {
																						Vector<Stmt> scopes = new Vector<Stmt>();
																						scopes.add(stmt);
																						uses.put((Stmt) defn, scopes);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					} else if (!(uses.get(defn)
																							.contains(stmt))) {
																						uses.get(defn).add(stmt);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					}
																				}
																			}
																		}
																	}
																}

															}

														} else if (stmt instanceof DefinitionStmt) {

															Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
															if (rhsInvoke instanceof InvokeExpr) {

																Vector<Integer> taintVector = new Vector<Integer>();

																Iterator defIt2 = s.getDefBoxes().iterator();
																while (defIt2.hasNext()) {
																	ValueBox vbox2 = (ValueBox) defIt2.next();
																	if (vbox2.getValue() instanceof Local) {
																		// System.out.println(vbox2.getValue());
																		InvokeExpr invokeEx = stmt.getInvokeExpr();
																		int argCount = invokeEx.getArgCount();
																		for (int i = 0; i < argCount; i++) {
																			if (invokeEx.getArg(i) == vbox2
																					.getValue()) {
																				taintVector.add(i);
																			}
																		}

																		// for instance
																		// invoke,
																		// consider this
																		// reference
																		// too.
																		if (invokeEx instanceof InstanceInvokeExpr) {
																			if (((InstanceInvokeExpr) invokeEx)
																					.getBase() == vbox2.getValue()) {
																				taintVector.add(MyConstants.thisObject);
																			}
																		}
																	}
																}

																Iterator targets = null;
																if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																} else {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls NON-CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	targets = new Targets(
																			this.callGraph.edgesOutOf(stmt));

																	if (!targets.hasNext()) {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(stmt
																					+ " does NOT have a target. add a DEFAULT one");
																		List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																		defaultTargets
																				.add(stmt.getInvokeExpr().getMethod());
																		targets = defaultTargets.iterator();
																	}
																}

																if (targets == null) {
																	continue;
																}

																while (targets.hasNext()) {
																	SootMethod target = (SootMethod) targets.next();

																	boolean noNewTaint = true;

																	String propKey = sMethod.getSignature() + "|"
																			+ stmt.toString() + "|"
																			+ Integer.toHexString(
																					System.identityHashCode(stmt));

																	if (!propagationHistory.containsKey(propKey)) {
																		noNewTaint = false;
																		List<Integer> sinks = new ArrayList<Integer>();
																		sinks.addAll(taintVector);
																		propagationHistory.put(propKey, sinks);
																	} else {
																		List<Integer> sinks = propagationHistory
																				.get(propKey);

																		for (Integer taint : taintVector) {
																			if (!sinks.contains(taint)) {
																				noNewTaint = false;
																				sinks.add(taint);
																			}
																		}
																	}

																	if (noNewTaint) {
																		break;
																	}

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(
																				"PROPAGATING from METHOD: " + sMethod);
																		System.out.println(
																				"PROPAGATING from STATEMENT: " + stmt);
																	}
																	taintedFieldsInCaller.addAll(taintedFields);
																	Vector<Integer> tainted = propagate(target,
																			taintVector, flowSink, stmt, sMethod);
																	for (SootField sf : taintedFieldsInCallee) {
																		if (!taintedFields.contains(sf)) {
																			taintedFields.add(sf);
																		}
																	}
																	taintedFieldsInCallee.clear();

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(stmt + " |taint:"
																				+ taintVector + "| PROPAGATION result: "
																				+ tainted);
																	}
																	if ((tainted != null) && (!tainted.isEmpty())) {

																		for (Integer i : tainted) {
																			int index = i.intValue();

																			if (index == MyConstants.thisObject) {
																				if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																					Value taintedThisRef = ((InstanceInvokeExpr) stmt
																							.getInvokeExpr()).getBase();

																					if (taintedThisRef instanceof Local) {
																						List<Unit> defs0 = mrd
																								.getDefsOfAt(
																										(Local) taintedThisRef,
																										stmt);

																						for (Unit defn : defs0) {

																							APIGraphNode defNode = CreateOrGetExistingNode(
																									(Stmt) defn,
																									sMethod);

																							stmtNode.addPred(defNode);
																							defNode.addSucc(stmtNode);
																							if (!methodToDDGMap
																									.get(sMethod)
																									.contains(
																											defNode)) {
																								methodToDDGMap
																										.get(sMethod)
																										.add(defNode);
																							}

																							UseWithScope defnWS = new UseWithScope(
																									(Stmt) defn, stmt);
																							if (!uses.containsKey(
																									defn)) {
																								Vector<Stmt> scopes = new Vector<Stmt>();
																								scopes.add(stmt);
																								uses.put((Stmt) defn,
																										scopes);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							} else if (!(uses.get(defn)
																									.contains(stmt))) {
																								uses.get(defn)
																										.add(stmt);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							}
																						}
																					}

																				}

																			} else if (index >= 0) {

																				Value taintedArg = stmt.getInvokeExpr()
																						.getArg(index);

																				if (taintedArg instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedArg, stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);

																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}
																			}
																		}
																	}

																}
															}
														}

													} // isStmtUsingS
												}
											} // if(lhs.getType() instanceof
												// RefLikeType){
										}

										Vector<Integer> taintVector = new Vector<Integer>();
										taintVector.add(MyConstants.returnValue);

										Iterator targets = null;
										if (s.getInvokeExpr().getMethod().isConcrete()) {
											if (MyConstants.DEBUG_INFO)
												System.out.println(
														s + " calls CONCRETE method: " + s.getInvokeExpr().getMethod());
											List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
											defaultTargets.add(s.getInvokeExpr().getMethod());
											targets = defaultTargets.iterator();
										} else {
											if (MyConstants.DEBUG_INFO)
												System.out.println(s + " calls NON-CONCRETE method: "
														+ s.getInvokeExpr().getMethod());
											targets = new Targets(this.callGraph.edgesOutOf(s));

											if (!targets.hasNext()) {
												if (MyConstants.DEBUG_INFO)
													System.out
															.println(s + " does NOT have a target. add a DEFAULT one");
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(s.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											}
										}

										if (targets == null) {
											continue;
										}

										while (targets.hasNext()) {
											SootMethod target = (SootMethod) targets.next();

											boolean noNewTaint = true;

											String propKey = sMethod.getSignature() + "|" + s.toString() + "|"
													+ Integer.toHexString(System.identityHashCode(s));

											if (!propagationHistory.containsKey(propKey)) {
												noNewTaint = false;
												List<Integer> sinks = new ArrayList<Integer>();
												sinks.addAll(taintVector);
												propagationHistory.put(propKey, sinks);
											} else {
												List<Integer> sinks = propagationHistory.get(propKey);

												for (Integer taint : taintVector) {
													if (!sinks.contains(taint)) {
														noNewTaint = false;
														sinks.add(taint);
													}
												}
											}

											if (noNewTaint) {
												break;
											}

											if (MyConstants.DEBUG_INFO) {
												System.out.println("PROPAGATING from METHOD: " + sMethod);
												System.out.println("PROPAGATING from STATEMENT: " + s);
											}
											taintedFieldsInCaller.addAll(taintedFields);
											Vector<Integer> tainted = propagate(target, taintVector, flowSink, s,
													sMethod);
											for (SootField sf : taintedFieldsInCallee) {
												if (!taintedFields.contains(sf)) {
													taintedFields.add(sf);
												}
											}
											taintedFieldsInCallee.clear();

											if (MyConstants.DEBUG_INFO) {
												System.out.println(s + " |taint:" + taintVector
														+ "| PROPAGATION result: " + tainted);
											}
											if ((tainted != null) && (!tainted.isEmpty())) {
												for (Integer i : tainted) {
													int index = i.intValue();

													if (index == MyConstants.thisObject) {
														if (s.getInvokeExpr() instanceof InstanceInvokeExpr) {
															Value taintedThisRef = ((InstanceInvokeExpr) s
																	.getInvokeExpr()).getBase();

															if (taintedThisRef instanceof Local) {
																List<Unit> defs0 = mrd
																		.getDefsOfAt((Local) taintedThisRef, s);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	sNode.addPred(defNode);
																	defNode.addSucc(sNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			s);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(s);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(s))) {
																		uses.get(defn).add(s);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}

														}

													} else if (index >= 0) {

														Value taintedArg = s.getInvokeExpr().getArg(index);

														if (taintedArg instanceof Local) {
															List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg, s);

															for (Unit defn : defs0) {

																APIGraphNode defNode = CreateOrGetExistingNode(
																		(Stmt) defn, sMethod);

																sNode.addPred(defNode);
																defNode.addSucc(sNode);
																if (!methodToDDGMap.get(sMethod).contains(defNode)) {
																	methodToDDGMap.get(sMethod).add(defNode);
																}

																UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
																if (!uses.containsKey(defn)) {
																	Vector<Stmt> scopes = new Vector<Stmt>();
																	scopes.add(s);
																	uses.put((Stmt) defn, scopes);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																} else if (!(uses.get(defn).contains(s))) {
																	uses.get(defn).add(s);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																}
															}
														}
													}
												}
											}
										}
										// invokes.add(s);

									}
								}

								// pure definiton statement:
								else {

									if (s instanceof DefinitionStmt) {
										Value rhs = ((DefinitionStmt) s).getRightOp();
										Value lhs = ((DefinitionStmt) s).getLeftOp();

										// if lhs is a reference
										if (MyConstants.CONSIDER_REDEFINE && lhs.getType() instanceof RefLikeType) {

											if (MyConstants.DEBUG_INFO)
												System.out.println("#2:looking for redefine:" + s);

											Iterator itForRedefine = body.getUnits().iterator();
											while (itForRedefine.hasNext()) {
												Stmt stmt = (Stmt) itForRedefine.next();

												if (!isInScope(eug, stmt, sScope)) {
													if (MyConstants.DEBUG_INFO)
														System.out
																.println(stmt + " is NOT in scope[<--" + sScope + "]");
													continue;
												}

												boolean isStmtUsingS = false;
												List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
												for (ValueBox useBox : useBoxesofStmt) {
													if (useBox.getValue() instanceof Local) {
														if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt)
																.contains(s)) {
															isStmtUsingS = true;
															break;
														}
													}
												}

												if (isStmtUsingS) {
													if (MyConstants.DEBUG_INFO)
														System.out.println(stmt + " IS using " + s);

													if (stmt.containsInvokeExpr()) {
														if (!stmt.getInvokeExpr().getMethod().getDeclaringClass()
																.isApplicationClass()) {
															AddTags(stmt, API_TAG);
														}
													}

													if (stmt instanceof DefinitionStmt) {
														// if(!stmt.containsInvokeExpr()){
														boolean usesConstant = false;
														List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
														for (ValueBox ccVB : checkConstUseBoxes) {
															if (ccVB.getValue() instanceof StringConstant) {
																if (!((StringConstant) ccVB.getValue()).value
																		.equals("")) {
																	usesConstant = true;
																	break;
																}
															}
														}
														if (usesConstant) {
															AddTags(stmt, STRING_CONST_TAG);
														}
														// }
													}

													APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);

													if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
														methodToDDGMap.get(sMethod).add(stmtNode);
													}

													APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope, sMethod);
													;

													if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
														methodToDDGMap.get(sMethod).add(sScopeNode);
													}

													sNode.removeSucc(sScopeNode);
													sScopeNode.removePred(sNode);

													sNode.addSucc(stmtNode);
													stmtNode.addPred(sNode);

													stmtNode.addSucc(sScopeNode);
													sScopeNode.addPred(stmtNode);

													if (stmt instanceof InvokeStmt) {

														Vector<Integer> taintVector = new Vector<Integer>();

														Iterator defIt2 = s.getDefBoxes().iterator();
														while (defIt2.hasNext()) {
															ValueBox vbox2 = (ValueBox) defIt2.next();
															if (vbox2.getValue() instanceof Local) {
																// System.out.println(vbox2.getValue());
																InvokeExpr invokeEx = stmt.getInvokeExpr();
																int argCount = invokeEx.getArgCount();
																for (int i = 0; i < argCount; i++) {
																	if (invokeEx.getArg(i) == vbox2.getValue()) {
																		taintVector.add(i);
																	}
																}

																// for instance invoke,
																// consider this
																// reference too.
																if (invokeEx instanceof InstanceInvokeExpr) {
																	if (((InstanceInvokeExpr) invokeEx)
																			.getBase() == vbox2.getValue()) {
																		taintVector.add(MyConstants.thisObject);
																	}
																}
															}
														}

														Iterator targets = null;
														if (stmt.getInvokeExpr().getMethod().isConcrete()) {
															if (MyConstants.DEBUG_INFO)
																System.out.println(stmt + " calls CONCRETE method: "
																		+ stmt.getInvokeExpr().getMethod());
															List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
															defaultTargets.add(stmt.getInvokeExpr().getMethod());
															targets = defaultTargets.iterator();
														} else {
															if (MyConstants.DEBUG_INFO)
																System.out.println(stmt + " calls NON-CONCRETE method: "
																		+ stmt.getInvokeExpr().getMethod());
															targets = new Targets(this.callGraph.edgesOutOf(stmt));

															if (!targets.hasNext()) {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(stmt
																			+ " does NOT have a target. add a DEFAULT one");
																List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																defaultTargets.add(stmt.getInvokeExpr().getMethod());
																targets = defaultTargets.iterator();
															}
														}

														if (targets == null) {
															continue;
														}

														while (targets.hasNext()) {
															SootMethod target = (SootMethod) targets.next();

															boolean noNewTaint = true;

															String propKey = sMethod.getSignature() + "|"
																	+ stmt.toString() + "|" + Integer
																			.toHexString(System.identityHashCode(stmt));

															if (!propagationHistory.containsKey(propKey)) {
																noNewTaint = false;
																List<Integer> sinks = new ArrayList<Integer>();
																sinks.addAll(taintVector);
																propagationHistory.put(propKey, sinks);
															} else {
																List<Integer> sinks = propagationHistory.get(propKey);

																for (Integer taint : taintVector) {
																	if (!sinks.contains(taint)) {
																		noNewTaint = false;
																		sinks.add(taint);
																	}
																}
															}

															if (noNewTaint) {
																break;
															}

															if (MyConstants.DEBUG_INFO) {
																System.out
																		.println("PROPAGATING from METHOD: " + sMethod);
																System.out
																		.println("PROPAGATING from STATEMENT: " + stmt);
															}
															taintedFieldsInCaller.addAll(taintedFields);
															Vector<Integer> tainted = propagate(target, taintVector,
																	flowSink, stmt, sMethod);
															for (SootField sf : taintedFieldsInCallee) {
																if (!taintedFields.contains(sf)) {
																	taintedFields.add(sf);
																}
															}
															taintedFieldsInCallee.clear();

															if (MyConstants.DEBUG_INFO) {
																System.out.println(stmt + " |taint:" + taintVector
																		+ "| PROPAGATION result: " + tainted);
															}
															if ((tainted != null) && (!tainted.isEmpty())) {
																for (Integer i : tainted) {
																	int index = i.intValue();

																	if (index == MyConstants.thisObject) {
																		if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																			Value taintedThisRef = ((InstanceInvokeExpr) stmt
																					.getInvokeExpr()).getBase();

																			if (taintedThisRef instanceof Local) {
																				List<Unit> defs0 = mrd.getDefsOfAt(
																						(Local) taintedThisRef, stmt);

																				for (Unit defn : defs0) {

																					APIGraphNode defNode = CreateOrGetExistingNode(
																							(Stmt) defn, sMethod);
																					stmtNode.addPred(defNode);
																					defNode.addSucc(stmtNode);
																					if (!methodToDDGMap.get(sMethod)
																							.contains(defNode)) {
																						methodToDDGMap.get(sMethod)
																								.add(defNode);
																					}

																					UseWithScope defnWS = new UseWithScope(
																							(Stmt) defn, stmt);
																					if (!uses.containsKey(defn)) {
																						Vector<Stmt> scopes = new Vector<Stmt>();
																						scopes.add(stmt);
																						uses.put((Stmt) defn, scopes);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					} else if (!(uses.get(defn)
																							.contains(stmt))) {
																						uses.get(defn).add(stmt);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					}
																				}
																			}

																		}

																	} else if (index >= 0) {

																		Value taintedArg = stmt.getInvokeExpr()
																				.getArg(index);

																		if (taintedArg instanceof Local) {
																			List<Unit> defs0 = mrd.getDefsOfAt(
																					(Local) taintedArg, stmt);

																			for (Unit defn : defs0) {

																				APIGraphNode defNode = CreateOrGetExistingNode(
																						(Stmt) defn, sMethod);

																				stmtNode.addPred(defNode);
																				defNode.addSucc(stmtNode);
																				if (!methodToDDGMap.get(sMethod)
																						.contains(defNode)) {
																					methodToDDGMap.get(sMethod)
																							.add(defNode);
																				}

																				UseWithScope defnWS = new UseWithScope(
																						(Stmt) defn, stmt);
																				if (!uses.containsKey(defn)) {
																					Vector<Stmt> scopes = new Vector<Stmt>();
																					scopes.add(stmt);
																					uses.put((Stmt) defn, scopes);
																					usesStack.push(defnWS);
																					if (MyConstants.DEBUG_INFO)
																						System.out.println(
																								"use stack doesn't contain "
																										+ defnWS.dump()
																										+ ". Push it.");
																				} else if (!(uses.get(defn)
																						.contains(stmt))) {
																					uses.get(defn).add(stmt);
																					usesStack.push(defnWS);
																					if (MyConstants.DEBUG_INFO)
																						System.out.println(
																								"use stack doesn't contain "
																										+ defnWS.dump()
																										+ ". Push it.");
																				}
																			}
																		}
																	}
																}
															}

														}

													} else if (stmt instanceof DefinitionStmt) {

														Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
														((DefinitionStmt) stmt).getRightOp();

														if (rhsInvoke instanceof InvokeExpr) {

															Vector<Integer> taintVector = new Vector<Integer>();

															Iterator defIt2 = s.getDefBoxes().iterator();
															while (defIt2.hasNext()) {
																ValueBox vbox2 = (ValueBox) defIt2.next();
																if (vbox2.getValue() instanceof Local) {
																	// System.out.println(vbox2.getValue());
																	InvokeExpr invokeEx = stmt.getInvokeExpr();
																	int argCount = invokeEx.getArgCount();
																	for (int i = 0; i < argCount; i++) {
																		if (invokeEx.getArg(i) == vbox2.getValue()) {
																			taintVector.add(i);
																		}
																	}

																	// for instance
																	// invoke, consider
																	// this reference
																	// too.
																	if (invokeEx instanceof InstanceInvokeExpr) {
																		if (((InstanceInvokeExpr) invokeEx)
																				.getBase() == vbox2.getValue()) {
																			taintVector.add(MyConstants.thisObject);
																		}
																	}
																}
															}

															Iterator targets = null;
															if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(stmt + " calls CONCRETE method: "
																			+ stmt.getInvokeExpr().getMethod());
																List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																defaultTargets.add(stmt.getInvokeExpr().getMethod());
																targets = defaultTargets.iterator();
															} else {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(
																			stmt + " calls NON-CONCRETE method: "
																					+ stmt.getInvokeExpr().getMethod());
																targets = new Targets(this.callGraph.edgesOutOf(stmt));

																if (!targets.hasNext()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " does NOT have a target. add a DEFAULT one");
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																}
															}

															if (targets == null) {
																continue;
															}

															while (targets.hasNext()) {
																SootMethod target = (SootMethod) targets.next();

																boolean noNewTaint = true;

																String propKey = sMethod.getSignature() + "|"
																		+ stmt.toString() + "|" + Integer.toHexString(
																				System.identityHashCode(stmt));

																if (!propagationHistory.containsKey(propKey)) {
																	noNewTaint = false;
																	List<Integer> sinks = new ArrayList<Integer>();
																	sinks.addAll(taintVector);
																	propagationHistory.put(propKey, sinks);
																} else {
																	List<Integer> sinks = propagationHistory
																			.get(propKey);

																	for (Integer taint : taintVector) {
																		if (!sinks.contains(taint)) {
																			noNewTaint = false;
																			sinks.add(taint);
																		}
																	}
																}

																if (noNewTaint) {
																	break;
																}

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(
																			"PROPAGATING from METHOD: " + sMethod);
																	System.out.println(
																			"PROPAGATING from STATEMENT: " + stmt);
																}
																taintedFieldsInCaller.addAll(taintedFields);
																Vector<Integer> tainted = propagate(target, taintVector,
																		flowSink, stmt, sMethod);
																for (SootField sf : taintedFieldsInCallee) {
																	if (!taintedFields.contains(sf)) {
																		taintedFields.add(sf);
																	}
																}
																taintedFieldsInCallee.clear();

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(stmt + " |taint:" + taintVector
																			+ "| PROPAGATION result: " + tainted);
																}
																if ((tainted != null) && (!tainted.isEmpty())) {
																	for (Integer i : tainted) {
																		int index = i.intValue();

																		if (index == MyConstants.thisObject) {
																			if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																				Value taintedThisRef = ((InstanceInvokeExpr) stmt
																						.getInvokeExpr()).getBase();

																				if (taintedThisRef instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedThisRef,
																							stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);

																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}

																			}

																		} else if (index >= 0) {

																			Value taintedArg = stmt.getInvokeExpr()
																					.getArg(index);

																			if (taintedArg instanceof Local) {
																				List<Unit> defs0 = mrd.getDefsOfAt(
																						(Local) taintedArg, stmt);

																				for (Unit defn : defs0) {

																					APIGraphNode defNode = CreateOrGetExistingNode(
																							(Stmt) defn, sMethod);

																					stmtNode.addPred(defNode);
																					defNode.addSucc(stmtNode);
																					if (!methodToDDGMap.get(sMethod)
																							.contains(defNode)) {
																						methodToDDGMap.get(sMethod)
																								.add(defNode);
																					}

																					UseWithScope defnWS = new UseWithScope(
																							(Stmt) defn, stmt);
																					if (!uses.containsKey(defn)) {
																						Vector<Stmt> scopes = new Vector<Stmt>();
																						scopes.add(stmt);
																						uses.put((Stmt) defn, scopes);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					} else if (!(uses.get(defn)
																							.contains(stmt))) {
																						uses.get(defn).add(stmt);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					}
																				}
																			}
																		}
																	}
																}

															}
														} else {
															Value left = ((DefinitionStmt) stmt).getLeftOp();
															Iterator it = left.getUseBoxes().iterator();
															while (it.hasNext()) {
																Value leftValue = ((ValueBox) it.next()).getValue();
																if (leftValue instanceof Local) {
																	mrd.getDefsOfAt((Local) leftValue, stmt);

																}
															}
														}
													}

												} // isStmtUsingS
											}
										} // if(lhs.getType() instanceof RefLikeType){

										// rhs is parameter ref
										if (rhs instanceof ParameterRef) {
											if (MyConstants.DEBUG_INFO) {
												System.out.println("returning to caller...");
											}
											if (MyConstants.DEBUG_INFO)
												System.out.println("return to caller from: " + sMethod + " | " + s);

											Chain<SootClass> classes = Scene.v().getClasses();
											Iterator<SootClass> classes_iter = classes.iterator();
											while (classes_iter.hasNext()) {
												SootClass soot_class = classes_iter.next();
												if (soot_class.isApplicationClass() == false) {
													continue;
												}

												List<SootMethod> methods = soot_class.getMethods();
												for (int methId = 0; methId < methods.size(); ++methId) {
													SootMethod method = methods.get(methId);
													if (!method.isConcrete()) {
														continue;
													}

													JimpleBody callerBody = (JimpleBody) method.retrieveActiveBody();
													Iterator callerIter = callerBody.getUnits().iterator();
													while (callerIter.hasNext()) {
														Stmt callerStmt = (Stmt) callerIter.next();
														if (callerStmt.containsInvokeExpr()) {
															SootMethod calleeMethod;
															try {
																calleeMethod = callerStmt.getInvokeExpr().getMethod();
															} catch (Throwable e) {
																continue;
															}
															if (calleeMethod.isConcrete()) {
																if (calleeMethod.equals(sMethod)) {

																	APIGraphNode callerStmtNode = CreateOrGetExistingNode(
																			callerStmt, method);

																	sNode.addPred(callerStmtNode);
																	callerStmtNode.addSucc(sNode);
																	if (!methodToDDGMap.containsKey(method)) {
																		List<APIGraphNode> ddg = new ArrayList<APIGraphNode>();
																		ddg.add(callerStmtNode);
																		methodToDDGMap.put(method, ddg);
																	} else {
																		if (!methodToDDGMap.get(method)
																				.contains(callerStmtNode)) {
																			methodToDDGMap.get(method)
																					.add(callerStmtNode);
																		}
																	}

																	if (!fullWorklist.contains(method)) {
																		worklist.add(method);
																		fullWorklist.add(method);
																	}

																	if (!sourceMethods.contains(sMethod)) {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"adding sourceMethod: " + sMethod);
																		sourceMethods.add(sMethod);
																	}
																}
															} else {
																Iterator targets = new Targets(
																		this.callGraph.edgesOutOf(callerStmt));

																while (targets.hasNext()) {
																	SootMethod target = (SootMethod) targets.next();
																	if (target.equals(sMethod)) {

																		APIGraphNode callerStmtNode = CreateOrGetExistingNode(
																				callerStmt, method);

																		sNode.addPred(callerStmtNode);
																		callerStmtNode.addSucc(sNode);
																		if (!methodToDDGMap.containsKey(method)) {
																			List<APIGraphNode> ddg = new ArrayList<APIGraphNode>();
																			ddg.add(callerStmtNode);
																			methodToDDGMap.put(method, ddg);
																		} else {
																			if (!methodToDDGMap.get(method)
																					.contains(callerStmtNode)) {
																				methodToDDGMap.get(method)
																						.add(callerStmtNode);
																			}
																		}

																		if (!fullWorklist.contains(method)) {
																			worklist.add(method);
																			fullWorklist.add(method);
																		}

																		if (!sourceMethods.contains(sMethod)) {
																			if (MyConstants.DEBUG_INFO)
																				System.out
																						.println("adding sourceMethod: "
																								+ sMethod);
																			sourceMethods.add(sMethod);
																		}
																	}
																}
															}
														}
													}
												}
											}
										} else if (rhs instanceof InstanceFieldRef) {
											if (MyConstants.TO_TAINT_INSTANCE_FIELD) {
												if (!taintedFields.contains(((InstanceFieldRef) rhs).getField())) {
													if (MyConstants.DEBUG_INFO)
														System.out.println("adding new field as source: "
																+ ((InstanceFieldRef) rhs).getField() + " from: " + s);
													taintedFields.add(((InstanceFieldRef) rhs).getField());
												}

												SootField fieldKey = ((InstanceFieldRef) rhs).getField();
												if (fieldToUsesMap.containsKey(fieldKey)) {
													List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
													if (!fieldUses.contains(s)) {
														fieldUses.add(s);
													}
												} else {
													List<Stmt> fieldUses = new ArrayList<Stmt>();
													fieldToUsesMap.put(fieldKey, fieldUses);
													fieldUses.add(s);
												}
											}
										} else if (rhs instanceof StaticFieldRef) {
											if (MyConstants.TO_TAINT_STATIC_FIELD) {
												if (!taintedFields.contains(((StaticFieldRef) rhs).getField())) {
													if (MyConstants.DEBUG_INFO)
														System.out.println("adding new field as source: "
																+ ((StaticFieldRef) rhs).getField() + " from: " + s);
													taintedFields.add(((StaticFieldRef) rhs).getField());
												}

												SootField fieldKey = ((StaticFieldRef) rhs).getField();
												if (fieldToUsesMap.containsKey(fieldKey)) {
													List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
													if (!fieldUses.contains(s)) {
														fieldUses.add(s);
													}
												} else {
													List<Stmt> fieldUses = new ArrayList<Stmt>();
													fieldToUsesMap.put(fieldKey, fieldUses);
													fieldUses.add(s);
												}
											}
										}

										Iterator<ValueBox> sUseIter = s.getUseBoxes().iterator();
										while (sUseIter.hasNext()) {
											Value v = sUseIter.next().getValue();
											if (v instanceof Local) {

												List<Unit> defs = mrd.getDefsOfAt((Local) v, s);

												for (Unit defn : defs) {

													APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
															sMethod);
													;
													sNode.addPred(defNode);
													defNode.addSucc(sNode);
													if (!methodToDDGMap.get(sMethod).contains(defNode)) {
														methodToDDGMap.get(sMethod).add(defNode);
													}

													UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
													if (!uses.containsKey(defn)) {
														Vector<Stmt> scopes = new Vector<Stmt>();
														scopes.add(s);
														uses.put((Stmt) defn, scopes);
														usesStack.push(defnWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println("use stack doesn't contain "
																	+ defnWS.dump() + ". Push it.");
													} else if (!(uses.get(defn).contains(s))) {
														uses.get(defn).add(s);
														usesStack.push(defnWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println("use stack doesn't contain "
																	+ defnWS.dump() + ". Push it.");
													}
												}
											}
										}

									} // if(s instanceof DefinitionStmt){

								}
							}
						} // while(!usesStack.isEmpty()){

						Iterator i = uses.keySet().iterator();
						while (i.hasNext()) {
							Stmt s = (Stmt) i.next();
							// System.out.print(s + "|");

							AddTags(s, taintTag);
							Iterator usesIt = s.getUseBoxes().iterator();
							while (usesIt.hasNext()) {
								ValueBox vbox = (ValueBox) usesIt.next();
								if (vbox.getValue() instanceof Local) {
									Local l = (Local) vbox.getValue();

									Iterator rDefsIt = mrd.getDefsOfAt(l, s).iterator();
									while (rDefsIt.hasNext()) {
										Stmt next = (Stmt) rDefsIt.next();
										if (!next.getTags().contains(taintTag)) {
											AddTags(next, extraDefTag);
										}
									}
								}
							}
							// System.out.println(s.getTags());
						}
						// System.out.println();

						if (MyConstants.DEBUG_INFO) {
							System.out.println();
							System.out.println("method:" + sMethod.getSignature());
							System.out.println("dataflow for " + sink + ":");
						}
						Iterator printIt = body.getUnits().iterator();
						while (printIt.hasNext()) {
							Stmt s = (Stmt) printIt.next();
							if (s.getTags().contains(taintTag) || s.getTags().contains(extraDefTag)) {
								if (MyConstants.DEBUG_INFO) {
									dumpTaggedStmt(s);
								}

								Vector<Integer> labels = new Vector<Integer>();

								for (Tag tag : s.getTags()) {
									if (taintTagReverseMap.containsKey(tag)) {
										Integer label = ((TaintTag) tag).getLabel();
										if (!labels.contains(label)) {
											labels.add(label);
										}
									} else if (extraDefTagReverseMap.containsKey(tag)) {
										Integer label = ((TaintTag) tag).getLabel();
										if (!labels.contains(label)) {
											labels.add(label);
										}
									}
								}

								List<ValueBox> vbs = s.getUseAndDefBoxes();
								Iterator iter = vbs.iterator();
								while (iter.hasNext()) {
									ValueBox vb = (ValueBox) iter.next();
									if (vb.getValue() instanceof InstanceFieldRef) {
										SootField instanceField = ((InstanceFieldRef) vb.getValue()).getField();

										if (instanceField.getDeclaringClass().isApplicationClass() == false) {
											continue;
										}

										// if(!instanceFields.contains(instanceField)){
										// instanceFields.add(instanceField);
										// }
										////
										if (!instanceFieldMap.containsKey(instanceField)) {

											Vector<Integer> taintSources = new Vector<Integer>();
											taintSources.addAll(labels);
											instanceFieldMap.put(instanceField, taintSources);

										} else {

											Vector<Integer> taintSources = instanceFieldMap.get(instanceField);
											for (Integer label : labels) {
												if (!taintSources.contains(label)) {
													taintSources.add(label);
												}
											}
										}
										////

										LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
										List<String> fieldList = new ArrayList<String>();
										if (fieldList.contains(instanceField.getSignature())) {
											fieldList.add(instanceField.getSignature());
										}
										taintSourceToField.put(flowSink, fieldList);

									} else if (vb.getValue() instanceof StaticFieldRef) {
										SootField staticField = ((StaticFieldRef) vb.getValue()).getField();

										if (staticField.getDeclaringClass().isApplicationClass() == false) {
											continue;
										}

										// if(!staticFields.contains(staticField)){
										// staticFields.add(staticField);
										// }
										///
										if (!staticFieldMap.containsKey(staticField)) {

											Vector<Integer> taintSources = new Vector<Integer>();
											taintSources.addAll(labels);
											staticFieldMap.put(staticField, taintSources);

										} else {

											Vector<Integer> taintSources = staticFieldMap.get(staticField);
											for (Integer label : labels) {
												if (!taintSources.contains(label)) {
													taintSources.add(label);
												}
											}
										}
										///

										LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
										List<String> fieldList = new ArrayList<String>();
										if (fieldList.contains(staticField.getSignature())) {
											fieldList.add(staticField.getSignature());
										}
										taintSourceToField.put(flowSink, fieldList);

									} else if (vb.getValue() instanceof Local) {

										String varName = ((Local) vb.getValue()).getName();
										LinkedHashMap<String, List<String>> taintSourceToVar = new LinkedHashMap<String, List<String>>();
										List<String> varList = new ArrayList<String>();
										if (varList.contains(varName)) {
											varList.add(varName);
										}
										taintSourceToVar.put(flowSink, varList);
									}
								}
							}
						}

						if (MyConstants.DEBUG_INFO) {
							System.out.println("end dataflow for " + sink + "\n");
						}

					} // while(!worklist.isEmpty())

					if (breakAnalysis) {
						if (MyConstants.DEBUG_INFO) {
							System.out.println("breakout#2:" + flowSink);
						}
						continue;
					}

					// doDataFlowAnalysis phase two:
					// iteratively performs data propagation for tainted static fields
					Set<SootField> processedField = new LinkedHashSet<SootField>();
					Queue<SootField> fWorklist = new LinkedList<SootField>();

					for (SootField sf : taintedFields) {
						if (sf.getType() instanceof RefLikeType) {
							fWorklist.add(sf);
						}
					}

					while (!fWorklist.isEmpty()) {
						if (apiNodeCount > MyConstants.MAX_APINODES_CONSIDERED) {
							// nodeCount = 0;
							apiNodeCount = 0;

							if (MyConstants.DEBUG_INFO) {
								System.out.println("breakout#3: API_NODE_COUNT = " + apiNodeCount);
							}

							break;
						}
						SootField taintedField = fWorklist.remove();

						if (!fieldToUsesMap.containsKey(taintedField) || processedField.contains(taintedField)) {
							System.out.println("ERROR: definitions of a field " + taintedField + " is not recorded!");
							continue;
						}
						processedField.add(taintedField);

						List<Stmt> fieldUsesForPTA = fieldToUsesMap.get(taintedField);

						entryPoints = new ArrayList<SootMethod>();

						Chain<SootClass> classes = Scene.v().getClasses();
						Iterator<SootClass> classes_iter = classes.snapshotIterator();
						while (classes_iter.hasNext()) {
							SootClass soot_class = classes_iter.next();

							if (soot_class.isApplicationClass() == false) {
								continue;
							}

							if (MyConstants.DEBUG_INFO) {
								System.out.println("looking for define of field in " + soot_class.getName() + "...");
							}

							List<SootMethod> methods = soot_class.getMethods();
							for (SootMethod method : methods) {
								if (!method.isConcrete()) {
									continue;
								}

								JimpleBody body = (JimpleBody) method.retrieveActiveBody();
								Iterator it = body.getUnits().iterator();

								while (it.hasNext()) {
									Stmt s = (Stmt) it.next();
									if (s instanceof DefinitionStmt) {
										Value lhs = ((DefinitionStmt) s).getLeftOp();
										if (lhs instanceof StaticFieldRef) {
											if (((StaticFieldRef) lhs).getField().equals(taintedField)) {
												entryPoints.add(method);
											}
										}

										else if (lhs instanceof InstanceFieldRef) {

											if (((InstanceFieldRef) lhs).getField().equals(taintedField)) {
												entryPoints.add(method);
											}

										}
									}
								}
							}
						}

						worklist = new LinkedList<SootMethod>();
						fullWorklist = new LinkedList<SootMethod>();

						worklist.addAll(entryPoints);
						fullWorklist.addAll(entryPoints);

						sourceMethods = new ArrayList<SootMethod>();

						while (!worklist.isEmpty()) {
							SootMethod sMethod = worklist.remove();
							if (!methodToDDGMap.containsKey(sMethod)) {
								List<APIGraphNode> apiGraph = new ArrayList<APIGraphNode>();
								methodToDDGMap.put(sMethod, apiGraph);
							}

							if (MyConstants.DEBUG_INFO) {
								System.out.println();
								System.out.println("analyzing method#2:" + sMethod.getSignature());
							}

							JimpleBody body = (JimpleBody) sMethod.retrieveActiveBody();
							
							ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);
							MyReachingDefinition mrd = new MyReachingDefinition(eug);

							Stack<UseWithScope> usesStack = new Stack<UseWithScope>();
							new Vector<UseWithScope>();

							LinkedHashMap<Stmt, Vector<Stmt>> uses = new LinkedHashMap<Stmt, Vector<Stmt>>();

							Stmt sink = null;
							{
								Iterator it = body.getUnits().iterator();
								while (it.hasNext()) {
									Stmt s = (Stmt) it.next();

									if (s instanceof DefinitionStmt) {
										Value lhs = ((DefinitionStmt) s).getLeftOp();
										if (lhs instanceof StaticFieldRef) {
											if (((StaticFieldRef) lhs).getField().equals(taintedField)) {

												UseWithScope sWS = new UseWithScope(s, s);
												if (!uses.containsKey(s)) {
													uses.put(s, new Vector<Stmt>());
													usesStack.push(sWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println("use stack doesn't contain " + sWS.dump()
																+ ". Push it.");
													sink = s;

													APIGraphNode sNode = CreateOrGetExistingNode(s, sMethod);
													;
													if (!methodToDDGMap.get(sMethod).contains(sNode)) {
														methodToDDGMap.get(sMethod).add(sNode);
													}

													if (fieldToDefsMap.containsKey(taintedField)) {
														List<Stmt> fieldDefs = fieldToDefsMap.get(taintedField);
														if (!fieldDefs.contains(s)) {
															fieldDefs.add(s);
														}
													} else {
														List<Stmt> fieldDefs = new ArrayList<Stmt>();
														fieldDefs.add(s);
														fieldToDefsMap.put(taintedField, fieldDefs);
													}
												}
											}
										}

										else if (lhs instanceof InstanceFieldRef) {
											if (((InstanceFieldRef) lhs).getField().equals(taintedField)) {

												boolean ptsHasIntersection = false;
												PointsToSet ptsLhs = this.pta
														.reachingObjects((Local) ((InstanceFieldRef) lhs).getBase());
												if (!ptsLhs.isEmpty()) {

													for (Stmt fieldUseForPTA : fieldUsesForPTA) {
														PointsToSet ptsToTest = this.pta.reachingObjects(
																(Local) ((InstanceFieldRef) ((DefinitionStmt) fieldUseForPTA)
																		.getRightOp()).getBase());
														if (ptsLhs.hasNonEmptyIntersection(ptsToTest)) {
															ptsHasIntersection = true;
															break;
														}
													}
												}

												if (ptsHasIntersection || ptsLhs.isEmpty()) {

													UseWithScope sWS = new UseWithScope(s, s);
													if (!uses.containsKey(s)) {
														uses.put(s, new Vector<Stmt>());
														usesStack.push(sWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println("use stack doesn't contain " + sWS.dump()
																	+ ". Push it.");
														sink = s;

														APIGraphNode sNode = CreateOrGetExistingNode(s, sMethod);

														if (!methodToDDGMap.get(sMethod).contains(sNode)) {
															methodToDDGMap.get(sMethod).add(sNode);
														}

														if (fieldToDefsMap.containsKey(taintedField)) {
															List<Stmt> fieldDefs = fieldToDefsMap.get(taintedField);
															if (!fieldDefs.contains(s)) {
																fieldDefs.add(s);
															}
														} else {
															List<Stmt> fieldDefs = new ArrayList<Stmt>();
															fieldDefs.add(s);
															fieldToDefsMap.put(taintedField, fieldDefs);
														}
													}

												}
											}
										}
									}

									Iterator useIt = s.getUseBoxes().iterator();
									while (useIt.hasNext()) {
										ValueBox vBox = (ValueBox) useIt.next();
										if (vBox.getValue() instanceof InvokeExpr) {
											if (sourceMethods.contains(((InvokeExpr) vBox.getValue()).getMethod())) {

												UseWithScope sWS = new UseWithScope(s, s);
												if (!uses.containsKey(s)) {
													Vector<Stmt> scopes = new Vector<Stmt>();
													scopes.add(s);
													uses.put(s, scopes);
													usesStack.push(sWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println("use stack doesn't contain " + sWS.dump()
																+ ". Push it.");
													sink = s;
												}
											}
										}
									}
								}
							}

							while (!usesStack.isEmpty()) {

								UseWithScope useWS = usesStack.pop();
								if (MyConstants.DEBUG_INFO)
									System.out.println("POP from use stack: " + useWS.dump());

								// use-def analysis
								Stmt s = useWS.getUse();
								Stmt sScope = useWS.getScopeEnd();

								if (s.containsInvokeExpr()) {
									if (!s.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()) {
										AddTags(s, API_TAG);
									}
								}

								if (s instanceof DefinitionStmt) {
									// if(!s.containsInvokeExpr()){
									boolean usesConstant = false;
									List<ValueBox> checkConstUseBoxes = s.getUseBoxes();
									for (ValueBox ccVB : checkConstUseBoxes) {
										if (ccVB.getValue() instanceof StringConstant) {
											if (!((StringConstant) ccVB.getValue()).value.equals("")) {
												usesConstant = true;
												break;
											}
										}
									}
									if (usesConstant) {
										AddTags(s, STRING_CONST_TAG);
									}
									// }
								}

								APIGraphNode sNode = CreateOrGetExistingNode(s, sMethod);
								;
								if (!methodToDDGMap.get(sMethod).contains(sNode)) {
									methodToDDGMap.get(sMethod).add(sNode);
								}

								if (s instanceof InvokeStmt) {

									if (s.getInvokeExpr().getMethod().getSignature().equals(flowSink)
											|| sourceMethods.contains(s.getInvokeExpr().getMethod())) {
										List<ValueBox> usesBoxes = s.getUseBoxes();
										Iterator usesIter = usesBoxes.iterator();
										while (usesIter.hasNext()) {
											ValueBox usesBox = (ValueBox) usesIter.next();
											if (usesBox.getValue() instanceof Local) {
												List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
												for (Unit def : defs) {

													APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
													;
													sNode.addPred(defNode);
													defNode.addSucc(sNode);
													if (!methodToDDGMap.get(sMethod).contains(defNode)) {
														methodToDDGMap.get(sMethod).add(defNode);
													}

													UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
													if (!uses.containsKey(def)) {
														Vector<Stmt> scopes = new Vector<Stmt>();
														scopes.add(s);
														uses.put((Stmt) def, scopes);
														usesStack.push(defofuseWS);
													} else {
														Vector<Stmt> scopes = uses.get(def);
														if (!scopes.contains(s)) {
															scopes.add(s);
															usesStack.push(defofuseWS);
														}
													}
												}
											}
										}
									}

								}
								// added for NLP project
								else if ((s instanceof IfStmt || s instanceof TableSwitchStmt
										|| s instanceof LookupSwitchStmt) && s.toString().contains(flowSink)
										&& sMethod.getSignature().equals(stmtMethod)) {
									List<ValueBox> usesBoxes = s.getUseBoxes();
									Iterator usesIter = usesBoxes.iterator();
									while (usesIter.hasNext()) {
										ValueBox usesBox = (ValueBox) usesIter.next();
										if (usesBox.getValue() instanceof Local) {
											List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
											for (Unit def : defs) {

												APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
												;

												sNode.addPred(defNode);
												defNode.addSucc(sNode);
												if (!methodToDDGMap.get(sMethod).contains(defNode)) {
													methodToDDGMap.get(sMethod).add(defNode);
												}

												UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
												if (!uses.containsKey(def)) {
													Vector<Stmt> scopes = new Vector<Stmt>();
													scopes.add(s);
													uses.put((Stmt) def, scopes);
													usesStack.push(defofuseWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																"use stack: " + defofuseWS.dump() + ". Push it.");
												} else {
													Vector<Stmt> scopes = uses.get(def);
													if (!scopes.contains(s)) {
														scopes.add(s);
														usesStack.push(defofuseWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println(
																	"use stack: " + defofuseWS.dump() + ". Push it.");
													}
												}
											}
										}
									}
								} else {

									boolean isInvoke = false;

									Iterator iUse = s.getUseBoxes().iterator();
									while (iUse.hasNext()) {
										ValueBox vB = (ValueBox) iUse.next();
										if (vB.getValue() instanceof InvokeExpr) {
											isInvoke = true;
										}
									}

									// rhs is invoke, lhs is ret
									if (isInvoke) {

										if (s.getInvokeExpr().getMethod().getSignature().equals(flowSink)
												|| sourceMethods.contains(s.getInvokeExpr().getMethod())) {

											List<ValueBox> usesBoxes = s.getUseBoxes();
											Iterator usesIter = usesBoxes.iterator();
											while (usesIter.hasNext()) {
												ValueBox usesBox = (ValueBox) usesIter.next();
												if (usesBox.getValue() instanceof Local) {
													List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
													for (Unit def : defs) {

														APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def,
																sMethod);

														sNode.addPred(defNode);
														defNode.addSucc(sNode);
														if (!methodToDDGMap.get(sMethod).contains(defNode)) {
															methodToDDGMap.get(sMethod).add(defNode);
														}

														UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
														if (!uses.containsKey(def)) {
															Vector<Stmt> scopes = new Vector<Stmt>();
															scopes.add(s);
															uses.put((Stmt) def, scopes);
															usesStack.push(defofuseWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack: " + defofuseWS.dump()
																		+ ". Push it.");
														} else {
															Vector<Stmt> scopes = uses.get(def);
															if (!scopes.contains(s)) {
																scopes.add(s);
																usesStack.push(defofuseWS);
																if (MyConstants.DEBUG_INFO)
																	System.out.println("use stack: " + defofuseWS.dump()
																			+ ". Push it.");
															}
														}
													}
												}
											}
										}

										else {

											if (s instanceof DefinitionStmt) {
												Value lhs = ((DefinitionStmt) s).getLeftOp();

												if (MyConstants.CONSIDER_REDEFINE
														&& lhs.getType() instanceof RefLikeType) {

													if (MyConstants.DEBUG_INFO)
														System.out.println("#3:looking for redefine:" + s);

													Iterator itForRedefine = body.getUnits().iterator();
													while (itForRedefine.hasNext()) {
														Stmt stmt = (Stmt) itForRedefine.next();

														if (!isInScope(eug, stmt, sScope)) {
															if (MyConstants.DEBUG_INFO)
																System.out.println(
																		stmt + " is NOT in scope[<--" + sScope + "]");
															continue;
														}

														boolean isStmtUsingS = false;
														List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
														for (ValueBox useBox : useBoxesofStmt) {
															if (useBox.getValue() instanceof Local) {
																if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt)
																		.contains(s)) {
																	isStmtUsingS = true;
																	break;
																}
															}
														}

														if (isStmtUsingS) {
															if (MyConstants.DEBUG_INFO)
																System.out.println(stmt + " IS using " + s);

															if (stmt.containsInvokeExpr()) {
																if (!stmt.getInvokeExpr().getMethod()
																		.getDeclaringClass().isApplicationClass()) {
																	AddTags(stmt, API_TAG);
																}
															}

															if (stmt instanceof DefinitionStmt) {
																boolean usesConstant = false;
																List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
																for (ValueBox ccVB : checkConstUseBoxes) {
																	if (ccVB.getValue() instanceof StringConstant) {
																		if (!((StringConstant) ccVB.getValue()).value
																				.equals("")) {
																			usesConstant = true;
																			break;
																		}
																	}
																}
																if (usesConstant) {
																	AddTags(stmt, STRING_CONST_TAG);
																}
															}

															APIGraphNode stmtNode = CreateOrGetExistingNode(stmt,
																	sMethod);

															if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
																methodToDDGMap.get(sMethod).add(stmtNode);
															}

															APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope,
																	sMethod);

															if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
																methodToDDGMap.get(sMethod).add(sScopeNode);
															}

															sNode.removeSucc(sScopeNode);
															sScopeNode.removePred(sNode);

															sNode.addSucc(stmtNode);
															stmtNode.addPred(sNode);

															stmtNode.addSucc(sScopeNode);
															sScopeNode.addPred(stmtNode);

															if (stmt instanceof InvokeStmt) {

																Vector<Integer> taintVector = new Vector<Integer>();

																Iterator defIt2 = s.getDefBoxes().iterator();
																while (defIt2.hasNext()) {
																	ValueBox vbox2 = (ValueBox) defIt2.next();
																	if (vbox2.getValue() instanceof Local) {
																		// System.out.println(vbox2.getValue());
																		InvokeExpr invokeEx = stmt.getInvokeExpr();
																		int argCount = invokeEx.getArgCount();
																		for (int i = 0; i < argCount; i++) {
																			if (invokeEx.getArg(i) == vbox2
																					.getValue()) {
																				taintVector.add(i);
																			}
																		}

																		if (invokeEx instanceof InstanceInvokeExpr) {
																			if (((InstanceInvokeExpr) invokeEx)
																					.getBase() == vbox2.getValue()) {
																				taintVector.add(MyConstants.thisObject);
																			}
																		}
																	}
																}

																Iterator targets = null;
																if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																} else {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls NON-CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	targets = new Targets(
																			this.callGraph.edgesOutOf(stmt));

																	if (!targets.hasNext()) {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(stmt
																					+ " does NOT have a target. add a DEFAULT one");
																		List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																		defaultTargets
																				.add(stmt.getInvokeExpr().getMethod());
																		targets = defaultTargets.iterator();
																	}
																}

																if (targets == null) {
																	continue;
																}

																while (targets.hasNext()) {
																	SootMethod target = (SootMethod) targets.next();

																	boolean noNewTaint = true;

																	String propKey = sMethod.getSignature() + "|"
																			+ stmt.toString() + "|"
																			+ Integer.toHexString(
																					System.identityHashCode(stmt));

																	if (!propagationHistory.containsKey(propKey)) {
																		noNewTaint = false;
																		List<Integer> sinks = new ArrayList<Integer>();
																		sinks.addAll(taintVector);
																		propagationHistory.put(propKey, sinks);
																	} else {
																		List<Integer> sinks = propagationHistory
																				.get(propKey);
																		for (Integer taint : taintVector) {
																			if (!sinks.contains(taint)) {
																				noNewTaint = false;
																				sinks.add(taint);
																			}
																		}
																	}

																	if (noNewTaint) {
																		break;
																	}

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(
																				"PROPAGATING from METHOD: " + sMethod);
																		System.out.println(
																				"PROPAGATING from STATEMENT: " + stmt);
																	}
																	taintedFieldsInCaller.addAll(taintedFields);
																	Vector<Integer> tainted = propagate(target,
																			taintVector, flowSink, stmt, sMethod);
																	for (SootField sf : taintedFieldsInCallee) {
																		if (!taintedFields.contains(sf)) {
																			taintedFields.add(sf);
																		}
																	}
																	taintedFieldsInCallee.clear();

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(stmt + " |taint:"
																				+ taintVector + "| PROPAGATION result: "
																				+ tainted);
																	}
																	if ((tainted != null) && (!tainted.isEmpty())) {

																		for (Integer i : tainted) {
																			int index = i.intValue();
																			if (index == MyConstants.thisObject) {
																				if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																					Value taintedThisRef = ((InstanceInvokeExpr) stmt
																							.getInvokeExpr()).getBase();

																					if (taintedThisRef instanceof Local) {
																						List<Unit> defs0 = mrd
																								.getDefsOfAt(
																										(Local) taintedThisRef,
																										stmt);

																						for (Unit defn : defs0) {

																							APIGraphNode defNode = CreateOrGetExistingNode(
																									(Stmt) defn,
																									sMethod);

																							stmtNode.addPred(defNode);
																							defNode.addSucc(stmtNode);
																							if (!methodToDDGMap
																									.get(sMethod)
																									.contains(
																											defNode)) {
																								methodToDDGMap
																										.get(sMethod)
																										.add(defNode);
																							}

																							UseWithScope defnWS = new UseWithScope(
																									(Stmt) defn, stmt);
																							if (!uses.containsKey(
																									defn)) {
																								Vector<Stmt> scopes = new Vector<Stmt>();
																								scopes.add(stmt);
																								uses.put((Stmt) defn,
																										scopes);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							} else if (!(uses.get(defn)
																									.contains(stmt))) {
																								uses.get(defn)
																										.add(stmt);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							}
																						}
																					}

																				}

																			} else if (index >= 0) {

																				Value taintedArg = stmt.getInvokeExpr()
																						.getArg(index);

																				if (taintedArg instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedArg, stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);

																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}
																			}
																		}
																	}
																}
															} else if (stmt instanceof DefinitionStmt) {

																Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
																if (rhsInvoke instanceof InvokeExpr) {

																	Vector<Integer> taintVector = new Vector<Integer>();

																	Iterator defIt2 = s.getDefBoxes().iterator();
																	while (defIt2.hasNext()) {
																		ValueBox vbox2 = (ValueBox) defIt2.next();
																		if (vbox2.getValue() instanceof Local) {
																			InvokeExpr invokeEx = stmt.getInvokeExpr();
																			int argCount = invokeEx.getArgCount();
																			for (int i = 0; i < argCount; i++) {
																				if (invokeEx.getArg(i) == vbox2
																						.getValue()) {
																					taintVector.add(i);
																				}
																			}

																			if (invokeEx instanceof InstanceInvokeExpr) {
																				if (((InstanceInvokeExpr) invokeEx)
																						.getBase() == vbox2
																								.getValue()) {
																					taintVector.add(
																							MyConstants.thisObject);
																				}
																			}
																		}
																	}

																	Iterator targets = null;
																	if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(stmt
																					+ " calls CONCRETE method: "
																					+ stmt.getInvokeExpr().getMethod());
																		List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																		defaultTargets
																				.add(stmt.getInvokeExpr().getMethod());
																		targets = defaultTargets.iterator();
																	} else {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(stmt
																					+ " calls NON-CONCRETE method: "
																					+ stmt.getInvokeExpr().getMethod());
																		targets = new Targets(
																				this.callGraph.edgesOutOf(stmt));

																		if (!targets.hasNext()) {
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(stmt
																						+ " does NOT have a target. add a DEFAULT one");
																			List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																			defaultTargets.add(
																					stmt.getInvokeExpr().getMethod());
																			targets = defaultTargets.iterator();
																		}
																	}

																	if (targets == null) {
																		continue;
																	}

																	while (targets.hasNext()) {
																		SootMethod target = (SootMethod) targets.next();

																		boolean noNewTaint = true;

																		String propKey = sMethod.getSignature() + "|"
																				+ stmt.toString() + "|"
																				+ Integer.toHexString(
																						System.identityHashCode(stmt));

																		if (!propagationHistory.containsKey(propKey)) {
																			noNewTaint = false;
																			List<Integer> sinks = new ArrayList<Integer>();
																			sinks.addAll(taintVector);
																			propagationHistory.put(propKey, sinks);
																		} else {
																			List<Integer> sinks = propagationHistory
																					.get(propKey);

																			for (Integer taint : taintVector) {
																				if (!sinks.contains(taint)) {
																					noNewTaint = false;
																					sinks.add(taint);
																				}
																			}
																		}

																		if (noNewTaint) {
																			break;
																		}

																		if (MyConstants.DEBUG_INFO) {
																			System.out
																					.println("PROPAGATING from METHOD: "
																							+ sMethod);
																			System.out.println(
																					"PROPAGATING from STATEMENT: "
																							+ stmt);
																		}
																		taintedFieldsInCaller.addAll(taintedFields);
																		Vector<Integer> tainted = propagate(target,
																				taintVector, flowSink, stmt, sMethod);
																		for (SootField sf : taintedFieldsInCallee) {
																			if (!taintedFields.contains(sf)) {
																				taintedFields.add(sf);
																			}
																		}
																		taintedFieldsInCallee.clear();

																		if (MyConstants.DEBUG_INFO) {
																			System.out.println(
																					stmt + " |taint:" + taintVector
																							+ "| PROPAGATION result: "
																							+ tainted);
																		}
																		if ((tainted != null) && (!tainted.isEmpty())) {

																			for (Integer i : tainted) {
																				int index = i.intValue();

																				if (index == MyConstants.thisObject) {
																					if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																						Value taintedThisRef = ((InstanceInvokeExpr) stmt
																								.getInvokeExpr())
																										.getBase();

																						if (taintedThisRef instanceof Local) {
																							List<Unit> defs0 = mrd
																									.getDefsOfAt(
																											(Local) taintedThisRef,
																											stmt);

																							for (Unit defn : defs0) {
																								APIGraphNode defNode = CreateOrGetExistingNode(
																										(Stmt) defn,
																										sMethod);

																								stmtNode.addPred(
																										defNode);
																								defNode.addSucc(
																										stmtNode);
																								if (!methodToDDGMap
																										.get(sMethod)
																										.contains(
																												defNode)) {
																									methodToDDGMap.get(
																											sMethod)
																											.add(defNode);
																								}

																								UseWithScope defnWS = new UseWithScope(
																										(Stmt) defn,
																										stmt);
																								if (!uses.containsKey(
																										defn)) {
																									Vector<Stmt> scopes = new Vector<Stmt>();
																									scopes.add(stmt);
																									uses.put(
																											(Stmt) defn,
																											scopes);
																									usesStack.push(
																											defnWS);
																									if (MyConstants.DEBUG_INFO)
																										System.out
																												.println(
																														"use stack doesn't contain "
																																+ defnWS.dump()
																																+ ". Push it.");
																								} else if (!(uses
																										.get(defn)
																										.contains(
																												stmt))) {
																									uses.get(defn)
																											.add(stmt);
																									usesStack.push(
																											defnWS);
																									if (MyConstants.DEBUG_INFO)
																										System.out
																												.println(
																														"use stack doesn't contain "
																																+ defnWS.dump()
																																+ ". Push it.");
																								}
																							}
																						}
																					}
																				} else if (index >= 0) {
																					Value taintedArg = stmt
																							.getInvokeExpr()
																							.getArg(index);
																					if (taintedArg instanceof Local) {
																						List<Unit> defs0 = mrd
																								.getDefsOfAt(
																										(Local) taintedArg,
																										stmt);

																						for (Unit defn : defs0) {

																							APIGraphNode defNode = CreateOrGetExistingNode(
																									(Stmt) defn,
																									sMethod);
																							stmtNode.addPred(defNode);
																							defNode.addSucc(stmtNode);
																							if (!methodToDDGMap
																									.get(sMethod)
																									.contains(
																											defNode)) {
																								methodToDDGMap
																										.get(sMethod)
																										.add(defNode);
																							}

																							UseWithScope defnWS = new UseWithScope(
																									(Stmt) defn, stmt);
																							if (!uses.containsKey(
																									defn)) {
																								Vector<Stmt> scopes = new Vector<Stmt>();
																								scopes.add(stmt);
																								uses.put((Stmt) defn,
																										scopes);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							} else if (!(uses.get(defn)
																									.contains(stmt))) {
																								uses.get(defn)
																										.add(stmt);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							}
																						}
																					}
																				}
																			}
																		}

																	}
																}
															}

														} // isStmtUsingS
													}
												} // if(lhs.getType() instanceof
													// RefLikeType){
											}

											Vector<Integer> taintVector = new Vector<Integer>();
											taintVector.add(MyConstants.returnValue);

											Iterator targets = null;
											if (s.getInvokeExpr().getMethod().isConcrete()) {
												if (MyConstants.DEBUG_INFO)
													System.out.println(s + " calls CONCRETE method: "
															+ s.getInvokeExpr().getMethod());
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(s.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											} else {
												if (MyConstants.DEBUG_INFO)
													System.out.println(s + " calls NON-CONCRETE method: "
															+ s.getInvokeExpr().getMethod());
												targets = new Targets(this.callGraph.edgesOutOf(s));

												if (!targets.hasNext()) {
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																s + " does NOT have a target. add a DEFAULT one");
													List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
													defaultTargets.add(s.getInvokeExpr().getMethod());
													targets = defaultTargets.iterator();
												}
											}

											if (targets == null) {
												continue;
											}

											while (targets.hasNext()) {
												SootMethod target = (SootMethod) targets.next();

												boolean noNewTaint = true;

												String propKey = sMethod.getSignature() + "|" + s.toString() + "|"
														+ Integer.toHexString(System.identityHashCode(s));

												if (!propagationHistory.containsKey(propKey)) {
													noNewTaint = false;
													List<Integer> sinks = new ArrayList<Integer>();
													sinks.addAll(taintVector);
													propagationHistory.put(propKey, sinks);
												} else {
													List<Integer> sinks = propagationHistory.get(propKey);

													for (Integer taint : taintVector) {
														if (!sinks.contains(taint)) {
															noNewTaint = false;
															sinks.add(taint);
														}
													}
												}

												if (noNewTaint) {
													break;
												}

												if (MyConstants.DEBUG_INFO) {
													System.out.println("PROPAGATING from METHOD: " + sMethod);
													System.out.println("PROPAGATING from STATEMENT: " + s);
												}
												taintedFieldsInCaller.addAll(taintedFields);
												Vector<Integer> tainted = propagate(target, taintVector, flowSink, s,
														sMethod);
												for (SootField sf : taintedFieldsInCallee) {
													if (!taintedFields.contains(sf)) {
														taintedFields.add(sf);
													}
												}
												taintedFieldsInCallee.clear();

												if (MyConstants.DEBUG_INFO) {
													System.out.println(s + " |taint:" + taintVector
															+ "| PROPAGATION result: " + tainted);
												}
												if ((tainted != null) && (!tainted.isEmpty())) {

													for (Integer i : tainted) {
														int index = i.intValue();

														if (index == MyConstants.thisObject) {
															if (s.getInvokeExpr() instanceof InstanceInvokeExpr) {
																Value taintedThisRef = ((InstanceInvokeExpr) s
																		.getInvokeExpr()).getBase();

																if (taintedThisRef instanceof Local) {
																	List<Unit> defs0 = mrd
																			.getDefsOfAt((Local) taintedThisRef, s);

																	for (Unit defn : defs0) {

																		APIGraphNode defNode = CreateOrGetExistingNode(
																				(Stmt) defn, sMethod);

																		sNode.addPred(defNode);
																		defNode.addSucc(sNode);
																		if (!methodToDDGMap.get(sMethod)
																				.contains(defNode)) {
																			methodToDDGMap.get(sMethod).add(defNode);
																		}

																		UseWithScope defnWS = new UseWithScope(
																				(Stmt) defn, s);
																		if (!uses.containsKey(defn)) {
																			Vector<Stmt> scopes = new Vector<Stmt>();
																			scopes.add(s);
																			uses.put((Stmt) defn, scopes);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		} else if (!(uses.get(defn).contains(s))) {
																			uses.get(defn).add(s);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		}
																	}
																}

															}

														} else if (index >= 0) {

															Value taintedArg = s.getInvokeExpr().getArg(index);

															if (taintedArg instanceof Local) {
																List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg,
																		s);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	sNode.addPred(defNode);
																	defNode.addSucc(sNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			s);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(s);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(s))) {
																		uses.get(defn).add(s);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}
														}
													}
												}

											}
											// invokes.add(s);
										}
									}

									// pure definiton statement:
									else {

										if (s instanceof DefinitionStmt) {
											Value rhs = ((DefinitionStmt) s).getRightOp();
											Value lhs = ((DefinitionStmt) s).getLeftOp();

											// if lhs is a reference
											if (MyConstants.CONSIDER_REDEFINE && lhs.getType() instanceof RefLikeType) {

												if (MyConstants.DEBUG_INFO)
													System.out.println("#4:looking for redefine:" + s);

												Iterator itForRedefine = body.getUnits().iterator();
												while (itForRedefine.hasNext()) {
													Stmt stmt = (Stmt) itForRedefine.next();

													if (!isInScope(eug, stmt, sScope)) {
														if (MyConstants.DEBUG_INFO)
															System.out.println(
																	stmt + " is NOT in scope[<--" + sScope + "]");
														continue;
													}

													boolean isStmtUsingS = false;
													List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
													for (ValueBox useBox : useBoxesofStmt) {
														if (useBox.getValue() instanceof Local) {
															if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt)
																	.contains(s)) {
																isStmtUsingS = true;
																break;
															}
														}
													}

													if (isStmtUsingS) {
														if (MyConstants.DEBUG_INFO)
															System.out.println(stmt + " IS using " + s);

														if (stmt.containsInvokeExpr()) {
															if (!stmt.getInvokeExpr().getMethod().getDeclaringClass()
																	.isApplicationClass()) {
																AddTags(stmt, API_TAG);
															}
														}

														if (stmt instanceof DefinitionStmt) {
															// if(!stmt.containsInvokeExpr()){
															boolean usesConstant = false;
															List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
															for (ValueBox ccVB : checkConstUseBoxes) {
																if (ccVB.getValue() instanceof StringConstant) {
																	if (!((StringConstant) ccVB.getValue()).value
																			.equals("")) {
																		usesConstant = true;
																		break;
																	}
																}
															}
															if (usesConstant) {
																AddTags(stmt, STRING_CONST_TAG);
															}
															// }
														}

														APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);
														if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
															methodToDDGMap.get(sMethod).add(stmtNode);
														}

														APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope,
																sMethod);
														if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
															methodToDDGMap.get(sMethod).add(sScopeNode);
														}

														sNode.removeSucc(sScopeNode);
														sScopeNode.removePred(sNode);

														sNode.addSucc(stmtNode);
														stmtNode.addPred(sNode);

														stmtNode.addSucc(sScopeNode);
														sScopeNode.addPred(stmtNode);

														if (stmt instanceof InvokeStmt) {

															Vector<Integer> taintVector = new Vector<Integer>();

															Iterator defIt2 = s.getDefBoxes().iterator();
															while (defIt2.hasNext()) {
																ValueBox vbox2 = (ValueBox) defIt2.next();
																if (vbox2.getValue() instanceof Local) {
																	// System.out.println(vbox2.getValue());
																	InvokeExpr invokeEx = stmt.getInvokeExpr();
																	int argCount = invokeEx.getArgCount();
																	for (int i = 0; i < argCount; i++) {
																		if (invokeEx.getArg(i) == vbox2.getValue()) {
																			taintVector.add(i);
																		}
																	}

																	if (invokeEx instanceof InstanceInvokeExpr) {
																		if (((InstanceInvokeExpr) invokeEx)
																				.getBase() == vbox2.getValue()) {
																			taintVector.add(MyConstants.thisObject);
																		}
																	}
																}
															}

															Iterator targets = null;
															if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(stmt + " calls CONCRETE method: "
																			+ stmt.getInvokeExpr().getMethod());
																List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																defaultTargets.add(stmt.getInvokeExpr().getMethod());
																targets = defaultTargets.iterator();
															} else {
																if (MyConstants.DEBUG_INFO)
																	System.out.println(
																			stmt + " calls NON-CONCRETE method: "
																					+ stmt.getInvokeExpr().getMethod());
																targets = new Targets(this.callGraph.edgesOutOf(stmt));

																if (!targets.hasNext()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " does NOT have a target. add a DEFAULT one");
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																}
															}

															if (targets == null) {
																continue;
															}

															while (targets.hasNext()) {
																SootMethod target = (SootMethod) targets.next();

																boolean noNewTaint = true;

																String propKey = sMethod.getSignature() + "|"
																		+ stmt.toString() + "|" + Integer.toHexString(
																				System.identityHashCode(stmt));

																if (!propagationHistory.containsKey(propKey)) {
																	noNewTaint = false;
																	List<Integer> sinks = new ArrayList<Integer>();
																	sinks.addAll(taintVector);
																	propagationHistory.put(propKey, sinks);
																} else {
																	List<Integer> sinks = propagationHistory
																			.get(propKey);

																	for (Integer taint : taintVector) {
																		if (!sinks.contains(taint)) {
																			noNewTaint = false;
																			sinks.add(taint);
																		}
																	}
																}

																if (noNewTaint) {
																	break;
																}

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(
																			"PROPAGATING from METHOD: " + sMethod);
																	System.out.println(
																			"PROPAGATING from STATEMENT: " + stmt);
																}
																taintedFieldsInCaller.addAll(taintedFields);
																Vector<Integer> tainted = propagate(target, taintVector,
																		flowSink, stmt, sMethod);
																for (SootField sf : taintedFieldsInCallee) {
																	if (!taintedFields.contains(sf)) {
																		taintedFields.add(sf);
																	}
																}
																taintedFieldsInCallee.clear();

																if (MyConstants.DEBUG_INFO) {
																	System.out.println(stmt + " |taint:" + taintVector
																			+ "| PROPAGATION result: " + tainted);
																}
																if ((tainted != null) && (!tainted.isEmpty())) {
																	for (Integer i : tainted) {
																		int index = i.intValue();

																		if (index == MyConstants.thisObject) {
																			if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																				Value taintedThisRef = ((InstanceInvokeExpr) stmt
																						.getInvokeExpr()).getBase();

																				if (taintedThisRef instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedThisRef,
																							stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);
																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}

																			}

																		} else if (index >= 0) {

																			Value taintedArg = stmt.getInvokeExpr()
																					.getArg(index);

																			if (taintedArg instanceof Local) {
																				List<Unit> defs0 = mrd.getDefsOfAt(
																						(Local) taintedArg, stmt);

																				for (Unit defn : defs0) {

																					APIGraphNode defNode = CreateOrGetExistingNode(
																							(Stmt) defn, sMethod);
																					stmtNode.addPred(defNode);
																					defNode.addSucc(stmtNode);
																					if (!methodToDDGMap.get(sMethod)
																							.contains(defNode)) {
																						methodToDDGMap.get(sMethod)
																								.add(defNode);
																					}

																					UseWithScope defnWS = new UseWithScope(
																							(Stmt) defn, stmt);
																					if (!uses.containsKey(defn)) {
																						Vector<Stmt> scopes = new Vector<Stmt>();
																						scopes.add(stmt);
																						uses.put((Stmt) defn, scopes);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					} else if (!(uses.get(defn)
																							.contains(stmt))) {
																						uses.get(defn).add(stmt);
																						usesStack.push(defnWS);
																						if (MyConstants.DEBUG_INFO)
																							System.out.println(
																									"use stack doesn't contain "
																											+ defnWS.dump()
																											+ ". Push it.");
																					}
																				}
																			}
																		}
																	}
																}

															}

														} else if (stmt instanceof DefinitionStmt) {

															Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
															if (rhsInvoke instanceof InvokeExpr) {

																Vector<Integer> taintVector = new Vector<Integer>();

																Iterator defIt2 = s.getDefBoxes().iterator();
																while (defIt2.hasNext()) {
																	ValueBox vbox2 = (ValueBox) defIt2.next();
																	if (vbox2.getValue() instanceof Local) {
																		// System.out.println(vbox2.getValue());
																		InvokeExpr invokeEx = stmt.getInvokeExpr();
																		int argCount = invokeEx.getArgCount();
																		for (int i = 0; i < argCount; i++) {
																			if (invokeEx.getArg(i) == vbox2
																					.getValue()) {
																				taintVector.add(i);
																			}
																		}

																		if (invokeEx instanceof InstanceInvokeExpr) {
																			if (((InstanceInvokeExpr) invokeEx)
																					.getBase() == vbox2.getValue()) {
																				taintVector.add(MyConstants.thisObject);
																			}
																		}
																	}
																}

																Iterator targets = null;
																if (stmt.getInvokeExpr().getMethod().isConcrete()) {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																	defaultTargets
																			.add(stmt.getInvokeExpr().getMethod());
																	targets = defaultTargets.iterator();
																} else {
																	if (MyConstants.DEBUG_INFO)
																		System.out.println(stmt
																				+ " calls NON-CONCRETE method: "
																				+ stmt.getInvokeExpr().getMethod());
																	targets = new Targets(
																			this.callGraph.edgesOutOf(stmt));

																	if (!targets.hasNext()) {
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(stmt
																					+ " does NOT have a target. add a DEFAULT one");
																		List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
																		defaultTargets
																				.add(stmt.getInvokeExpr().getMethod());
																		targets = defaultTargets.iterator();
																	}
																}

																if (targets == null) {
																	continue;
																}

																while (targets.hasNext()) {
																	SootMethod target = (SootMethod) targets.next();

																	boolean noNewTaint = true;

																	String propKey = sMethod.getSignature() + "|"
																			+ stmt.toString() + "|"
																			+ Integer.toHexString(
																					System.identityHashCode(stmt));

																	if (!propagationHistory.containsKey(propKey)) {
																		noNewTaint = false;
																		List<Integer> sinks = new ArrayList<Integer>();
																		sinks.addAll(taintVector);
																		propagationHistory.put(propKey, sinks);
																	} else {
																		List<Integer> sinks = propagationHistory
																				.get(propKey);

																		for (Integer taint : taintVector) {
																			if (!sinks.contains(taint)) {
																				noNewTaint = false;
																				sinks.add(taint);
																			}
																		}
																	}

																	if (noNewTaint) {
																		break;
																	}

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(
																				"PROPAGATING from METHOD: " + sMethod);
																		System.out.println(
																				"PROPAGATING from STATEMENT: " + stmt);
																	}
																	taintedFieldsInCaller.addAll(taintedFields);
																	Vector<Integer> tainted = propagate(target,
																			taintVector, flowSink, stmt, sMethod);
																	for (SootField sf : taintedFieldsInCallee) {
																		if (!taintedFields.contains(sf)) {
																			taintedFields.add(sf);
																		}
																	}
																	taintedFieldsInCallee.clear();

																	if (MyConstants.DEBUG_INFO) {
																		System.out.println(stmt + " |taint:"
																				+ taintVector + "| PROPAGATION result: "
																				+ tainted);
																	}
																	if ((tainted != null) && (!tainted.isEmpty())) {
																		for (Integer i : tainted) {
																			int index = i.intValue();

																			if (index == MyConstants.thisObject) {
																				if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																					Value taintedThisRef = ((InstanceInvokeExpr) stmt
																							.getInvokeExpr()).getBase();

																					if (taintedThisRef instanceof Local) {
																						List<Unit> defs0 = mrd
																								.getDefsOfAt(
																										(Local) taintedThisRef,
																										stmt);

																						for (Unit defn : defs0) {

																							APIGraphNode defNode = CreateOrGetExistingNode(
																									(Stmt) defn,
																									sMethod);
																							stmtNode.addPred(defNode);
																							defNode.addSucc(stmtNode);
																							if (!methodToDDGMap
																									.get(sMethod)
																									.contains(
																											defNode)) {
																								methodToDDGMap
																										.get(sMethod)
																										.add(defNode);
																							}

																							UseWithScope defnWS = new UseWithScope(
																									(Stmt) defn, stmt);
																							if (!uses.containsKey(
																									defn)) {
																								Vector<Stmt> scopes = new Vector<Stmt>();
																								scopes.add(stmt);
																								uses.put((Stmt) defn,
																										scopes);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							} else if (!(uses.get(defn)
																									.contains(stmt))) {
																								uses.get(defn)
																										.add(stmt);
																								usesStack.push(defnWS);
																								if (MyConstants.DEBUG_INFO)
																									System.out.println(
																											"use stack doesn't contain "
																													+ defnWS.dump()
																													+ ". Push it.");
																							}
																						}
																					}

																				}

																			} else if (index >= 0) {

																				Value taintedArg = stmt.getInvokeExpr()
																						.getArg(index);

																				if (taintedArg instanceof Local) {
																					List<Unit> defs0 = mrd.getDefsOfAt(
																							(Local) taintedArg, stmt);

																					for (Unit defn : defs0) {

																						APIGraphNode defNode = CreateOrGetExistingNode(
																								(Stmt) defn, sMethod);
																						stmtNode.addPred(defNode);
																						defNode.addSucc(stmtNode);
																						if (!methodToDDGMap.get(sMethod)
																								.contains(defNode)) {
																							methodToDDGMap.get(sMethod)
																									.add(defNode);
																						}

																						UseWithScope defnWS = new UseWithScope(
																								(Stmt) defn, stmt);
																						if (!uses.containsKey(defn)) {
																							Vector<Stmt> scopes = new Vector<Stmt>();
																							scopes.add(stmt);
																							uses.put((Stmt) defn,
																									scopes);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						} else if (!(uses.get(defn)
																								.contains(stmt))) {
																							uses.get(defn).add(stmt);
																							usesStack.push(defnWS);
																							if (MyConstants.DEBUG_INFO)
																								System.out.println(
																										"use stack doesn't contain "
																												+ defnWS.dump()
																												+ ". Push it.");
																						}
																					}
																				}
																			}
																		}
																	}

																}
															}
														}

													} // isStmtUsingS
												}
											} // if(lhs.getType() instanceof
												// RefLikeType){

											// rhs is parameter ref
											if (rhs instanceof ParameterRef) {
												if (MyConstants.DEBUG_INFO) {
													System.out.println("returning to caller...");
												}
												if (MyConstants.DEBUG_INFO)
													System.out.println("return to caller from: " + sMethod + " | " + s);

												Chain<SootClass> classes_inner = Scene.v().getClasses();
												Iterator<SootClass> classes_inner_iter = classes_inner.iterator();
												while (classes_inner_iter.hasNext()) {
													SootClass soot_class = classes_inner_iter.next();

													if (soot_class.isApplicationClass() == false) {
														continue;
													}

													List<SootMethod> methods = soot_class.getMethods();
													for (int methIdx = 0; methIdx < methods.size(); ++methIdx) {
														SootMethod method = methods.get(methIdx);

														if (!method.isConcrete()) {
															continue;
														}

														JimpleBody callerBody = (JimpleBody) method
																.retrieveActiveBody();
														Iterator callerIter = callerBody.getUnits().iterator();
														while (callerIter.hasNext()) {
															Stmt callerStmt = (Stmt) callerIter.next();
															if (callerStmt.containsInvokeExpr()) {
																SootMethod calleeMethod;
																try {
																	calleeMethod = callerStmt.getInvokeExpr()
																			.getMethod();
																} catch (Throwable e) {
																	continue;
																}
																if (calleeMethod.isConcrete()) {
																	if (calleeMethod.equals(sMethod)) {

																		APIGraphNode callerStmtNode = CreateOrGetExistingNode(
																				callerStmt, method);
																		sNode.addPred(callerStmtNode);
																		callerStmtNode.addSucc(sNode);
																		if (!methodToDDGMap.containsKey(method)) {
																			List<APIGraphNode> ddg = new ArrayList<APIGraphNode>();
																			ddg.add(callerStmtNode);
																			methodToDDGMap.put(method, ddg);
																		} else {
																			if (!methodToDDGMap.get(method)
																					.contains(callerStmtNode)) {
																				methodToDDGMap.get(method)
																						.add(callerStmtNode);
																			}
																		}

																		if (!fullWorklist.contains(method)) {
																			worklist.add(method);
																			fullWorklist.add(method);
																		}

																		if (!sourceMethods.contains(sMethod)) {
																			if (MyConstants.DEBUG_INFO)
																				System.out
																						.println("adding sourceMethod: "
																								+ sMethod);
																			sourceMethods.add(sMethod);
																		}
																	}
																} else {
																	Iterator targets = new Targets(
																			this.callGraph.edgesOutOf(callerStmt));

																	while (targets.hasNext()) {
																		SootMethod target = (SootMethod) targets.next();
																		if (target.equals(sMethod)) {

																			APIGraphNode callerStmtNode = CreateOrGetExistingNode(
																					callerStmt, method);
																			sNode.addPred(callerStmtNode);
																			callerStmtNode.addSucc(sNode);
																			if (!methodToDDGMap.containsKey(method)) {
																				List<APIGraphNode> ddg = new ArrayList<APIGraphNode>();
																				ddg.add(callerStmtNode);
																				methodToDDGMap.put(method, ddg);
																			} else {
																				if (!methodToDDGMap.get(method)
																						.contains(callerStmtNode)) {
																					methodToDDGMap.get(method)
																							.add(callerStmtNode);
																				}
																			}

																			if (!fullWorklist.contains(method)) {
																				worklist.add(method);
																				fullWorklist.add(method);
																			}

																			if (!sourceMethods.contains(sMethod)) {
																				if (MyConstants.DEBUG_INFO)
																					System.out.println(
																							"adding sourceMethod: "
																									+ sMethod);
																				sourceMethods.add(sMethod);
																			}
																		}
																	}
																}
															}
														}
													}
												}
											} else if (rhs instanceof InstanceFieldRef) {
												if (MyConstants.TO_TAINT_INSTANCE_FIELD) {
													SootField fieldKey = ((InstanceFieldRef) rhs).getField();
													if (!taintedFields.contains(fieldKey)) {
														if (MyConstants.DEBUG_INFO)
															System.out.println("adding new field as source: "
																	+ ((InstanceFieldRef) rhs).getField() + " from: "
																	+ s);

														taintedFields.add(fieldKey);
														if (fieldKey.getType() instanceof RefLikeType) {
															fWorklist.add(fieldKey);
														}
													}

													if (fieldToUsesMap.containsKey(fieldKey)) {
														List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
														if (!fieldUses.contains(s)) {
															fieldUses.add(s);
														}
													} else {
														List<Stmt> fieldUses = new ArrayList<Stmt>();
														fieldToUsesMap.put(fieldKey, fieldUses);
														fieldUses.add(s);
													}
												}
											} else if (rhs instanceof StaticFieldRef) {
												if (MyConstants.TO_TAINT_STATIC_FIELD) {
													SootField fieldKey = ((StaticFieldRef) rhs).getField();
													if (!taintedFields.contains(fieldKey)) {
														if (MyConstants.DEBUG_INFO)
															System.out.println("adding new field as source: "
																	+ ((StaticFieldRef) rhs).getField() + " from: "
																	+ s);
														taintedFields.add(fieldKey);
														if (fieldKey.getType() instanceof RefLikeType) {
															fWorklist.add(fieldKey);
														}
													}

													if (fieldToUsesMap.containsKey(fieldKey)) {
														List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
														if (!fieldUses.contains(s)) {
															fieldUses.add(s);
														}
													} else {
														List<Stmt> fieldUses = new ArrayList<Stmt>();
														fieldToUsesMap.put(fieldKey, fieldUses);
														fieldUses.add(s);
													}
												}
											}

											Iterator<ValueBox> sUseIter = s.getUseBoxes().iterator();
											while (sUseIter.hasNext()) {
												Value v = sUseIter.next().getValue();
												if (v instanceof Local) {

													List<Unit> defs = mrd.getDefsOfAt((Local) v, s);

													for (Unit defn : defs) {

														APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
																sMethod);
														sNode.addPred(defNode);
														defNode.addSucc(sNode);
														if (!methodToDDGMap.get(sMethod).contains(defNode)) {
															methodToDDGMap.get(sMethod).add(defNode);
														}

														UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
														if (!uses.containsKey(defn)) {
															Vector<Stmt> scopes = new Vector<Stmt>();
															scopes.add(s);
															uses.put((Stmt) defn, scopes);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														} else if (!(uses.get(defn).contains(s))) {
															uses.get(defn).add(s);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														}
													}
												}
											}

										} // if(s instanceof DefinitionStmt){

									}
								}
							} // while(!usesStack.isEmpty()){

							Iterator i = uses.keySet().iterator();
							while (i.hasNext()) {
								Stmt s = (Stmt) i.next();
								AddTags(s, taintTag);

								Iterator usesIt = s.getUseBoxes().iterator();
								while (usesIt.hasNext()) {
									ValueBox vbox = (ValueBox) usesIt.next();
									if (vbox.getValue() instanceof Local) {
										Local l = (Local) vbox.getValue();

										Iterator rDefsIt = mrd.getDefsOfAt(l, s).iterator();
										while (rDefsIt.hasNext()) {
											Stmt next = (Stmt) rDefsIt.next();
											if (!next.getTags().contains(taintTag)) {
												AddTags(s, extraDefTag);
											}
										}
									}
								}
							}

							if (MyConstants.DEBUG_INFO) {
								System.out.println();
								System.out.println("method:" + sMethod.getSignature());
								System.out.println("dataflow for " + sink + ":");
							}
							Iterator printIt = body.getUnits().iterator();
							while (printIt.hasNext()) {
								Stmt s = (Stmt) printIt.next();
								if (s.getTags().contains(taintTag) || s.getTags().contains(extraDefTag)) {
									if (MyConstants.DEBUG_INFO) {
										dumpTaggedStmt(s);
									}

									Vector<Integer> labels = new Vector<Integer>();

									for (Tag tag : s.getTags()) {
										if (taintTagReverseMap.containsKey(tag)) {
											Integer label = ((TaintTag) tag).getLabel();
											if (!labels.contains(label)) {
												labels.add(label);
											}
										} else if (extraDefTagReverseMap.containsKey(tag)) {
											Integer label = ((TaintTag) tag).getLabel();
											if (!labels.contains(label)) {
												labels.add(label);
											}
										}
									}

									List<ValueBox> vbs = s.getUseAndDefBoxes();
									Iterator iter = vbs.iterator();
									while (iter.hasNext()) {
										ValueBox vb = (ValueBox) iter.next();
										if (vb.getValue() instanceof InstanceFieldRef) {
											SootField instanceField = ((InstanceFieldRef) vb.getValue()).getField();

											if (instanceField.getDeclaringClass().isApplicationClass() == false) {
												continue;
											}

											// if(!instanceFields.contains(instanceField)){
											// instanceFields.add(instanceField);
											// }
											////
											if (!instanceFieldMap.containsKey(instanceField)) {

												Vector<Integer> taintSources = new Vector<Integer>();
												taintSources.addAll(labels);
												instanceFieldMap.put(instanceField, taintSources);

											} else {

												Vector<Integer> taintSources = instanceFieldMap.get(instanceField);
												for (Integer label : labels) {
													if (!taintSources.contains(label)) {
														taintSources.add(label);
													}
												}
											}
											////

											LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
											List<String> fieldList = new ArrayList<String>();
											if (fieldList.contains(instanceField.getSignature())) {
												fieldList.add(instanceField.getSignature());
											}
											taintSourceToField.put(flowSink, fieldList);

										} else if (vb.getValue() instanceof StaticFieldRef) {
											SootField staticField = ((StaticFieldRef) vb.getValue()).getField();

											if (staticField.getDeclaringClass().isApplicationClass() == false) {
												continue;
											}

											// if(!staticFields.contains(staticField)){
											// staticFields.add(staticField);
											// }
											///
											if (!staticFieldMap.containsKey(staticField)) {

												Vector<Integer> taintSources = new Vector<Integer>();
												taintSources.addAll(labels);
												staticFieldMap.put(staticField, taintSources);

											} else {

												Vector<Integer> taintSources = staticFieldMap.get(staticField);
												for (Integer label : labels) {
													if (!taintSources.contains(label)) {
														taintSources.add(label);
													}
												}
											}
											///

											LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
											List<String> fieldList = new ArrayList<String>();
											if (fieldList.contains(staticField.getSignature())) {
												fieldList.add(staticField.getSignature());
											}
											taintSourceToField.put(flowSink, fieldList);

										} else if (vb.getValue() instanceof Local) {

											String varName = ((Local) vb.getValue()).getName();
											LinkedHashMap<String, List<String>> taintSourceToVar = new LinkedHashMap<String, List<String>>();
											List<String> varList = new ArrayList<String>();
											if (varList.contains(varName)) {
												varList.add(varName);
											}
											taintSourceToVar.put(flowSink, varList);
										}
									}
								}
							}

							if (MyConstants.DEBUG_INFO) {
								System.out.println("end dataflow for " + sink + "\n");
							}

						} // while(!worklist.isEmpty())

					} // end while(!fWorklist.isEmpty())

					Set<SootField> fieldSet = fieldToDefsMap.keySet();
					Iterator<SootField> fieldIter = fieldSet.iterator();
					while (fieldIter.hasNext()) {
						SootField field = fieldIter.next();
						List<Stmt> fieldDefs = fieldToDefsMap.get(field);
						if (fieldToUsesMap.containsKey(field)) {
							List<Stmt> fieldUses = fieldToUsesMap.get(field);
							for (Stmt fieldDef : fieldDefs) {

								if (fieldDef instanceof DefinitionStmt) {
									Value lhs = ((DefinitionStmt) fieldDef).getLeftOp();
									if (lhs instanceof StaticFieldRef) {
										APIGraphNode fieldDefNode = stmtToNodeMap.get(fieldDef);
										for (Stmt fieldUse : fieldUses) {
											APIGraphNode fieldUseNode = stmtToNodeMap.get(fieldUse);
											fieldDefNode.addSucc(fieldUseNode);
											fieldUseNode.addPred(fieldDefNode);
										}
									} else if (lhs instanceof InstanceFieldRef) {
										Value defBase = ((InstanceFieldRef) lhs).getBase();
										APIGraphNode fieldDefNode = stmtToNodeMap.get(fieldDef);
										for (Stmt fieldUse : fieldUses) {
											if (fieldUse instanceof DefinitionStmt) {
												Value rhs = ((DefinitionStmt) fieldUse).getRightOp();
												if (rhs instanceof InstanceFieldRef) {

													Value useBase = ((InstanceFieldRef) rhs).getBase();
													if (this.pta.reachingObjects((Local) defBase)
															.hasNonEmptyIntersection(
																	this.pta.reachingObjects((Local) useBase))
															|| this.pta.reachingObjects((Local) defBase).isEmpty()) {

														APIGraphNode fieldUseNode = stmtToNodeMap.get(fieldUse);
														fieldDefNode.addSucc(fieldUseNode);
														fieldUseNode.addPred(fieldDefNode);
													}
												}
											}
										}
									}
								}
							}
						}
					}

					Set<SootField> instanceKeySet = instanceFieldMap.keySet();
					Iterator<SootField> instanceIter = instanceKeySet.iterator();
					while (instanceIter.hasNext()) {
						SootField f = instanceIter.next();
						Vector<Integer> newLabels = instanceFieldMap.get(f);

						if (usedInstanceFieldMap.containsKey(f)) {
							Vector<Integer> oldLabels = usedInstanceFieldMap.get(f);
							for (Integer label : newLabels) {
								if (!oldLabels.contains(label)) {
									oldLabels.add(label);
								}
							}
						} else {
							Vector<Integer> labels = new Vector<Integer>();
							labels.addAll(newLabels);
							usedInstanceFieldMap.put(f, labels);
						}
					}

					Set<SootField> staticKeySet = staticFieldMap.keySet();
					Iterator<SootField> staticIter = staticKeySet.iterator();
					while (staticIter.hasNext()) {
						SootField f = staticIter.next();
						Vector<Integer> newLabels = staticFieldMap.get(f);

						if (usedStaticFieldMap.containsKey(f)) {
							Vector<Integer> oldLabels = usedStaticFieldMap.get(f);
							for (Integer label : newLabels) {
								if (!oldLabels.contains(label)) {
									oldLabels.add(label);
								}
							}
						} else {
							Vector<Integer> labels = new Vector<Integer>();
							labels.addAll(newLabels);
							usedStaticFieldMap.put(f, labels);
						}
					}

					// check whether we should look upward for data dependencies
					Set<SootMethod> considerIntoEdgesFor = new LinkedHashSet<SootMethod>();
					Set<Stmt> ddgKeySet = this.stmtToNodeMap.keySet();
					Iterator<Stmt> ddgIter = ddgKeySet.iterator();
					while (ddgIter.hasNext()) {
						Stmt s = ddgIter.next();
						APIGraphNode ddgNode = this.stmtToNodeMap.get(s);
						/*
						 * isDefWithParam checks whether current stmt depends on method parameters. If
						 * so, look upwards for methods that invokes current method.
						 * 
						 * however, we are going to cover all possible paths that leads to a sink,
						 * regardless of whether there is a data dependency.
						 */
						if (isDefWithParam(ddgNode.getStmt()) && upwardMethods.contains(ddgNode.getHostMethod())) {
							considerIntoEdgesFor.add(ddgNode.getHostMethod());
						}
					}

					for (SootMethod consMeth : considerIntoEdgesFor) {
						Iterator<Edge> itInto = this.callGraph.edgesInto(consMeth);
						while (itInto.hasNext()) {
							pendingIntoEdges.add(itInto.next());
						}
					}
				} while (!pendingIntoEdges.isEmpty());

				Set<Stmt> sKeySet = this.stmtToNodeMap.keySet();
				Iterator<Stmt> sIter = sKeySet.iterator();
				Stmt sinkStmt = null;

				while (sIter.hasNext()) {
					Stmt s = sIter.next();
					if (s.toString().contains(flowSink)) {
						sinkStmt = s;
						break;
					}
				}

				if (this.ddgHandler != null) {
					this.ddgHandler.onDDGAvailable(entrySig, sinkStmt, this.stmtToNodeMap);
				}
				clearDataStructures();
			}
		}
	}

	private APIGraphNode CreateOrGetExistingNode(Stmt s, SootMethod sMethod) {
		APIGraphNode sNode = null;
		if (!stmtToNodeMap.containsKey(s)) {
			sNode = new APIGraphNode(s, sMethod);
			if (isAndroidAPICall(s)) {
				apiNodeCount++;
			}
			stmtToNodeMap.put(s, sNode);
		} else {
			sNode = stmtToNodeMap.get(s);
		}
		return sNode;
	}

	private boolean isAndroidAPICall(Stmt s) {
		if (s.containsInvokeExpr()) {
			SootClass c = s.getInvokeExpr().getMethod().getDeclaringClass();
			String str = c.toString();

			if (!c.isApplicationClass()) {
				if (str.contains("java.io.") || str.contains("org.apache.http.client.")
						|| str.contains("org.apache.http.impl.client.") || str.contains("java.net.")) {
					return true;
				} else if (!str.contains("java.") && !str.contains("javax.") && !str.contains("org.")) {
					return true;
				}
			}
		}

		return false;
	}

	public static void printDDGEdgeList(String sig, List<APIGraphNode> apiGraph) {
		if (null == apiGraph || apiGraph.size() == 0) {
			Log.dumpln("graph is empty");
			return;
		}

		LinkedHashMap<APIGraphNode, Integer> nodeToIndex = new LinkedHashMap<APIGraphNode, Integer>();
		int index = 0;
		for (APIGraphNode node : apiGraph) {
			nodeToIndex.put(node, index);
			index++;
		}

		Log.dumpln("Start of G " + sig);
		Log.dumpln("digraph G {");

		for (APIGraphNode node : apiGraph) {
			nodeToIndex.get(node).intValue();
			List<APIGraphNode> succNodes = node.getSuccessors();
			String nodeSig = "";
			nodeSig = node.getHostMethod() + ": " + node.getStmt().toString();
			nodeSig = nodeSig.replace("\"", "'");
			Log.dumpln("DGNode:" + nodeSig + "-->");

			for (APIGraphNode succNode : succNodes) {
				String succNodeSig = "";
				succNodeSig = succNode.getHostMethod() + ": " + succNode.getStmt().toString();
				succNodeSig = succNodeSig.replace("\"", "'");
				// Log.dumpln(" " + "\"" + nodeSig + "\" -> " + "\"" +
				// succNodeSig + "\";");
				Log.dumpln("DGSucc:" + succNodeSig);
			}
		}

		Log.dumpln("}\n");
	}

	private void dumpTaggedStmt(Stmt s) {
		StringBuilder sb = new StringBuilder(s + "|[");

		int count = s.getTags().size();
		for (int i = 0; i < count; i++) {
			if (s.getTags().get(i) instanceof TaintTag) {
				if (i == count - 1) {
					sb.append(((TaintTag) s.getTags().get(i)).getSecondaryName());
				} else {
					sb.append(((TaintTag) s.getTags().get(i)).getSecondaryName() + ",");
				}
			}
		}
		sb.append("]");
		System.out.println(sb.toString());
	}

	private void AddTags(Stmt s, Tag tag) {
		if (!s.getTags().contains(tag)) {
			s.addTag(tag);
		}
	}

	@SuppressWarnings("rawtypes")
	private Vector<Integer> propagate(SootMethod sMethod, Vector<Integer> taintIndexes, String flowSink, Stmt from,
			SootMethod fromMethod) {
		if (MyConstants.DEBUG_INFO) {
			System.out.println("PROPAGATE--sMethod:" + sMethod.toString() + "\nPROPAGATE--flowSink:" + flowSink
					+ "\nPROPAGATE--from:" + from.toString() + "\nPROPAGATE--fromMethod:" + fromMethod.toString());
		}

		if (callString.contains(sMethod)) {
			if (MyConstants.DEBUG_INFO) {
				System.out.println("RECURSIVE call found, return null");
			}

			return null;
		}

		if (callString.size() > MyConstants.DFA_DEPTH) {
			if (MyConstants.DEBUG_INFO) {
				System.out.println("exceeds defined DEPTH, return null");
			}

			return null;
		}

		if (!methodToDDGMap.containsKey(sMethod)) {
			List<APIGraphNode> apiGraph = new ArrayList<APIGraphNode>();
			methodToDDGMap.put(sMethod, apiGraph);
		}

		TaintTag taintTag = taintTagMap.get(flowSink);
		TaintTag extraDefTag = extraDefTagMap.get(flowSink);

		callString.push(sMethod);

		if (MyConstants.DEBUG_INFO) {
			System.out.println("step into method: " + sMethod + "|taintIndexes: " + taintIndexes + "\n");
		}

		LinkedHashMap<SootField, Vector<Integer>> instanceFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();
		LinkedHashMap<SootField, Vector<Integer>> staticFieldMap = new LinkedHashMap<SootField, Vector<Integer>>();

		List<SootField> taintedFields = new ArrayList<SootField>();
		taintedFields.addAll(taintedFieldsInCaller);
		taintedFieldsInCaller.clear();

		Vector<Integer> taintResult = new Vector<Integer>();

		// function summaries would be inserted here
		if (sMethod.getDeclaringClass().isApplicationClass() == false || (!sMethod.isConcrete())) {
			if (MyConstants.DEBUG_INFO) {
				System.out.println(sMethod + " is not declared in an application class");
				System.out.println("CHECK if " + sMethod.getSignature() + " has a function summary?");
			}

			LinkedHashMap<Integer, List<Integer>> result = AndroidFunctionSummary
					.lookupFunctionSummary(sMethod.getSignature());
			if (result == null) {
				if (MyConstants.DEBUG_INFO) {
					System.out.println(
							sMethod.getSignature() + " has NO function summary, adding all params into taint ...");
				}

				int paraCount = sMethod.getParameterCount();
				for (int count = 0; count < paraCount; count++) {
					taintResult.add(count);
				}

				if (!taintIndexes.contains(MyConstants.thisObject)) {
					if (!sMethod.isStatic()) {
						taintResult.add(MyConstants.thisObject);
					}
				}

				callString.pop();
				return taintResult;
			} else {
				if (MyConstants.DEBUG_INFO) {
					System.out.println(sMethod.getSignature() + " HAS function SUMMARY");
				}

				Set<Integer> sources = result.keySet();
				Iterator iterSources = sources.iterator();
				while (iterSources.hasNext()) {
					Integer source = (Integer) iterSources.next();
					List<Integer> dests = result.get(source);
					for (Integer dest : dests) {
						if (taintIndexes.contains(dest)) {
							if (!taintResult.contains(source)) {
								taintResult.add(source);
							}
						}
					}
				}

				if (MyConstants.DEBUG_INFO) {
					System.out.println("function summary tells which ones are tainted: " + taintResult);
				}

				callString.pop();
				return taintResult;
			}
		}

		Stmt sink = null;
		List<Local> localSinkVars = new ArrayList<Local>();
		LinkedHashMap<Stmt, Vector<Stmt>> uses = new LinkedHashMap<Stmt, Vector<Stmt>>();
		Stack<UseWithScope> usesStack = new Stack<UseWithScope>();
		new Vector<UseWithScope>();

		JimpleBody body = (JimpleBody) sMethod.retrieveActiveBody();

		ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);
		MyReachingDefinition mrd = new MyReachingDefinition(eug);

		{
			Iterator it = body.getUnits().iterator();
			while (it.hasNext()) {
				Stmt s = (Stmt) it.next();
				if (s instanceof IdentityStmt) {
					List<ValueBox> vBoxes = ((IdentityStmt) s).getUseBoxes();
					Iterator iBox = vBoxes.iterator();
					while (iBox.hasNext()) {
						ValueBox vBox = (ValueBox) iBox.next();
						if (vBox.getValue() instanceof ParameterRef) {

							if (taintIndexes.contains(((ParameterRef) vBox.getValue()).getIndex())) {
								Value lhs = ((IdentityStmt) s).getLeftOp();

								if (lhs instanceof Local) {
									if (!localSinkVars.contains(lhs)) {
										localSinkVars.add((Local) lhs);
									}
								}
							}
						} else if (vBox.getValue() instanceof ThisRef) {
							if (taintIndexes.contains(MyConstants.thisObject)) {

								Value lhs = ((IdentityStmt) s).getLeftOp();

								if (lhs instanceof Local) {
									if (!localSinkVars.contains(lhs)) {
										localSinkVars.add((Local) lhs);
									}
								}
							}
						}
					}
				} else if (s instanceof ReturnStmt) {
					if (taintIndexes.contains(MyConstants.returnValue)) {

						APIGraphNode returnNode = CreateOrGetExistingNode(s, sMethod);

						if (!methodToDDGMap.get(sMethod).contains(returnNode)) {
							methodToDDGMap.get(sMethod).add(returnNode);
						}

						APIGraphNode fromNode = CreateOrGetExistingNode(from, fromMethod);
						if (!methodToDDGMap.get(fromMethod).contains(fromNode)) {
							methodToDDGMap.get(fromMethod).add(fromNode);
						}

						fromNode.addPred(returnNode);
						returnNode.addSucc(fromNode);

						Value op = ((ReturnStmt) s).getOp();
						if (op instanceof Local) {
							UseWithScope sWS = new UseWithScope(s, s);
							if (!uses.containsKey(s)) {
								uses.put(s, new Vector<Stmt>());
								usesStack.push(sWS);
								if (MyConstants.DEBUG_INFO)
									System.out.println(
											"use stack doesn't contain return stmt " + sWS.dump() + ". Push it.");
								sink = s;
							}
						}
					}
				}
			}

			//// FIX HERE!
			if (MyConstants.CONSIDER_REDEFINE && !localSinkVars.isEmpty()) {
				for (Local localSinkVar : localSinkVars) {
					Iterator it2 = body.getUnits().iterator();
					while (it2.hasNext()) {
						Stmt stmt = (Stmt) it2.next();
						try {
							if (stmt.containsInvokeExpr()) {
								if (!stmt.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()) {
									AddTags(stmt, API_TAG);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}

						if (stmt instanceof DefinitionStmt) {
							boolean usesConstant = false;
							List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
							for (ValueBox ccVB : checkConstUseBoxes) {
								if (ccVB.getValue() instanceof StringConstant) {
									if (!((StringConstant) ccVB.getValue()).value.equals("")) {
										usesConstant = true;
										break;
									}
								}
							}
							if (usesConstant) {
								AddTags(stmt, STRING_CONST_TAG);
							}
						}

						APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);
						if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
							methodToDDGMap.get(sMethod).add(stmtNode);
						}

						if (stmt instanceof InvokeStmt) {
							Vector<Integer> taintVector = new Vector<Integer>();
							InvokeExpr invokeEx = stmt.getInvokeExpr();
							int argCount = invokeEx.getArgCount();
							for (int i = 0; i < argCount; i++) {
								if (invokeEx.getArg(i) == localSinkVar) {
									taintVector.add(i);
								}
							}

							// for instance invoke, consider this reference too.
							if (invokeEx instanceof InstanceInvokeExpr) {
								if (((InstanceInvokeExpr) invokeEx).getBase() == localSinkVar) {
									taintVector.add(MyConstants.thisObject);
								}
							}

							Iterator targets = null;
							if (stmt.getInvokeExpr().getMethod().isConcrete()) {
								if (MyConstants.DEBUG_INFO)
									System.out.println(
											stmt + " calls CONCRETE method: " + stmt.getInvokeExpr().getMethod());
								List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
								defaultTargets.add(stmt.getInvokeExpr().getMethod());
								targets = defaultTargets.iterator();
							} else {
								if (MyConstants.DEBUG_INFO)
									System.out.println(
											stmt + " calls NON-CONCRETE method: " + stmt.getInvokeExpr().getMethod());
								targets = new Targets(this.callGraph.edgesOutOf(stmt));

								if (!targets.hasNext()) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " does NOT have a target. add a DEFAULT one");
									List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
									defaultTargets.add(stmt.getInvokeExpr().getMethod());
									targets = defaultTargets.iterator();
								}
							}

							if (targets == null) {
								continue;
							}

							while (targets.hasNext()) {
								SootMethod target = (SootMethod) targets.next();

								boolean noNewTaint = true;

								String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
										+ Integer.toHexString(System.identityHashCode(stmt));

								if (!propagationHistory.containsKey(propKey)) {
									noNewTaint = false;
									List<Integer> sinks = new ArrayList<Integer>();
									sinks.addAll(taintVector);
									propagationHistory.put(propKey, sinks);
								} else {
									List<Integer> sinks = propagationHistory.get(propKey);

									for (Integer taint : taintVector) {
										if (!sinks.contains(taint)) {
											noNewTaint = false;
											sinks.add(taint);
										}
									}
								}

								if (noNewTaint) {
									break;
								}

								if (MyConstants.DEBUG_INFO) {
									System.out.println("PROPAGATING from METHOD: " + sMethod);
									System.out.println("PROPAGATING from STATEMENT: " + stmt);
								}

								taintedFieldsInCaller.addAll(taintedFields);
								Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt, sMethod);
								for (SootField sf : taintedFieldsInCallee) {
									if (!taintedFields.contains(sf)) {
										taintedFields.add(sf);
									}
								}
								taintedFieldsInCallee.clear();
								if (MyConstants.DEBUG_INFO) {
									System.out.println(
											stmt + " |taint:" + taintVector + "| PROPAGATION result: " + tainted);
								}
								if ((tainted != null) && (!tainted.isEmpty())) {
									for (Integer i : tainted) {
										int index = i.intValue();

										if (index == MyConstants.thisObject) {
											if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
												Value taintedThisRef = ((InstanceInvokeExpr) stmt.getInvokeExpr())
														.getBase();

												if (taintedThisRef instanceof Local) {
													List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedThisRef, stmt);

													for (Unit defn : defs0) {

														APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
																sMethod);

														stmtNode.addPred(defNode);
														defNode.addSucc(stmtNode);

														if (!methodToDDGMap.get(sMethod).contains(defNode)) {
															methodToDDGMap.get(sMethod).add(defNode);
														}

														UseWithScope defnWS = new UseWithScope((Stmt) defn, stmt);
														if (!uses.containsKey(defn)) {
															Vector<Stmt> scopes = new Vector<Stmt>();
															scopes.add(stmt);
															uses.put((Stmt) defn, scopes);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														} else if (!(uses.get(defn).contains(stmt))) {
															uses.get(defn).add(stmt);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														}
													}
												}

											}

										} else if (index >= 0) {
											Value taintedArg = stmt.getInvokeExpr().getArg(index);
											if (taintedArg instanceof Local) {
												List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg, stmt);

												for (Unit defn : defs0) {

													APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
															sMethod);

													stmtNode.addPred(defNode);
													defNode.addSucc(stmtNode);

													if (!methodToDDGMap.get(sMethod).contains(defNode)) {
														methodToDDGMap.get(sMethod).add(defNode);
													}

													UseWithScope defnWS = new UseWithScope((Stmt) defn, stmt);
													if (!uses.containsKey(defn)) {
														Vector<Stmt> scopes = new Vector<Stmt>();
														scopes.add(stmt);
														uses.put((Stmt) defn, scopes);
														usesStack.push(defnWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println("use stack doesn't contain "
																	+ defnWS.dump() + ". Push it.");
													} else if (!(uses.get(defn).contains(stmt))) {
														uses.get(defn).add(stmt);
														usesStack.push(defnWS);
														if (MyConstants.DEBUG_INFO)
															System.out.println("use stack doesn't contain "
																	+ defnWS.dump() + ". Push it.");
													}
												}
											}
										}
									}
								}
							}
						} else if (stmt instanceof DefinitionStmt) {
							Value rhs = ((DefinitionStmt) stmt).getRightOp();
							if (rhs instanceof InvokeExpr) {
								Vector<Integer> taintVector = new Vector<Integer>();
								// System.out.println(vbox2.getValue());
								InvokeExpr invokeEx = stmt.getInvokeExpr();
								int argCount = invokeEx.getArgCount();
								for (int i = 0; i < argCount; i++) {
									if (invokeEx.getArg(i) == localSinkVar) {
										taintVector.add(i);
									}
								}

								// for instance invoke, consider this reference
								// too.
								if (invokeEx instanceof InstanceInvokeExpr) {
									if (((InstanceInvokeExpr) invokeEx).getBase() == localSinkVar) {
										taintVector.add(MyConstants.thisObject);
									}
								}

								Iterator targets = null;
								if (stmt.getInvokeExpr().getMethod().isConcrete()) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(
												stmt + " calls CONCRETE method: " + stmt.getInvokeExpr().getMethod());
									List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
									defaultTargets.add(stmt.getInvokeExpr().getMethod());
									targets = defaultTargets.iterator();
								} else {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " calls NON-CONCRETE method: "
												+ stmt.getInvokeExpr().getMethod());
									targets = new Targets(this.callGraph.edgesOutOf(stmt));

									if (!targets.hasNext()) {
										if (MyConstants.DEBUG_INFO)
											System.out.println(stmt + " does NOT have a target. add a DEFAULT one");
										List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
										defaultTargets.add(stmt.getInvokeExpr().getMethod());
										targets = defaultTargets.iterator();
									}
								}

								if (targets == null) {
									continue;
								}

								while (targets.hasNext()) {
									SootMethod target = (SootMethod) targets.next();

									boolean noNewTaint = true;

									String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
											+ Integer.toHexString(System.identityHashCode(stmt));

									if (!propagationHistory.containsKey(propKey)) {
										noNewTaint = false;
										List<Integer> sinks = new ArrayList<Integer>();
										sinks.addAll(taintVector);
										propagationHistory.put(propKey, sinks);
									} else {
										List<Integer> sinks = propagationHistory.get(propKey);

										for (Integer taint : taintVector) {
											if (!sinks.contains(taint)) {
												noNewTaint = false;
												sinks.add(taint);
											}
										}
									}

									if (noNewTaint) {
										break;
									}

									if (MyConstants.DEBUG_INFO) {
										System.out.println("PROPAGATING from METHOD: " + sMethod);
										System.out.println("PROPAGATING from STATEMENT: " + stmt);
									}
									taintedFieldsInCaller.addAll(taintedFields);
									Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt, sMethod);
									for (SootField sf : taintedFieldsInCallee) {
										if (!taintedFields.contains(sf)) {
											taintedFields.add(sf);
										}
									}
									taintedFieldsInCallee.clear();

									if (MyConstants.DEBUG_INFO) {
										System.out.println(
												stmt + " |taint:" + taintVector + "| PROPAGATION result: " + tainted);
									}
									if ((tainted != null) && (!tainted.isEmpty())) {
										for (Integer i : tainted) {
											int index = i.intValue();

											if (index == MyConstants.thisObject) {
												if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
													Value taintedThisRef = ((InstanceInvokeExpr) stmt.getInvokeExpr())
															.getBase();

													if (taintedThisRef instanceof Local) {
														List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedThisRef,
																stmt);

														for (Unit defn : defs0) {

															APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
																	sMethod);

															stmtNode.addPred(defNode);
															defNode.addSucc(stmtNode);

															if (!methodToDDGMap.get(sMethod).contains(defNode)) {
																methodToDDGMap.get(sMethod).add(defNode);
															}

															UseWithScope defnWS = new UseWithScope((Stmt) defn, stmt);
															if (!uses.containsKey(defn)) {
																Vector<Stmt> scopes = new Vector<Stmt>();
																scopes.add(stmt);
																uses.put((Stmt) defn, scopes);
																usesStack.push(defnWS);
																if (MyConstants.DEBUG_INFO)
																	System.out.println("use stack doesn't contain "
																			+ defnWS.dump() + ". Push it.");
															} else if (!(uses.get(defn).contains(stmt))) {
																uses.get(defn).add(stmt);
																usesStack.push(defnWS);
																if (MyConstants.DEBUG_INFO)
																	System.out.println("use stack doesn't contain "
																			+ defnWS.dump() + ". Push it.");
															}
														}
													}

												}

											} else if (index >= 0) {

												Value taintedArg = stmt.getInvokeExpr().getArg(index);

												if (taintedArg instanceof Local) {
													List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg, stmt);

													for (Unit defn : defs0) {

														APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn,
																sMethod);

														stmtNode.addPred(defNode);
														defNode.addSucc(stmtNode);

														if (!methodToDDGMap.get(sMethod).contains(defNode)) {
															methodToDDGMap.get(sMethod).add(defNode);
														}

														UseWithScope defnWS = new UseWithScope((Stmt) defn, stmt);
														if (!uses.containsKey(defn)) {
															Vector<Stmt> scopes = new Vector<Stmt>();
															scopes.add(stmt);
															uses.put((Stmt) defn, scopes);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														} else if (!(uses.get(defn).contains(stmt))) {
															uses.get(defn).add(stmt);
															usesStack.push(defnWS);
															if (MyConstants.DEBUG_INFO)
																System.out.println("use stack doesn't contain "
																		+ defnWS.dump() + ". Push it.");
														}
													}
												}
											}
										}
									}
								}
							} else {
								// this part added by anonymous
								if (MyConstants.DEBUG_INFO) {
									System.out.println("Label #6 stmt:" + stmt);
								}

								Value lhs = ((DefinitionStmt) stmt).getLeftOp();
								boolean addToUses = false;
								if (lhs instanceof Local && lhs == localSinkVar) {
									addToUses = true;
								} else if (lhs instanceof InstanceFieldRef) {
									if (((InstanceFieldRef) lhs).getBase() == localSinkVar) {
										addToUses = true;
									}
								}

								if (addToUses) {
									List<Local> vals = new ArrayList<Local>();
									if (rhs instanceof Local) {
										vals.add((Local) rhs);
									} else {
										List<ValueBox> boxes = rhs.getUseBoxes();
										for (ValueBox vb : boxes) {
											if (vb.getValue() instanceof Local) {
												Local l = (Local) (vb.getValue());
												vals.add(l);
											}
										}
									}

									for (Local v : vals) {
										if (MyConstants.DEBUG_INFO) {
											System.out.println("Label #6 stmt:" + stmt + " v:" + v);
										}

										List<Unit> defs0 = mrd.getDefsOfAt(v, stmt);

										for (Unit defn : defs0) {
											if (MyConstants.DEBUG_INFO) {
												System.out.println("Label #6 v:" + v + " def:" + defn);
											}

											APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn, sMethod);

											stmtNode.addPred(defNode);
											defNode.addSucc(stmtNode);

											if (!methodToDDGMap.get(sMethod).contains(defNode)) {
												methodToDDGMap.get(sMethod).add(defNode);
											}

											UseWithScope defnWS = new UseWithScope((Stmt) defn, stmt);
											if (!uses.containsKey(defn)) {
												Vector<Stmt> scopes = new Vector<Stmt>();
												scopes.add(stmt);
												uses.put((Stmt) defn, scopes);
												usesStack.push(defnWS);
												if (MyConstants.DEBUG_INFO) {
													System.out.println("Label #6: use stack doesn't contain "
															+ defnWS.dump() + ". Push it.");
												}
											} else if (!(uses.get(defn).contains(stmt))) {
												uses.get(defn).add(stmt);
												usesStack.push(defnWS);
												if (MyConstants.DEBUG_INFO) {
													System.out.println("Label #6: use stack doesn't contain "
															+ defnWS.dump() + ". Push it.");
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		if (uses.isEmpty()) {
			callString.pop();
			return null;
		}

		while (!usesStack.isEmpty()) {

			UseWithScope useWS = usesStack.pop();
			if (MyConstants.DEBUG_INFO)
				System.out.println("POP from use stack: " + useWS.dump());

			// use-def analysis
			Stmt s = useWS.getUse();
			Stmt sScope = useWS.getScopeEnd();

			if (s.containsInvokeExpr()) {
				if (!s.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()) {
					AddTags(s, API_TAG);
				}
			}

			if (s instanceof DefinitionStmt) {
				boolean usesConstant = false;
				List<ValueBox> checkConstUseBoxes = s.getUseBoxes();
				for (ValueBox ccVB : checkConstUseBoxes) {
					if (ccVB.getValue() instanceof StringConstant) {
						if (!((StringConstant) ccVB.getValue()).value.equals("")) {
							usesConstant = true;
							break;
						}
					}
				}
				if (usesConstant) {
					AddTags(s, STRING_CONST_TAG);
				}
			}

			APIGraphNode sNode = CreateOrGetExistingNode(s, sMethod);
			;

			if (!methodToDDGMap.get(sMethod).contains(sNode)) {
				methodToDDGMap.get(sMethod).add(sNode);
			}

			if (s instanceof InvokeStmt) {

				if (s.getInvokeExpr().getMethod().getSignature().equals(flowSink)) {
					List<ValueBox> usesBoxes = s.getUseBoxes();
					Iterator usesIter = usesBoxes.iterator();
					while (usesIter.hasNext()) {
						ValueBox usesBox = (ValueBox) usesIter.next();
						if (usesBox.getValue() instanceof Local) {
							List<Unit> defs = mrd.getDefsOfAt((Local) usesBox.getValue(), s);
							for (Unit def : defs) {

								APIGraphNode defNode = CreateOrGetExistingNode((Stmt) def, sMethod);
								;

								sNode.addPred(defNode);
								defNode.addSucc(sNode);
								if (!methodToDDGMap.get(sMethod).contains(defNode)) {
									methodToDDGMap.get(sMethod).add(defNode);
								}

								UseWithScope defofuseWS = new UseWithScope((Stmt) def, s);
								if (!uses.containsKey(def)) {
									Vector<Stmt> scopes = new Vector<Stmt>();
									scopes.add(s);
									uses.put((Stmt) def, scopes);
									usesStack.push(defofuseWS);
								} else {
									Vector<Stmt> scopes = uses.get(def);
									if (!scopes.contains(s)) {
										scopes.add(s);
										usesStack.push(defofuseWS);
									}
								}
							}
						}
					}
				}

			} else {

				/*
				 * boolean isInvoke = false;
				 * 
				 * Iterator iUse = s.getUseBoxes().iterator(); while (iUse.hasNext()) { ValueBox
				 * vB = (ValueBox) iUse.next(); if (vB.getValue() instanceof InvokeExpr) {
				 * isInvoke = true; } }
				 */
				boolean isInvoke = s.containsInvokeExpr();

				// rhs is invoke, lhs is ret
				if (isInvoke) {

					if (s.getInvokeExpr().getMethod().getSignature().equals(flowSink)) {

					}

					if (s instanceof DefinitionStmt) {
						Value lhs = ((DefinitionStmt) s).getLeftOp();

						if (MyConstants.CONSIDER_REDEFINE && lhs.getType() instanceof RefLikeType) {

							if (MyConstants.DEBUG_INFO)
								System.out.println("#5:looking for redefine:" + s);

							Iterator itForRedefine = body.getUnits().iterator();
							while (itForRedefine.hasNext()) {
								Stmt stmt = (Stmt) itForRedefine.next();

								if (!isInScope(eug, stmt, sScope)) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " is NOT in scope[<--" + sScope + "]");
									continue;
								}

								boolean isStmtUsingS = false;
								List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
								for (ValueBox useBox : useBoxesofStmt) {
									if (useBox.getValue() instanceof Local) {
										if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt).contains(s)) {
											isStmtUsingS = true;
											break;
										}
									}
								}

								if (isStmtUsingS) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " IS using " + s);

									if (stmt.containsInvokeExpr()) {
										if (!stmt.getInvokeExpr().getMethod().getDeclaringClass()
												.isApplicationClass()) {
											AddTags(s, API_TAG);
										}
									}

									if (stmt instanceof DefinitionStmt) {
										// if(!stmt.containsInvokeExpr()){
										boolean usesConstant = false;
										List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
										for (ValueBox ccVB : checkConstUseBoxes) {
											if (ccVB.getValue() instanceof StringConstant) {
												if (!((StringConstant) ccVB.getValue()).value.equals("")) {
													usesConstant = true;
													break;
												}
											}
										}
										if (usesConstant) {
											AddTags(s, STRING_CONST_TAG);
										}
										// }
									}

									APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);
									;

									if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
										methodToDDGMap.get(sMethod).add(stmtNode);
									}

									APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope, sMethod);
									;

									if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
										methodToDDGMap.get(sMethod).add(sScopeNode);
									}

									sNode.removeSucc(sScopeNode);
									sScopeNode.removePred(sNode);

									sNode.addSucc(stmtNode);
									stmtNode.addPred(sNode);

									stmtNode.addSucc(sScopeNode);
									sScopeNode.addPred(stmtNode);

									if (stmt instanceof InvokeStmt) {

										Vector<Integer> taintVector = new Vector<Integer>();

										Iterator defIt2 = s.getDefBoxes().iterator();
										while (defIt2.hasNext()) {
											ValueBox vbox2 = (ValueBox) defIt2.next();
											if (vbox2.getValue() instanceof Local) {
												// System.out.println(vbox2.getValue());
												InvokeExpr invokeEx = stmt.getInvokeExpr();
												int argCount = invokeEx.getArgCount();
												for (int i = 0; i < argCount; i++) {
													if (invokeEx.getArg(i) == vbox2.getValue()) {
														taintVector.add(i);
													}
												}

												// for instance invoke, consider
												// this reference too.
												if (invokeEx instanceof InstanceInvokeExpr) {
													if (((InstanceInvokeExpr) invokeEx).getBase() == vbox2.getValue()) {
														taintVector.add(MyConstants.thisObject);
													}
												}
											}
										}

										Iterator targets = null;
										if (stmt.getInvokeExpr().getMethod().isConcrete()) {
											if (MyConstants.DEBUG_INFO)
												System.out.println(stmt + " calls CONCRETE method: "
														+ stmt.getInvokeExpr().getMethod());
											List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
											defaultTargets.add(stmt.getInvokeExpr().getMethod());
											targets = defaultTargets.iterator();
										} else {
											if (MyConstants.DEBUG_INFO)
												System.out.println(stmt + " calls NON-CONCRETE method: "
														+ stmt.getInvokeExpr().getMethod());
											targets = new Targets(this.callGraph.edgesOutOf(stmt));

											if (!targets.hasNext()) {
												if (MyConstants.DEBUG_INFO)
													System.out.println(
															stmt + " does NOT have a target. add a DEFAULT one");
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(stmt.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											}
										}

										if (targets == null) {
											continue;
										}

										while (targets.hasNext()) {
											SootMethod target = (SootMethod) targets.next();

											boolean noNewTaint = true;

											String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
													+ Integer.toHexString(System.identityHashCode(stmt));

											if (!propagationHistory.containsKey(propKey)) {
												noNewTaint = false;
												List<Integer> sinks = new ArrayList<Integer>();
												sinks.addAll(taintVector);
												propagationHistory.put(propKey, sinks);
											} else {
												List<Integer> sinks = propagationHistory.get(propKey);

												for (Integer taint : taintVector) {
													if (!sinks.contains(taint)) {
														noNewTaint = false;
														sinks.add(taint);
													}
												}
											}

											if (noNewTaint) {
												break;
											}

											if (MyConstants.DEBUG_INFO) {
												System.out.println("PROPAGATING from METHOD: " + sMethod);
												System.out.println("PROPAGATING from STATEMENT: " + stmt);
											}
											taintedFieldsInCaller.addAll(taintedFields);
											Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt,
													sMethod);
											for (SootField sf : taintedFieldsInCallee) {
												if (!taintedFields.contains(sf)) {
													taintedFields.add(sf);
												}
											}
											taintedFieldsInCallee.clear();

											if (MyConstants.DEBUG_INFO) {
												System.out.println(stmt + " |taint:" + taintVector
														+ "| PROPAGATION result: " + tainted);
											}
											if ((tainted != null) && (!tainted.isEmpty())) {
												for (Integer i : tainted) {
													int index = i.intValue();

													if (index == MyConstants.thisObject) {
														if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
															Value taintedThisRef = ((InstanceInvokeExpr) stmt
																	.getInvokeExpr()).getBase();

															if (taintedThisRef instanceof Local) {
																List<Unit> defs0 = mrd
																		.getDefsOfAt((Local) taintedThisRef, stmt);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	stmtNode.addPred(defNode);
																	defNode.addSucc(stmtNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			stmt);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(stmt);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(stmt))) {
																		uses.get(defn).add(stmt);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}

														}

													} else if (index >= 0) {

														Value taintedArg = stmt.getInvokeExpr().getArg(index);

														if (taintedArg instanceof Local) {
															List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg,
																	stmt);

															for (Unit defn : defs0) {

																APIGraphNode defNode = CreateOrGetExistingNode(
																		(Stmt) defn, sMethod);

																stmtNode.addPred(defNode);
																defNode.addSucc(stmtNode);
																if (!methodToDDGMap.get(sMethod).contains(defNode)) {
																	methodToDDGMap.get(sMethod).add(defNode);
																}

																UseWithScope defnWS = new UseWithScope((Stmt) defn,
																		stmt);
																if (!uses.containsKey(defn)) {
																	Vector<Stmt> scopes = new Vector<Stmt>();
																	scopes.add(stmt);
																	uses.put((Stmt) defn, scopes);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																} else if (!(uses.get(defn).contains(stmt))) {
																	uses.get(defn).add(stmt);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																}
															}
														}
													}
												}
											}

										}

									} else if (stmt instanceof DefinitionStmt) {

										Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
										if (rhsInvoke instanceof InvokeExpr) {

											Vector<Integer> taintVector = new Vector<Integer>();

											Iterator defIt2 = s.getDefBoxes().iterator();
											while (defIt2.hasNext()) {
												ValueBox vbox2 = (ValueBox) defIt2.next();
												if (vbox2.getValue() instanceof Local) {
													// System.out.println(vbox2.getValue());
													InvokeExpr invokeEx = stmt.getInvokeExpr();
													int argCount = invokeEx.getArgCount();
													for (int i = 0; i < argCount; i++) {
														if (invokeEx.getArg(i) == vbox2.getValue()) {
															taintVector.add(i);
														}
													}

													// for instance invoke,
													// consider this reference
													// too.
													if (invokeEx instanceof InstanceInvokeExpr) {
														if (((InstanceInvokeExpr) invokeEx).getBase() == vbox2
																.getValue()) {
															taintVector.add(MyConstants.thisObject);
														}
													}
												}
											}

											Iterator targets = null;
											if (stmt.getInvokeExpr().getMethod().isConcrete()) {
												if (MyConstants.DEBUG_INFO)
													System.out.println(stmt + " calls CONCRETE method: "
															+ stmt.getInvokeExpr().getMethod());
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(stmt.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											} else {
												if (MyConstants.DEBUG_INFO)
													System.out.println(stmt + " calls NON-CONCRETE method: "
															+ stmt.getInvokeExpr().getMethod());
												targets = new Targets(this.callGraph.edgesOutOf(stmt));

												if (!targets.hasNext()) {
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																stmt + " does NOT have a target. add a DEFAULT one");
													List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
													defaultTargets.add(stmt.getInvokeExpr().getMethod());
													targets = defaultTargets.iterator();
												}
											}

											if (targets == null) {
												continue;
											}

											while (targets.hasNext()) {
												SootMethod target = (SootMethod) targets.next();

												boolean noNewTaint = true;

												String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
														+ Integer.toHexString(System.identityHashCode(stmt));

												if (!propagationHistory.containsKey(propKey)) {
													noNewTaint = false;
													List<Integer> sinks = new ArrayList<Integer>();
													sinks.addAll(taintVector);
													propagationHistory.put(propKey, sinks);
												} else {
													List<Integer> sinks = propagationHistory.get(propKey);

													for (Integer taint : taintVector) {
														if (!sinks.contains(taint)) {
															noNewTaint = false;
															sinks.add(taint);
														}
													}
												}

												if (noNewTaint) {
													break;
												}

												if (MyConstants.DEBUG_INFO) {
													System.out.println("PROPAGATING from METHOD: " + sMethod);
													System.out.println("PROPAGATING from STATEMENT: " + stmt);
												}
												taintedFieldsInCaller.addAll(taintedFields);
												Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt,
														sMethod);
												for (SootField sf : taintedFieldsInCallee) {
													if (!taintedFields.contains(sf)) {
														taintedFields.add(sf);
													}
												}
												taintedFieldsInCallee.clear();

												if (MyConstants.DEBUG_INFO) {
													System.out.println(stmt + " |taint:" + taintVector
															+ "| PROPAGATION result: " + tainted);
												}
												if ((tainted != null) && (!tainted.isEmpty())) {
													for (Integer i : tainted) {
														int index = i.intValue();

														if (index == MyConstants.thisObject) {
															if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																Value taintedThisRef = ((InstanceInvokeExpr) stmt
																		.getInvokeExpr()).getBase();

																if (taintedThisRef instanceof Local) {
																	List<Unit> defs0 = mrd
																			.getDefsOfAt((Local) taintedThisRef, stmt);

																	for (Unit defn : defs0) {

																		APIGraphNode defNode = CreateOrGetExistingNode(
																				(Stmt) defn, sMethod);

																		stmtNode.addPred(defNode);
																		defNode.addSucc(stmtNode);
																		if (!methodToDDGMap.get(sMethod)
																				.contains(defNode)) {
																			methodToDDGMap.get(sMethod).add(defNode);
																		}

																		UseWithScope defnWS = new UseWithScope(
																				(Stmt) defn, stmt);
																		if (!uses.containsKey(defn)) {
																			Vector<Stmt> scopes = new Vector<Stmt>();
																			scopes.add(stmt);
																			uses.put((Stmt) defn, scopes);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		} else if (!(uses.get(defn).contains(stmt))) {
																			uses.get(defn).add(stmt);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		}
																	}
																}

															}

														} else if (index >= 0) {

															Value taintedArg = stmt.getInvokeExpr().getArg(index);

															if (taintedArg instanceof Local) {
																List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg,
																		stmt);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	stmtNode.addPred(defNode);
																	defNode.addSucc(stmtNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			stmt);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(stmt);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(stmt))) {
																		uses.get(defn).add(stmt);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}
														}
													}
												}

											}
										}
									}

								} // isStmtUsingS
							}
						} // if(lhs.getType() instanceof RefLikeType){
					}

					Vector<Integer> taintVector = new Vector<Integer>();
					taintVector.add(MyConstants.returnValue);

					Iterator targets = null;
					if (s.getInvokeExpr().getMethod().isConcrete()) {
						if (MyConstants.DEBUG_INFO)
							System.out.println(s + " calls CONCRETE method: " + s.getInvokeExpr().getMethod());
						List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
						defaultTargets.add(s.getInvokeExpr().getMethod());
						targets = defaultTargets.iterator();
					} else {
						if (MyConstants.DEBUG_INFO)
							System.out.println(s + " calls NON-CONCRETE method: " + s.getInvokeExpr().getMethod());
						targets = new Targets(this.callGraph.edgesOutOf(s));

						if (!targets.hasNext()) {
							if (MyConstants.DEBUG_INFO)
								System.out.println(s + " does NOT have a target. add a DEFAULT one");
							List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
							defaultTargets.add(s.getInvokeExpr().getMethod());
							targets = defaultTargets.iterator();
						}
					}

					if (targets == null) {
						continue;
					}

					while (targets.hasNext()) {
						SootMethod target = (SootMethod) targets.next();

						boolean noNewTaint = true;

						String propKey = sMethod.getSignature() + "|" + s.toString() + "|"
								+ Integer.toHexString(System.identityHashCode(s));

						if (!propagationHistory.containsKey(propKey)) {
							noNewTaint = false;
							List<Integer> sinks = new ArrayList<Integer>();
							sinks.addAll(taintVector);
							propagationHistory.put(propKey, sinks);
						} else {
							List<Integer> sinks = propagationHistory.get(propKey);

							for (Integer taint : taintVector) {
								if (!sinks.contains(taint)) {
									noNewTaint = false;
									sinks.add(taint);
								}
							}
						}

						if (noNewTaint) {
							break;
						}

						if (MyConstants.DEBUG_INFO) {
							System.out.println("PROPAGATING from METHOD: " + sMethod);
							System.out.println("PROPAGATING from STATEMENT: " + s);
						}
						taintedFieldsInCaller.addAll(taintedFields);
						Vector<Integer> tainted = propagate(target, taintVector, flowSink, s, sMethod);
						for (SootField sf : taintedFieldsInCallee) {
							if (!taintedFields.contains(sf)) {
								taintedFields.add(sf);
							}
						}
						taintedFieldsInCallee.clear();

						if (MyConstants.DEBUG_INFO) {
							System.out.println(s + " |taint:" + taintVector + "| PROPAGATION result: " + tainted);
						}
						if ((tainted != null) && (!tainted.isEmpty())) {
							for (Integer i : tainted) {
								int index = i.intValue();

								if (index == MyConstants.thisObject) {
									if (s.getInvokeExpr() instanceof InstanceInvokeExpr) {
										Value taintedThisRef = ((InstanceInvokeExpr) s.getInvokeExpr()).getBase();

										if (taintedThisRef instanceof Local) {
											List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedThisRef, s);

											for (Unit defn : defs0) {

												APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn, sMethod);
												;

												sNode.addPred(defNode);
												defNode.addSucc(sNode);
												if (!methodToDDGMap.get(sMethod).contains(defNode)) {
													methodToDDGMap.get(sMethod).add(defNode);
												}

												UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
												if (!uses.containsKey(defn)) {
													Vector<Stmt> scopes = new Vector<Stmt>();
													scopes.add(s);
													uses.put((Stmt) defn, scopes);
													usesStack.push(defnWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println("use stack doesn't contain " + defnWS.dump()
																+ ". Push it.");
												} else if (!(uses.get(defn).contains(s))) {
													uses.get(defn).add(s);
													usesStack.push(defnWS);
													if (MyConstants.DEBUG_INFO)
														System.out.println("use stack doesn't contain " + defnWS.dump()
																+ ". Push it.");
												}
											}
										}

									}

								} else if (index >= 0) {

									Value taintedArg = s.getInvokeExpr().getArg(index);

									if (taintedArg instanceof Local) {
										List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg, s);

										for (Unit defn : defs0) {

											APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn, sMethod);
											;

											sNode.addPred(defNode);
											defNode.addSucc(sNode);
											if (!methodToDDGMap.get(sMethod).contains(defNode)) {
												methodToDDGMap.get(sMethod).add(defNode);
											}

											UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
											if (!uses.containsKey(defn)) {
												Vector<Stmt> scopes = new Vector<Stmt>();
												scopes.add(s);
												uses.put((Stmt) defn, scopes);
												usesStack.push(defnWS);
												if (MyConstants.DEBUG_INFO)
													System.out.println("use stack doesn't contain " + defnWS.dump()
															+ ". Push it.");
											} else if (!(uses.get(defn).contains(s))) {
												uses.get(defn).add(s);
												usesStack.push(defnWS);
												if (MyConstants.DEBUG_INFO)
													System.out.println("use stack doesn't contain " + defnWS.dump()
															+ ". Push it.");
											}
										}
									}
								}
							}
						}

					}
					// invokes.add(s);
				}

				// pure definiton statement:
				else {

					if (s instanceof DefinitionStmt) {
						Value rhs = ((DefinitionStmt) s).getRightOp();
						Value lhs = ((DefinitionStmt) s).getLeftOp();

						// if lhs is a reference
						if (MyConstants.CONSIDER_REDEFINE && lhs.getType() instanceof RefLikeType) {
							// look for implicit redefine. e.g., a reference is
							// passed into a method and thus its value is
							// changed in callee.
							if (MyConstants.DEBUG_INFO)
								System.out.println("#6:looking for redefine:" + s);

							Iterator itForRedefine = body.getUnits().iterator();
							while (itForRedefine.hasNext()) {
								Stmt stmt = (Stmt) itForRedefine.next();

								if (!isInScope(eug, stmt, sScope)) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " is NOT in scope[<--" + sScope + "]");
									continue;
								}

								boolean isStmtUsingS = false;
								List<ValueBox> useBoxesofStmt = stmt.getUseBoxes();
								for (ValueBox useBox : useBoxesofStmt) {
									if (useBox.getValue() instanceof Local) {
										if (mrd.getDefsOfAt((Local) (useBox.getValue()), stmt).contains(s)) {
											isStmtUsingS = true;
											break;
										}
									}
								}

								if (isStmtUsingS) {
									if (MyConstants.DEBUG_INFO)
										System.out.println(stmt + " IS using " + s);

									if (stmt.containsInvokeExpr()) {
										if (!stmt.getInvokeExpr().getMethod().getDeclaringClass()
												.isApplicationClass()) {
											AddTags(stmt, API_TAG);
										}
									}

									if (stmt instanceof DefinitionStmt) {
										// if(!stmt.containsInvokeExpr()){
										boolean usesConstant = false;
										List<ValueBox> checkConstUseBoxes = stmt.getUseBoxes();
										for (ValueBox ccVB : checkConstUseBoxes) {
											if (ccVB.getValue() instanceof StringConstant) {
												if (!((StringConstant) ccVB.getValue()).value.equals("")) {
													usesConstant = true;
													break;
												}
											}
										}
										if (usesConstant) {
											AddTags(stmt, STRING_CONST_TAG);
										}
										// }
									}

									APIGraphNode stmtNode = CreateOrGetExistingNode(stmt, sMethod);
									;

									if (!methodToDDGMap.get(sMethod).contains(stmtNode)) {
										methodToDDGMap.get(sMethod).add(stmtNode);
									}

									APIGraphNode sScopeNode = CreateOrGetExistingNode(sScope, sMethod);
									;

									if (!methodToDDGMap.get(sMethod).contains(sScopeNode)) {
										methodToDDGMap.get(sMethod).add(sScopeNode);
									}

									sNode.removeSucc(sScopeNode);
									sScopeNode.removePred(sNode);

									sNode.addSucc(stmtNode);
									stmtNode.addPred(sNode);

									stmtNode.addSucc(sScopeNode);
									sScopeNode.addPred(stmtNode);

									if (stmt instanceof InvokeStmt) {

										Vector<Integer> taintVector = new Vector<Integer>();

										Iterator defIt2 = s.getDefBoxes().iterator();
										while (defIt2.hasNext()) {
											ValueBox vbox2 = (ValueBox) defIt2.next();
											if (vbox2.getValue() instanceof Local) {
												// System.out.println(vbox2.getValue());
												InvokeExpr invokeEx = stmt.getInvokeExpr();
												int argCount = invokeEx.getArgCount();
												for (int i = 0; i < argCount; i++) {
													if (invokeEx.getArg(i) == vbox2.getValue()) {
														taintVector.add(i);
													}
												}

												// for instance invoke, consider
												// this reference too.
												if (invokeEx instanceof InstanceInvokeExpr) {
													if (((InstanceInvokeExpr) invokeEx).getBase() == vbox2.getValue()) {
														taintVector.add(MyConstants.thisObject);
													}
												}
											}
										}

										Iterator targets = null;
										if (stmt.getInvokeExpr().getMethod().isConcrete()) {
											if (MyConstants.DEBUG_INFO)
												System.out.println(stmt + " calls CONCRETE method: "
														+ stmt.getInvokeExpr().getMethod());
											List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
											defaultTargets.add(stmt.getInvokeExpr().getMethod());
											targets = defaultTargets.iterator();
										} else {
											if (MyConstants.DEBUG_INFO)
												System.out.println(stmt + " calls NON-CONCRETE method: "
														+ stmt.getInvokeExpr().getMethod());
											targets = new Targets(this.callGraph.edgesOutOf(stmt));

											if (!targets.hasNext()) {
												if (MyConstants.DEBUG_INFO)
													System.out.println(
															stmt + " does NOT have a target. add a DEFAULT one");
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(stmt.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											}
										}

										if (targets == null) {
											continue;
										}

										while (targets.hasNext()) {
											SootMethod target = (SootMethod) targets.next();

											boolean noNewTaint = true;

											String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
													+ Integer.toHexString(System.identityHashCode(stmt));

											if (!propagationHistory.containsKey(propKey)) {
												noNewTaint = false;
												List<Integer> sinks = new ArrayList<Integer>();
												sinks.addAll(taintVector);
												propagationHistory.put(propKey, sinks);
											} else {
												List<Integer> sinks = propagationHistory.get(propKey);

												for (Integer taint : taintVector) {
													if (!sinks.contains(taint)) {
														noNewTaint = false;
														sinks.add(taint);
													}
												}
											}

											if (noNewTaint) {
												break;
											}

											if (MyConstants.DEBUG_INFO) {
												System.out.println("PROPAGATING from METHOD: " + sMethod);
												System.out.println("PROPAGATING from STATEMENT: " + stmt);
											}
											taintedFieldsInCaller.addAll(taintedFields);
											Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt,
													sMethod);
											for (SootField sf : taintedFieldsInCallee) {
												if (!taintedFields.contains(sf)) {
													taintedFields.add(sf);
												}
											}
											taintedFieldsInCallee.clear();

											if (MyConstants.DEBUG_INFO) {
												System.out.println(stmt + " |taint:" + taintVector
														+ "| PROPAGATION result: " + tainted);
											}
											if ((tainted != null) && (!tainted.isEmpty())) {
												for (Integer i : tainted) {
													int index = i.intValue();

													if (index == MyConstants.thisObject) {
														if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
															Value taintedThisRef = ((InstanceInvokeExpr) stmt
																	.getInvokeExpr()).getBase();

															if (taintedThisRef instanceof Local) {
																List<Unit> defs0 = mrd
																		.getDefsOfAt((Local) taintedThisRef, stmt);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	stmtNode.addPred(defNode);
																	defNode.addSucc(stmtNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			stmt);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(stmt);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(stmt))) {
																		uses.get(defn).add(stmt);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}

														}

													} else if (index >= 0) {

														Value taintedArg = stmt.getInvokeExpr().getArg(index);

														if (taintedArg instanceof Local) {
															List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg,
																	stmt);

															for (Unit defn : defs0) {

																APIGraphNode defNode = CreateOrGetExistingNode(
																		(Stmt) defn, sMethod);

																stmtNode.addPred(defNode);
																defNode.addSucc(stmtNode);
																if (!methodToDDGMap.get(sMethod).contains(defNode)) {
																	methodToDDGMap.get(sMethod).add(defNode);
																}

																UseWithScope defnWS = new UseWithScope((Stmt) defn,
																		stmt);
																if (!uses.containsKey(defn)) {
																	Vector<Stmt> scopes = new Vector<Stmt>();
																	scopes.add(stmt);
																	uses.put((Stmt) defn, scopes);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																} else if (!(uses.get(defn).contains(stmt))) {
																	uses.get(defn).add(stmt);
																	usesStack.push(defnWS);
																	if (MyConstants.DEBUG_INFO)
																		System.out.println("use stack doesn't contain "
																				+ defnWS.dump() + ". Push it.");
																}
															}
														}
													}
												}
											}

										}

									} else if (stmt instanceof DefinitionStmt) {

										Value rhsInvoke = ((DefinitionStmt) stmt).getRightOp();
										if (rhsInvoke instanceof InvokeExpr) {

											Vector<Integer> taintVector = new Vector<Integer>();

											Iterator defIt2 = s.getDefBoxes().iterator();
											while (defIt2.hasNext()) {
												ValueBox vbox2 = (ValueBox) defIt2.next();
												if (vbox2.getValue() instanceof Local) {
													// System.out.println(vbox2.getValue());
													InvokeExpr invokeEx = stmt.getInvokeExpr();
													int argCount = invokeEx.getArgCount();
													for (int i = 0; i < argCount; i++) {
														if (invokeEx.getArg(i) == vbox2.getValue()) {
															taintVector.add(i);
														}
													}

													// for instance invoke,
													// consider this reference
													// too.
													if (invokeEx instanceof InstanceInvokeExpr) {
														if (((InstanceInvokeExpr) invokeEx).getBase() == vbox2
																.getValue()) {
															taintVector.add(MyConstants.thisObject);
														}
													}
												}
											}

											Iterator targets = null;
											if (stmt.getInvokeExpr().getMethod().isConcrete()) {
												if (MyConstants.DEBUG_INFO)
													System.out.println(stmt + " calls CONCRETE method: "
															+ stmt.getInvokeExpr().getMethod());
												List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
												defaultTargets.add(stmt.getInvokeExpr().getMethod());
												targets = defaultTargets.iterator();
											} else {
												if (MyConstants.DEBUG_INFO)
													System.out.println(stmt + " calls NON-CONCRETE method: "
															+ stmt.getInvokeExpr().getMethod());
												targets = new Targets(this.callGraph.edgesOutOf(stmt));

												if (!targets.hasNext()) {
													if (MyConstants.DEBUG_INFO)
														System.out.println(
																stmt + " does NOT have a target. add a DEFAULT one");
													List<SootMethod> defaultTargets = new ArrayList<SootMethod>();
													defaultTargets.add(stmt.getInvokeExpr().getMethod());
													targets = defaultTargets.iterator();
												}
											}

											if (targets == null) {
												continue;
											}

											while (targets.hasNext()) {
												SootMethod target = (SootMethod) targets.next();

												boolean noNewTaint = true;

												String propKey = sMethod.getSignature() + "|" + stmt.toString() + "|"
														+ Integer.toHexString(System.identityHashCode(stmt));

												if (!propagationHistory.containsKey(propKey)) {
													noNewTaint = false;
													List<Integer> sinks = new ArrayList<Integer>();
													sinks.addAll(taintVector);
													propagationHistory.put(propKey, sinks);
												} else {
													List<Integer> sinks = propagationHistory.get(propKey);

													for (Integer taint : taintVector) {
														if (!sinks.contains(taint)) {
															noNewTaint = false;
															sinks.add(taint);
														}
													}
												}

												if (noNewTaint) {
													break;
												}

												if (MyConstants.DEBUG_INFO) {
													System.out.println("PROPAGATING from METHOD: " + sMethod);
													System.out.println("PROPAGATING from STATEMENT: " + stmt);
												}
												taintedFieldsInCaller.addAll(taintedFields);
												Vector<Integer> tainted = propagate(target, taintVector, flowSink, stmt,
														sMethod);
												for (SootField sf : taintedFieldsInCallee) {
													if (!taintedFields.contains(sf)) {
														taintedFields.add(sf);
													}
												}
												taintedFieldsInCallee.clear();

												if (MyConstants.DEBUG_INFO) {
													System.out.println(stmt + " |taint:" + taintVector
															+ "| PROPAGATION result: " + tainted);
												}
												if ((tainted != null) && (!tainted.isEmpty())) {

													for (Integer i : tainted) {
														int index = i.intValue();

														if (index == MyConstants.thisObject) {
															if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
																Value taintedThisRef = ((InstanceInvokeExpr) stmt
																		.getInvokeExpr()).getBase();

																if (taintedThisRef instanceof Local) {
																	List<Unit> defs0 = mrd
																			.getDefsOfAt((Local) taintedThisRef, stmt);

																	for (Unit defn : defs0) {

																		APIGraphNode defNode = CreateOrGetExistingNode(
																				(Stmt) defn, sMethod);

																		stmtNode.addPred(defNode);
																		defNode.addSucc(stmtNode);
																		if (!methodToDDGMap.get(sMethod)
																				.contains(defNode)) {
																			methodToDDGMap.get(sMethod).add(defNode);
																		}

																		UseWithScope defnWS = new UseWithScope(
																				(Stmt) defn, stmt);
																		if (!uses.containsKey(defn)) {
																			Vector<Stmt> scopes = new Vector<Stmt>();
																			scopes.add(stmt);
																			uses.put((Stmt) defn, scopes);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		} else if (!(uses.get(defn).contains(stmt))) {
																			uses.get(defn).add(stmt);
																			usesStack.push(defnWS);
																			if (MyConstants.DEBUG_INFO)
																				System.out.println(
																						"use stack doesn't contain "
																								+ defnWS.dump()
																								+ ". Push it.");
																		}
																	}
																}

															}

														} else if (index >= 0) {

															Value taintedArg = stmt.getInvokeExpr().getArg(index);

															if (taintedArg instanceof Local) {
																List<Unit> defs0 = mrd.getDefsOfAt((Local) taintedArg,
																		stmt);

																for (Unit defn : defs0) {

																	APIGraphNode defNode = CreateOrGetExistingNode(
																			(Stmt) defn, sMethod);

																	stmtNode.addPred(defNode);
																	defNode.addSucc(stmtNode);
																	if (!methodToDDGMap.get(sMethod)
																			.contains(defNode)) {
																		methodToDDGMap.get(sMethod).add(defNode);
																	}

																	UseWithScope defnWS = new UseWithScope((Stmt) defn,
																			stmt);
																	if (!uses.containsKey(defn)) {
																		Vector<Stmt> scopes = new Vector<Stmt>();
																		scopes.add(stmt);
																		uses.put((Stmt) defn, scopes);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	} else if (!(uses.get(defn).contains(stmt))) {
																		uses.get(defn).add(stmt);
																		usesStack.push(defnWS);
																		if (MyConstants.DEBUG_INFO)
																			System.out.println(
																					"use stack doesn't contain "
																							+ defnWS.dump()
																							+ ". Push it.");
																	}
																}
															}
														}
													}
												}

											}
										}
									}

								} // isStmtUsingS
							}
						} // if(lhs.getType() instanceof RefLikeType){

						if (rhs instanceof InstanceFieldRef) {
							if (MyConstants.TO_TAINT_INSTANCE_FIELD) {
								if (!taintedFields.contains(((InstanceFieldRef) rhs).getField())) {
									if (MyConstants.DEBUG_INFO)
										System.out.println("adding new field as source: "
												+ ((InstanceFieldRef) rhs).getField() + " from: " + s);
									taintedFields.add(((InstanceFieldRef) rhs).getField());
								}

								SootField fieldKey = ((InstanceFieldRef) rhs).getField();
								if (fieldToUsesMap.containsKey(fieldKey)) {
									List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
									if (!fieldUses.contains(s)) {
										fieldUses.add(s);
									}
								} else {
									List<Stmt> fieldUses = new ArrayList<Stmt>();
									fieldToUsesMap.put(fieldKey, fieldUses);
									fieldUses.add(s);
								}
							}
						} else if (rhs instanceof StaticFieldRef) {
							if (MyConstants.TO_TAINT_STATIC_FIELD) {
								if (!taintedFields.contains(((StaticFieldRef) rhs).getField())) {
									if (MyConstants.DEBUG_INFO)
										System.out.println("adding new field as source: "
												+ ((StaticFieldRef) rhs).getField() + " from: " + s);
									taintedFields.add(((StaticFieldRef) rhs).getField());
								}

								SootField fieldKey = ((StaticFieldRef) rhs).getField();
								if (fieldToUsesMap.containsKey(fieldKey)) {
									List<Stmt> fieldUses = fieldToUsesMap.get(fieldKey);
									if (!fieldUses.contains(s)) {
										fieldUses.add(s);
									}
								} else {
									List<Stmt> fieldUses = new ArrayList<Stmt>();
									fieldToUsesMap.put(fieldKey, fieldUses);
									fieldUses.add(s);
								}
							}
						}

						Iterator<ValueBox> sUseIter = s.getUseBoxes().iterator();
						while (sUseIter.hasNext()) {
							Value v = sUseIter.next().getValue();
							if (v instanceof Local) {

								List<Unit> defs = mrd.getDefsOfAt((Local) v, s);

								for (Unit defn : defs) {

									APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn, sMethod);
									;

									sNode.addPred(defNode);
									defNode.addSucc(sNode);
									if (!methodToDDGMap.get(sMethod).contains(defNode)) {
										methodToDDGMap.get(sMethod).add(defNode);
									}

									UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
									if (!uses.containsKey(defn)) {
										Vector<Stmt> scopes = new Vector<Stmt>();
										scopes.add(s);
										uses.put((Stmt) defn, scopes);
										usesStack.push(defnWS);
										if (MyConstants.DEBUG_INFO)
											System.out.println(
													"use stack doesn't contain " + defnWS.dump() + ". Push it.");
									} else if (!(uses.get(defn).contains(s))) {
										uses.get(defn).add(s);
										usesStack.push(defnWS);
										if (MyConstants.DEBUG_INFO)
											System.out.println(
													"use stack doesn't contain " + defnWS.dump() + ". Push it.");
									}
								}
							}
						}

					} // if(s instanceof DefinitionStmt){
					else if (s instanceof ReturnStmt) {
						Value op = ((ReturnStmt) s).getOp();

						if (op instanceof Local) {

							List<Unit> defs = mrd.getDefsOfAt((Local) op, s);

							for (Unit defn : defs) {

								APIGraphNode defNode = CreateOrGetExistingNode((Stmt) defn, sMethod);
								;

								sNode.addPred(defNode);
								defNode.addSucc(sNode);
								if (!methodToDDGMap.get(sMethod).contains(defNode)) {
									methodToDDGMap.get(sMethod).add(defNode);
								}

								UseWithScope defnWS = new UseWithScope((Stmt) defn, s);
								if (!uses.containsKey(defn)) {
									Vector<Stmt> scopes = new Vector<Stmt>();
									scopes.add(s);
									uses.put((Stmt) defn, scopes);
									usesStack.push(defnWS);
									if (MyConstants.DEBUG_INFO)
										System.out.println("use stack doesn't contain " + defnWS.dump() + ". Push it.");
								} else if (!(uses.get(defn).contains(s))) {
									uses.get(defn).add(s);
									usesStack.push(defnWS);
									if (MyConstants.DEBUG_INFO)
										System.out.println("use stack doesn't contain " + defnWS.dump() + ". Push it.");
								}
							}

						} else if (op instanceof Constant) {

						}
					}

				}
			}
		} // while(!usesStack.isEmpty()){

		//////////////////////////
		Iterator i = uses.keySet().iterator();
		while (i.hasNext()) {
			Stmt s = (Stmt) i.next();

			AddTags(s, taintTag);
			Iterator usesIt = s.getUseBoxes().iterator();
			while (usesIt.hasNext()) {
				ValueBox vbox = (ValueBox) usesIt.next();
				if (vbox.getValue() instanceof Local) {
					Local l = (Local) vbox.getValue();

					Iterator rDefsIt = mrd.getDefsOfAt(l, s).iterator();
					while (rDefsIt.hasNext()) {
						Stmt next = (Stmt) rDefsIt.next();
						if (!next.getTags().contains(taintTag)) {
							AddTags(next, extraDefTag);
						}
					}
				}
			}

			if (s instanceof IdentityStmt) {

				APIGraphNode idNode = CreateOrGetExistingNode(s, sMethod);
				;

				if (!methodToDDGMap.get(sMethod).contains(idNode)) {
					methodToDDGMap.get(sMethod).add(idNode);
				}

				APIGraphNode fromNode = CreateOrGetExistingNode(from, fromMethod);
				;

				if (!methodToDDGMap.get(fromMethod).contains(fromNode)) {
					methodToDDGMap.get(fromMethod).add(fromNode);
				}

				idNode.addPred(fromNode);
				fromNode.addSucc(idNode);

				Value rhsIdentity = ((IdentityStmt) s).getRightOp();
				if (rhsIdentity instanceof ThisRef) {
					if (!taintIndexes.contains(MyConstants.thisObject)) {
						taintResult.add(MyConstants.thisObject);
					}
				} else if (rhsIdentity instanceof ParameterRef) {
					int index = ((ParameterRef) rhsIdentity).getIndex();
					if (!taintIndexes.contains(index)) {
						taintResult.add(index);
					}
				}
			}
		}

		if (MyConstants.DEBUG_INFO) {
			System.out.println();
			System.out.println("method:" + sMethod.getSignature());
			System.out.println("dataflow for " + sink + ":");
		}

		Iterator printIt = body.getUnits().iterator();
		while (printIt.hasNext()) {
			Stmt s = (Stmt) printIt.next();
			if (s.getTags().contains(taintTag) || s.getTags().contains(extraDefTag)) {
				if (MyConstants.DEBUG_INFO) {
					dumpTaggedStmt(s);
				}

				Vector<Integer> labels = new Vector<Integer>();

				for (Tag tag : s.getTags()) {
					if (taintTagReverseMap.containsKey(tag)) {
						Integer label = ((TaintTag) tag).getLabel();
						if (!labels.contains(label)) {
							labels.add(label);
						}
					} else if (extraDefTagReverseMap.containsKey(tag)) {
						Integer label = ((TaintTag) tag).getLabel();
						if (!labels.contains(label)) {
							labels.add(label);
						}
					}
				}

				List<ValueBox> vbs = s.getUseAndDefBoxes();
				Iterator iter = vbs.iterator();
				while (iter.hasNext()) {
					ValueBox vb = (ValueBox) iter.next();
					if (vb.getValue() instanceof InstanceFieldRef) {
						SootField instanceField = ((InstanceFieldRef) vb.getValue()).getField();

						if (instanceField.getDeclaringClass().isApplicationClass() == false) {
							continue;
						}

						// if(!instanceFields.contains(instanceField)){
						// instanceFields.add(instanceField);
						// }

						////
						if (!instanceFieldMap.containsKey(instanceField)) {

							Vector<Integer> taintSources = new Vector<Integer>();
							taintSources.addAll(labels);
							instanceFieldMap.put(instanceField, taintSources);

						} else {

							Vector<Integer> taintSources = instanceFieldMap.get(instanceField);
							for (Integer label : labels) {
								if (!taintSources.contains(label)) {
									taintSources.add(label);
								}
							}
						}
						////

						LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
						List<String> fieldList = new ArrayList<String>();
						if (fieldList.contains(instanceField.getSignature())) {
							fieldList.add(instanceField.getSignature());
						}
						taintSourceToField.put(flowSink, fieldList);

					} else if (vb.getValue() instanceof StaticFieldRef) {
						SootField staticField = ((StaticFieldRef) vb.getValue()).getField();

						if (staticField.getDeclaringClass().isApplicationClass() == false) {
							continue;
						}

						// if(!staticFields.contains(staticField)){
						// staticFields.add(staticField);
						// }

						///
						if (!staticFieldMap.containsKey(staticField)) {

							Vector<Integer> taintSources = new Vector<Integer>();
							taintSources.addAll(labels);
							staticFieldMap.put(staticField, taintSources);

						} else {

							Vector<Integer> taintSources = staticFieldMap.get(staticField);
							for (Integer label : labels) {
								if (!taintSources.contains(label)) {
									taintSources.add(label);
								}
							}
						}
						///

						LinkedHashMap<String, List<String>> taintSourceToField = new LinkedHashMap<String, List<String>>();
						List<String> fieldList = new ArrayList<String>();
						if (fieldList.contains(staticField.getSignature())) {
							fieldList.add(staticField.getSignature());
						}
						taintSourceToField.put(flowSink, fieldList);

					} else if (vb.getValue() instanceof Local) {

						String varName = ((Local) vb.getValue()).getName();
						LinkedHashMap<String, List<String>> taintSourceToVar = new LinkedHashMap<String, List<String>>();
						List<String> varList = new ArrayList<String>();
						if (varList.contains(varName)) {
							varList.add(varName);
						}
						taintSourceToVar.put(flowSink, varList);
					}
				}
			}
		}

		if (MyConstants.DEBUG_INFO) {
			System.out.println("end dataflow for " + sink + "\n");
		}

		taintedFieldsInCallee.addAll(taintedFields);

		Set<SootField> instanceKeySet = instanceFieldMap.keySet();
		Iterator<SootField> instanceIter = instanceKeySet.iterator();
		while (instanceIter.hasNext()) {
			SootField f = instanceIter.next();
			Vector<Integer> newLabels = instanceFieldMap.get(f);

			if (usedInstanceFieldMap.containsKey(f)) {
				Vector<Integer> oldLabels = usedInstanceFieldMap.get(f);
				for (Integer label : newLabels) {
					if (!oldLabels.contains(label)) {
						oldLabels.add(label);
					}
				}
			} else {
				Vector<Integer> labels = new Vector<Integer>();
				labels.addAll(newLabels);
				usedInstanceFieldMap.put(f, labels);
			}
		}

		Set<SootField> staticKeySet = staticFieldMap.keySet();
		Iterator<SootField> staticIter = staticKeySet.iterator();
		while (staticIter.hasNext()) {
			SootField f = staticIter.next();
			Vector<Integer> newLabels = staticFieldMap.get(f);

			if (usedStaticFieldMap.containsKey(f)) {
				Vector<Integer> oldLabels = usedStaticFieldMap.get(f);
				for (Integer label : newLabels) {
					if (!oldLabels.contains(label)) {
						oldLabels.add(label);
					}
				}
			} else {
				Vector<Integer> labels = new Vector<Integer>();
				labels.addAll(newLabels);
				usedStaticFieldMap.put(f, labels);
			}
		}

		callString.pop();

		return taintResult;
	}

	private boolean isInScope(ExceptionalUnitGraph eug, Stmt toTest, Stmt scopeEnd) {
		if (scopeEnd == null) {
			return true;
		}

		if (toTest == scopeEnd) {
			return false;
		}

		Stack<Stmt> predecessors = new Stack<Stmt>();
		Vector<Stmt> traversedPreds = new Vector<Stmt>();

		predecessors.push(scopeEnd);
		traversedPreds.add(scopeEnd);

		while (!predecessors.isEmpty()) {
			Stmt predecessor = predecessors.pop();

			if (predecessor == toTest) {
				return true;
			}
			List<Unit> predsOfPredecessor = eug.getPredsOf(predecessor);
			for (Unit u : predsOfPredecessor) {
				Stmt s = (Stmt) u;
				if (!traversedPreds.contains(s)) {
					traversedPreds.add(s);
					predecessors.push(s);
				}
			}
		}

		return false;
	}
}