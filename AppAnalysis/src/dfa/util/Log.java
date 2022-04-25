package dfa.util;

import java.io.*;

public class Log {
	private static String filename;
	
	public static void init(String filename) {
		Log.filename = filename;
		
		File file = new File(filename);
		if (file.exists()) {
			file.delete();
		}
	}

	public static void dumpln(String msg) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
			out.println(msg);
			out.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Can't find file " + filename);
			System.exit(-1);
		} catch (IOException ex) {
			System.err.println("IOException!");
			System.exit(-1);
		}
	}
}
