package dfa;

public interface MyConstants {
	public static boolean DEBUG_INFO = true;

	// for analysis. conform to parameter index
	public static int thisObject = -1;
	public static int returnValue = -2;
	public static int noArgument = -3;

	// constraint for analysis
	public static int MAX_APINODES_CONSIDERED = 100;
	public static boolean CONSIDER_REDEFINE = true;

	public static boolean TO_TAINT_INSTANCE_FIELD = true;
	public static boolean TO_TAINT_STATIC_FIELD = true;
	public static int DFA_DEPTH = 5;
}