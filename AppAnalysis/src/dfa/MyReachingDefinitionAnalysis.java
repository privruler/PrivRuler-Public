package dfa;

import java.util.*;

import soot.*;
import soot.toolkits.scalar.*;
import soot.toolkits.graph.*;
import soot.util.*;
import soot.options.*;

public class MyReachingDefinitionAnalysis extends ForwardFlowAnalysis {
	FlowSet emptySet;
	Map<Local, BoundedFlowSet> localToPreserveSet;
	Map<Local, IntPair> localToIntPair;

	public MyReachingDefinitionAnalysis(UnitGraph g) {
		super(g);

		Object[] defs;
		FlowUniverse defUniverse;

		if (Options.v().time())
			Timers.v().defsSetupTimer.start();

		// Create a list of all the definitions and group defs of the same local
		// together
		{
			Map<Local, ArrayList> localToDefList = new HashMap<Local, ArrayList>(g.getBody().getLocalCount() * 2 + 1, 0.7f);

			// Initialize the set of defs for each local to empty
			{
				Iterator localIt = g.getBody().getLocals().iterator();

				while (localIt.hasNext()) {
					Local l = (Local) localIt.next();

					localToDefList.put(l, new ArrayList());
				}
			}

			// Fill the sets up
			{
				Iterator it = g.iterator();

				while (it.hasNext()) {
					Unit s = (Unit) it.next();

					List defBoxes = s.getDefBoxes();
					if (!defBoxes.isEmpty()) {
						if (!(defBoxes.size() == 1))
							throw new RuntimeException("invalid number of def boxes");

						if (((ValueBox) defBoxes.get(0)).getValue() instanceof Local) {
							Local defLocal = (Local) ((ValueBox) defBoxes.get(0)).getValue();
							List<Unit> l = localToDefList.get(defLocal);

							if (l == null)
								throw new RuntimeException("local " + defLocal + " is used but not declared!");
							else
								l.add(s);
						}
					}

				}
			}

			// Generate the list & localToIntPair
			{
				Iterator it = g.getBody().getLocals().iterator();
				List defList = new LinkedList();

				int startPos = 0;

				localToIntPair = new HashMap<Local, IntPair>(g.getBody().getLocalCount() * 2 + 1, 0.7f);

				// For every local, add all its defs
				{
					while (it.hasNext()) {
						Local l = (Local) it.next();
						Iterator jt = localToDefList.get(l).iterator();

						int endPos = startPos - 1;

						while (jt.hasNext()) {
							defList.add(jt.next());
							endPos++;
						}

						localToIntPair.put(l, new IntPair(startPos, endPos));

						// G.v().out.println(startPos + ":" + endPos);

						startPos = endPos + 1;
					}
				}

				defs = defList.toArray();
				defUniverse = new ArrayFlowUniverse(defs);
			}
		}

		emptySet = new ArrayPackedSet(defUniverse);

		// Create the preserve sets for each local.
		{
			Map<Local, FlowSet> localToKillSet = new HashMap<Local, FlowSet>(g.getBody().getLocalCount() * 2 + 1, 0.7f);
			localToPreserveSet = new HashMap<Local, BoundedFlowSet>(g.getBody().getLocalCount() * 2 + 1, 0.7f);

			Chain locals = g.getBody().getLocals();

			// Initialize to empty set
			{
				Iterator localIt = locals.iterator();

				while (localIt.hasNext()) {
					Local l = (Local) localIt.next();

					localToKillSet.put(l, emptySet.clone());
				}
			}

			for (Object element : defs) {
				Unit s = (Unit) element;

				List defBoxes = s.getDefBoxes();
				if (!(defBoxes.size() == 1))
					throw new RuntimeException("SimpleLocalDefs: invalid number of def boxes");

				if (((ValueBox) defBoxes.get(0)).getValue() instanceof Local) {
					Local defLocal = (Local) ((ValueBox) defBoxes.get(0)).getValue();
					BoundedFlowSet killSet = (BoundedFlowSet) localToKillSet.get(defLocal);
					killSet.add(s, killSet);

				}
			}

			// Store complement
			{
				Iterator localIt = locals.iterator();

				while (localIt.hasNext()) {
					Local l = (Local) localIt.next();

					BoundedFlowSet killSet = (BoundedFlowSet) localToKillSet.get(l);

					killSet.complement(killSet);

					localToPreserveSet.put(l, killSet);
				}
			}
		}

		if (Options.v().time())
			Timers.v().defsSetupTimer.end();

		if (Options.v().time())
			Timers.v().defsAnalysisTimer.start();

		doAnalysis();

		if (Options.v().time())
			Timers.v().defsAnalysisTimer.end();
	}

	protected Object newInitialFlow() {
		return emptySet.clone();
	}

	protected Object entryInitialFlow() {
		return emptySet.clone();
	}

	protected void flowThrough(Object inValue, Object d, Object outValue) {
		FlowSet in = (FlowSet) inValue, out = (FlowSet) outValue;
		Unit unit = (Unit) d;

		List defBoxes = unit.getDefBoxes();
		if (!defBoxes.isEmpty()) {
			if (!(defBoxes.size() == 1))
				throw new RuntimeException("SimpleLocalDefs: invalid number of def boxes");

			Value value = ((ValueBox) defBoxes.get(0)).getValue();
			if (value instanceof Local) {
				Local defLocal = (Local) value;

				// Perform kill on value
				in.intersection(localToPreserveSet.get(defLocal), out);

				// Perform generation
				out.add(unit, out);
			} else {
				in.copy(out);
				return;
			}

		} else
			in.copy(out);
	}

	protected void copy(Object source, Object dest) {
		FlowSet sourceSet = (FlowSet) source, destSet = (FlowSet) dest;

		sourceSet.copy(destSet);
	}

	protected void merge(Object in1, Object in2, Object out) {
		FlowSet inSet1 = (FlowSet) in1, inSet2 = (FlowSet) in2;

		FlowSet outSet = (FlowSet) out;

		inSet1.union(inSet2, outSet);
	}
}