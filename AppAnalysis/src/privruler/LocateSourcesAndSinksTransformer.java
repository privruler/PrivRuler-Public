package privruler;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import dfa.MyConstants;
import dfa.util.Log;
import soot.Body;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;

public class LocateSourcesAndSinksTransformer extends BodyTransformer {
	public Vector<String> SRCS = new Vector<String>();
	public Vector<String> SINKS = new Vector<String>();

	private LinkedHashMap<String, LinkedHashMap<String, String>> sinksLocationMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
	private LinkedHashMap<String, LinkedHashMap<String, String>> sourcesLocationMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();

	public LocateSourcesAndSinksTransformer(Collection<String> sources, Collection<String> sinks) {
		SRCS.addAll(sources);
		SINKS.addAll(sinks);
		Log.dumpln("[All sources] in " + SRCS);

		if (MyConstants.DEBUG_INFO) {
			Log.dumpln("[Sinks initialization]: " + SINKS);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void internalTransform(Body body, String string, Map map) {
		SootMethod sMethod = body.getMethod();
		SootClass sClass = sMethod.getDeclaringClass();
		if (!sClass.isApplicationClass()) {
			return;
		}

		Log.dumpln("[Locating sources/sinks] in " + sMethod);
		/* check body of sMethod */
		Body sBody = sMethod.retrieveActiveBody();
		Iterator sIter = sBody.getUnits().iterator();

		while (sIter.hasNext()) {
			Stmt s = (Stmt) sIter.next();
			// if statement s is a definition statement then check if the rhs is invoke
			// expression or instance field
			if (s instanceof DefinitionStmt) {
				Value rhs = ((DefinitionStmt) s).getRightOp();
				if (rhs instanceof InvokeExpr) {
					String signature = "";
					try {
						signature = ((InvokeExpr) rhs).getMethod().getSignature();
					} catch (Throwable t) {
						continue;
					}

					if (SRCS.contains(signature)) {
						if (!sClass.isPhantom()) {
							saveSourceLocation(signature, sMethod);
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("source added #1: " + signature + " method:" + sMethod);
						} else {
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("ignore source in lib: " + signature + " method:" + sMethod);
						}
					}

					if (SINKS.contains(signature)) {
						if (!sClass.isPhantom()) {
							saveSinkLocation(signature, sMethod);
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("sink added #1: " + signature + " method:" + sMethod);
						} else {
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("ignore sink in lib: " + signature + " method:" + sMethod);
						}
					}
				} else if (rhs instanceof InstanceFieldRef) {
					String signature = ((InstanceFieldRef) rhs).getField().getSignature();

					if (SRCS.contains(signature)) {
						if (!sClass.isPhantom()) {
							saveSourceLocation(signature, sMethod);
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("source added #2: " + signature + " method:" + sMethod);
						} else {
							if (MyConstants.DEBUG_INFO)
								Log.dumpln("ignore source in lib: " + signature + " method:" + sMethod);
						}
					}
				}
			} else if (s instanceof InvokeStmt) {
				// if statement s is a invoke statement
				String signature = "";
				try {
					signature = s.getInvokeExpr().getMethod().getSignature();
				} catch (Throwable t) {
					continue;
				}

				if (SRCS.contains(signature)) {
					if (!sClass.isPhantom()) {
						saveSourceLocation(signature, sMethod);
						if (MyConstants.DEBUG_INFO)
							Log.dumpln("source added #3: " + signature + " method:" + sMethod);
					} else {
						if (MyConstants.DEBUG_INFO)
							Log.dumpln("ignore source in lib: " + signature + " method:" + sMethod);
					}
				}

				if (SINKS.contains(signature)) {
					if (!sClass.isPhantom()) {
						saveSinkLocation(signature, sMethod);
						if (MyConstants.DEBUG_INFO)
							Log.dumpln("sink added #3: " + signature + " method:" + sMethod);
					} else {
						if (MyConstants.DEBUG_INFO)
							Log.dumpln("ignore sink in lib: " + signature + " method:" + sMethod);
					}
				}
			}
		}
	}

	private void saveSinkLocation(String sig, SootMethod hostMethod) {
		String loc = hostMethod.getDeclaringClass().getName() + "|" + hostMethod.getSignature();

		LinkedHashMap<String, String> locations = null;
		if (sinksLocationMap.containsKey(sig)) {
			locations = sinksLocationMap.get(sig);
			locations.put(loc, hostMethod.getSubSignature());
		} else {
			locations = new LinkedHashMap<String, String>();
			locations.put(loc, hostMethod.getSubSignature());
			sinksLocationMap.put(sig, locations);
		}
	}

	private void saveSourceLocation(String sig, SootMethod hostMethod) {
		String loc = hostMethod.getDeclaringClass().getName() + "|" + hostMethod.getSignature();
		LinkedHashMap<String, String> locations = null;
		if (sourcesLocationMap.containsKey(sig)) {
			locations = sourcesLocationMap.get(sig);
			locations.put(loc, hostMethod.getSubSignature());
		} else {
			locations = new LinkedHashMap<String, String>();
			locations.put(loc, hostMethod.getSubSignature());
			sourcesLocationMap.put(sig, locations);
		}
	}

	/*
	 * We want to treat each <source API, location> as a unique
	 * location, rather than <source API>
	 */
	public static LinkedHashMap<String, Integer> getSources(LocateSourcesAndSinksTransformer[] transformers) {
		LinkedHashMap<String, Integer> sources = new LinkedHashMap<String, Integer>();
		int count = 0;
		for (LocateSourcesAndSinksTransformer trans : transformers) {
			try {
				LinkedHashMap<String, LinkedHashMap<String, String>> sourcesLocationMap = trans.getSourcesLocationMap();
				for (String source : sourcesLocationMap.keySet()) {
					Map<String, String> locs = sourcesLocationMap.get(source);
					for (Map.Entry<String, String> ent : locs.entrySet()) {
						sources.put(source + "|" + ent.getKey(), new Integer(count));
						count++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sources;
	}

	public static LinkedHashMap<String, Integer> getSinks(LocateSourcesAndSinksTransformer[] transformers) {
		LinkedHashMap<String, Integer> sinks = new LinkedHashMap<String, Integer>();
		int count = 0;
		for (LocateSourcesAndSinksTransformer trans : transformers) {
			try {
				LinkedHashMap<String, LinkedHashMap<String, String>> sinksLocationMap = trans.getSinksLocationMap();
				for (String sink : sinksLocationMap.keySet()) {
					if (sinks.containsKey(sink)) {
						continue;
					}

					sinks.put(sink, new Integer(count));
					count++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return sinks;
	}

	public LinkedHashMap<String, LinkedHashMap<String, String>> getSinksLocationMap() {
		return this.sinksLocationMap;
	}

	public LinkedHashMap<String, LinkedHashMap<String, String>> getSourcesLocationMap() {
		return this.sourcesLocationMap;
	}

	public void setSinksLocationMap(LinkedHashMap<String, LinkedHashMap<String, String>> sinksLocationMap) {
		this.sinksLocationMap = sinksLocationMap;
	}

	public void setSourcesLocationMap(LinkedHashMap<String, LinkedHashMap<String, String>> sourcesLocationMap) {
		this.sourcesLocationMap = sourcesLocationMap;
	}

	public static LinkedHashMap<String, LinkedHashMap<String, String>> getSinksLocationMap(LocateSourcesAndSinksTransformer[] transformers) {
		LinkedHashMap<String, LinkedHashMap<String, String>> combinedSinksLocationMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();

		for (LocateSourcesAndSinksTransformer trans : transformers) {
			combinedSinksLocationMap.putAll(trans.getSinksLocationMap());
		}

		return combinedSinksLocationMap;
	}
}