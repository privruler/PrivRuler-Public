package privruler;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import analysisutils.AnalysisAPIs;
import analysisutils.Globals;
import dfa.util.Log;
import privruler.methodsignature.SignatureGenerator;
import soot.Transform;

import java.text.SimpleDateFormat;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Provide Params <APK_PATH> <TIMEOUT_SECONDS>");
			System.exit(-1);
		}

		String apkPath = args[0];
		int timeoutSeconds = Integer.parseInt(args[1]);
		new Main().run(apkPath, timeoutSeconds);
	}

	private void run(String apkPath, int timeoutSeconds) {
		Globals.setupApkForAnalysis(apkPath);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss aaa z");
		Log.dumpln(String.format("Analysis of %s starts at %s", Globals.APK_PATH, sdf.format(cal.getTime())));

		ExecutorService executor = Executors.newFixedThreadPool(64);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Main.this.runAnalysis();
			}
		});
		executor.shutdown();

		try {
			if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Log.dumpln(sw.toString());
		}

		Log.dumpln("=======================================================");
		Log.dumpln(" #FINISH#");
		Log.dumpln("=======================================================");
	}

	private void runAnalysis() {
		ComponentManager.getInstance().parseApkForComponents(Globals.APK_PATH);
		AnalysisAPIs.getReachableMethods();
		
		AnalysisAPIs.runCustomPack("jtp", new Transform[] { new Transform("jtp.signaturegenerator", new SignatureGenerator()) });
		
		ResourceCodeBridge rcb = new ResourceCodeBridge();
		rcb.bridgeStmtConstants();
		rcb.processCloudAPIs();

		CloudManager.getInstance("AWS").dumpJSONLineByLine();
		CloudManager.getInstance("AZURE").dumpJSONLineByLine();
		CloudManager.getInstance("ALIYUN").dumpJSONLineByLine();
	}
}