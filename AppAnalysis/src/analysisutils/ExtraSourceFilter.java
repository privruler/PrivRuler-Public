package analysisutils;

import soot.jimple.Stmt;

public interface ExtraSourceFilter {
	public boolean shouldIgnoreSource(Stmt stmt, String hostClazzName);
}
