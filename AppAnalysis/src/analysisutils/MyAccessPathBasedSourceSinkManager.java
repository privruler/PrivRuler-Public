package analysisutils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import dfa.util.Log;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
//import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
//import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.StatementSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

public class MyAccessPathBasedSourceSinkManager extends AccessPathBasedSourceSinkManager {
	private Set<String> stringSourcesSigs;
	private Set<String> fieldSourceSigs;
	private Set<String> stmtSourceSigs;

	public MyAccessPathBasedSourceSinkManager(Set<ISourceSinkDefinition> sources, Set<ISourceSinkDefinition> sinks,
			Set<AndroidCallbackDefinition> callbackMethods, /*
			Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks,
            Set<CallbackDefinition> callbackMethods,*/
			InfoflowAndroidConfiguration config,
			Map<Integer, AndroidLayoutControl> layoutControls, Set<String> stringSourcesSigs,
			Set<String> fieldSourceSigs, Set<String> stmtSourceSigs) {
		super(sources, sinks, callbackMethods, config, layoutControls);

		this.stringSourcesSigs = stringSourcesSigs;
		this.fieldSourceSigs = fieldSourceSigs;
		this.stmtSourceSigs = stmtSourceSigs;
	}

	private boolean isDataBlockSource(Stmt sCallSite) {
		return this.stmtSourceSigs.contains(sCallSite.toString());
	}

	private boolean isStringSource(Stmt sCallSite) {
		List<ValueBox> checkConstUseBoxes = sCallSite.getUseBoxes();
		for (ValueBox ccVB : checkConstUseBoxes) {
			if (ccVB.getValue() instanceof StringConstant) {
				String strV = ((StringConstant) ccVB.getValue()).value;

				for (String strSourceSig : this.stringSourcesSigs) {
					if (Pattern.matches(strSourceSig, strV)) {
						return true;
					}
					;
				}
			}
		}
		return false;
	}

	private boolean isFieldSource(Stmt sCallSite) {
		try {
			if (sCallSite instanceof DefinitionStmt) {
				Value rhs = ((DefinitionStmt) sCallSite).getRightOp();
				if (rhs instanceof FieldRef) {
					SootField rhsField = ((FieldRef) rhs).getField();
					if (this.fieldSourceSigs.contains(rhsField.getSignature())) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isSource(Stmt sCallSite) {
		return (this.isStringSource(sCallSite) || this.isDataBlockSource(sCallSite) || this.isFieldSource(sCallSite));
	}

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		SourceInfo ret = super.getSourceInfo(sCallSite, manager);

		if (ret != null) {
			SootMethod srcMethod = manager.getICFG().getMethodOf(sCallSite);
			if (Globals.EXTRA_SOURCE_FILTER.shouldIgnoreSource(sCallSite, srcMethod.getDeclaringClass().getName())) {
				Log.dumpln("SOURCE BLOCKED: " + sCallSite.toString() + " in " + srcMethod.getSignature());
				return null;
			}

			// if this api returns value in parameter
			if (sCallSite.containsInvokeExpr()
					&& Globals.RETURN_IN_PARAMS.containsKey(sCallSite.getInvokeExpr().getMethod().getSignature())) {
				InvokeExpr invokeExpr = (InvokeExpr) sCallSite.getInvokeExpr();
				Value varg0 = invokeExpr.getArg(Globals.RETURN_IN_PARAMS.get(invokeExpr.getMethod().getSignature()));
				if (varg0 instanceof Local) {
					Local local = (Local) varg0;
					Set<AccessPathTuple> apTuple = new HashSet<AccessPathTuple>();
					apTuple.add(AccessPathTuple.create(true, false));
					ISourceSinkDefinition/*SourceSinkDefinition*/ def = new StatementSourceSinkDefinition(sCallSite, local, apTuple);
					SourceInfo fret = createSourceInfo(sCallSite, manager, def);
					return fret;
				}
			}
			return ret;
		}

		if (!isSource(sCallSite)) {
			return null;
		}

		try {
			if (sCallSite instanceof DefinitionStmt) {
				Value lhs = ((DefinitionStmt) sCallSite).getLeftOp();
				if (lhs instanceof FieldRef) {
					SootField lhsField = ((FieldRef) lhs).getField();

					ISourceSinkDefinition/*SourceSinkDefinition*/ def = new FieldSourceSinkDefinition(lhsField.getSignature());
					this.sourceFields.put(lhsField, def);

					SourceInfo fret = createSourceInfo(sCallSite, manager, def);
					return fret;
				} else {
					Local local = null;
					for (ValueBox vb : sCallSite.getUseAndDefBoxes()) {
						if (vb.getValue() instanceof Local) {
							local = (Local) vb.getValue();
							break;
						}
					}

					if (local != null) {
						Set<AccessPathTuple> apTuple = new HashSet<AccessPathTuple>();
						apTuple.add(AccessPathTuple.create(true, false));
						ISourceSinkDefinition/*SourceSinkDefinition*/ def = new StatementSourceSinkDefinition(sCallSite, local, apTuple);
						this.sourceStatements.put(sCallSite, def);

						SourceInfo fret = createSourceInfo(sCallSite, manager, def);
						return fret;
					}
				}
			} else if (sCallSite.containsInvokeExpr()) {
				if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
					Local lLocal = (Local) (((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase());
					Set<AccessPathTuple> apTuple = new HashSet<AccessPathTuple>();
					apTuple.add(AccessPathTuple.create(true, false));
					ISourceSinkDefinition/*SourceSinkDefinition*/ def = new StatementSourceSinkDefinition(sCallSite, lLocal, apTuple);
					this.sourceStatements.put(sCallSite, def);

					SourceInfo fret = createSourceInfo(sCallSite, manager, def);
					return fret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath sourceAccessPath) {
		return super.getSinkInfo(sCallSite, manager, sourceAccessPath);
	}
}
