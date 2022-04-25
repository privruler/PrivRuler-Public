package dfa;

import java.util.*;

import soot.*;
import soot.toolkits.scalar.*;
import soot.toolkits.graph.*;
import soot.options.*;

@SuppressWarnings("rawtypes")
public class MyReachingDefinition implements LocalDefs {
	Map<LocalUnitPair, List> localUnitPairToDefs;

	/**
	 * Computes the analysis given a UnitGraph computed from a method body. It is
	 * recommended that a ExceptionalUnitGraph (or similar) be provided for correct
	 * results in the case of exceptional control flow.
	 * 
	 * @param g
	 *              a graph on which to compute the analysis.
	 * 
	 * @see ExceptionalUnitGraph
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public MyReachingDefinition(UnitGraph g) {
		if (Options.v().time())
			Timers.v().defsTimer.start();

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName() + "]     Constructing SimpleLocalDefs...");

		MyReachingDefinitionAnalysis analysis = new MyReachingDefinitionAnalysis(g);

		if (Options.v().time())
			Timers.v().defsPostTimer.start();

		// Build localUnitPairToDefs map
		{
			Iterator unitIt = g.iterator();

			localUnitPairToDefs = new HashMap<LocalUnitPair, List>(g.size() * 2 + 1, 0.7f);

			while (unitIt.hasNext()) {
				Unit s = (Unit) unitIt.next();

				Iterator boxIt = s.getUseBoxes().iterator();

				while (boxIt.hasNext()) {
					ValueBox box = (ValueBox) boxIt.next();

					if (box.getValue() instanceof Local) {
						Local l = (Local) box.getValue();
						LocalUnitPair pair = new LocalUnitPair(l, s);

						if (!localUnitPairToDefs.containsKey(pair)) {
							IntPair intPair = analysis.localToIntPair.get(l);

							ArrayPackedSet value = (ArrayPackedSet) analysis.getFlowBefore(s);

							List unitLocalDefs = value.toList(intPair.op1, intPair.op2);

							localUnitPairToDefs.put(pair, Collections.unmodifiableList(unitLocalDefs));
						}
					}
				}
			}
		}

		if (Options.v().time())
			Timers.v().defsPostTimer.end();

		if (Options.v().time())
			Timers.v().defsTimer.end();

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName() + "]     SimpleLocalDefs finished.");
	}

	public boolean hasDefsAt(Local l, Unit s) {
		return localUnitPairToDefs.containsKey(new LocalUnitPair(l, s));
	}

	@SuppressWarnings("unchecked")
	public List<Unit> getDefsOfAt(Local l, Unit s) {
		LocalUnitPair pair = new LocalUnitPair(l, s);
		List<Unit> toReturn = localUnitPairToDefs.get(pair);
		if (toReturn == null)
			throw new RuntimeException("Illegal LocalDefs query; local " + l + " has no definition at " + s.toString());

		return toReturn;
	}

	@SuppressWarnings("unchecked")
	public List<Unit> getDefsOf(Local l) {
		List<Unit> list = new LinkedList<Unit>();

		for (Map.Entry<LocalUnitPair, List> entry : localUnitPairToDefs.entrySet()) {
			if (l == entry.getKey().getLocal()) {
				list.addAll(entry.getValue());
			}
		}
		return list;
	}
}
