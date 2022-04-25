package analysisutils;

import soot.toolkits.scalar.Pair;

public interface ExtraEntryPointFilter {
	public boolean shouldIgnoreEntryPoint(Pair<String, String> entry);
}
