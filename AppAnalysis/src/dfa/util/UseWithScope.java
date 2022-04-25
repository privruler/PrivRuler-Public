package dfa.util;

import soot.jimple.*;

public class UseWithScope {

	private Stmt use;
	private Stmt scopeEnd;

	public UseWithScope(Stmt use) {
		this.use = use;
		this.scopeEnd = null;
	}

	public UseWithScope(Stmt use, Stmt scope) {
		this.use = use;
		this.scopeEnd = scope;
	}

	public Stmt getUse() {
		return this.use;
	}

	public Stmt getScopeEnd() {
		return this.scopeEnd;
	}

	public void setUse(Stmt use) {
		this.use = use;
	}

	public void setScopeEnd(Stmt scope) {
		this.scopeEnd = scope;
	}

	public String dump() {
		if (scopeEnd == null) {
			return use.toString();
		} else {
			return new StringBuilder(use + "[BEGIN <- " + scopeEnd + "]").toString();
		}
	}
}