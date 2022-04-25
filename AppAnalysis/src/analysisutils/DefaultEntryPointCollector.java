package analysisutils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.Body;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class DefaultEntryPointCollector extends BodyTransformer {
	private Set<Pair<String, String>> appMethods;
	private Set<Pair<String, String>> appCalledMethods;
	private Set<Pair<String, String>> appUncalledMethods;

	public DefaultEntryPointCollector() {
		appMethods = new HashSet<Pair<String, String>>();
		appCalledMethods = new HashSet<Pair<String, String>>();
	}

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		SootMethod method = body.getMethod();

		SootClass clazz = method.getDeclaringClass();
		Pair<String, String> hmPair = new Pair<String, String>(clazz.getName(), method.getSubSignature());

		if (method.isConcrete() && clazz.isApplicationClass()) {
			synchronized (appMethods) {
				appMethods.add(hmPair);
			}
		}

		Iterator<Unit> iter = body.getUnits().iterator();
		while (iter.hasNext()) {
			Stmt s = (Stmt) iter.next();
			if (!s.containsInvokeExpr()) {
				continue;
			}

			SootMethod m = s.getInvokeExpr().getMethod();
			SootClass c = m.getDeclaringClass();
			if (m.isConcrete() && c.isApplicationClass()) {
				Pair<String, String> mPair = new Pair<String, String>(c.getName(), m.getSubSignature());
				synchronized (appCalledMethods) {
					appCalledMethods.add(mPair);
				}
			}
		}
	}

	public Set<Pair<String, String>> getAppUncalledMethods() {
		if (appUncalledMethods == null) {
			appUncalledMethods = Sets.difference(appMethods, appCalledMethods);
		}
		return appUncalledMethods;
	}
}