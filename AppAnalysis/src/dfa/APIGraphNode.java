package dfa;

import java.util.Vector;

import soot.SootMethod;
import soot.jimple.Stmt;

public class APIGraphNode {

	private Stmt stmt;
	private Stmt callsite;
	private String annotation;
	private Vector<APIGraphNode> successors;
	private Vector<APIGraphNode> predecessors;
	private APIGraphNode immediateDom;
	private SootMethod hostMethod;

	public APIGraphNode(Stmt stmt) {
		this.stmt = stmt;
		this.callsite = null;
		this.annotation = "";
		this.successors = new Vector<APIGraphNode>();
		this.predecessors = new Vector<APIGraphNode>();
		this.hostMethod = null;
	}

	public APIGraphNode(Stmt stmt, SootMethod host) {
		this.stmt = stmt;
		this.callsite = null;
		this.annotation = "";
		this.successors = new Vector<APIGraphNode>();
		this.predecessors = new Vector<APIGraphNode>();
		this.hostMethod = host;
	}

	public boolean equal(APIGraphNode node) {
		return (this.stmt.toString() == node.getStmt().toString()) && (this.callsite.toString() == node.callsite.toString());
	}

	public APIGraphNode clone() {
		APIGraphNode clone = new APIGraphNode((Stmt) this.getStmt());
		if (this.callsite == null) {
			clone.callsite = null;
		} else {
			clone.callsite = (Stmt) this.callsite;
		}
		clone.annotation = this.annotation;

		return clone;
	}

	public Stmt getStmt() {
		return this.stmt;
	}

	public Stmt getCallsite() {
		return this.callsite;
	}

	public String getAnnotation() {
		return this.annotation;
	}

	public SootMethod getHostMethod() {
		return this.hostMethod;
	}

	public Vector<APIGraphNode> getSuccessors() {
		return this.successors;
	}

	public Vector<APIGraphNode> getPredecessors() {
		return this.predecessors;
	}

	public void setCallsite(Stmt callsite) {
		this.callsite = callsite;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public boolean hasSucc(APIGraphNode succ) {
		return this.successors.contains(succ);
	}

	public void addSucc(APIGraphNode succ) {
		if (!hasSucc(succ)) {
			this.successors.add(succ);
		}
	}

	public void removeSucc(APIGraphNode succ) {
		if (hasSucc(succ)) {
			this.successors.remove(succ);
		}
	}

	public boolean hasPred(APIGraphNode pred) {
		return this.predecessors.contains(pred);
	}

	public void addPred(APIGraphNode pred) {
		if (!hasPred(pred)) {
			this.predecessors.add(pred);
		}
	}

	public void removePred(APIGraphNode pred) {
		if (hasPred(pred)) {
			this.predecessors.remove(pred);
		}
	}

	public void removeDom() {
		this.immediateDom = null;
	}

	public void setImmediateDom(APIGraphNode dom) {
		this.immediateDom = dom;
	}

	public APIGraphNode getImmediateDom() {
		return this.immediateDom;
	}

	public void resetPreds(Vector<APIGraphNode> preds) {
		this.predecessors.clear();
		this.predecessors.addAll(preds);
	}

	public void resetSuccs(Vector<APIGraphNode> succs) {
		this.successors.clear();
		this.successors.addAll(succs);
	}

}