package analysisutils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import privruler.ComponentManager;
import soot.G;
import soot.Hierarchy;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.IdentityTaintWrapper;
import soot.jimple.infoflow.taintWrappers.TaintWrapperSet;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

public class AnalysisAPIs {
	/*
	 * use "uncalled methods in registered components" as entry points
	 */
	public static void setupDefaultEntries() {
		Globals.EXTRA_ENTRY_POINT_FILTER = new ExtraEntryPointFilter() {
			@Override
			public boolean shouldIgnoreEntryPoint(Pair<String, String> entry) {
				String hostClazz = entry.getO1();
				if (hostClazz.contains("$")) {
					hostClazz = hostClazz.substring(0, hostClazz.indexOf("$"));
				}

				if (ComponentManager.getInstance().hasComponent(hostClazz)) {
					return false;
				}
				return true;
			}
		};

		DefaultEntryPointCollector defaultEntryPointCollector = new DefaultEntryPointCollector();
		runCustomPack("jtp",
				new Transform[] { new Transform("jtp.defaultEntryPointCollector", defaultEntryPointCollector) });

		Set<Pair<String, String>> uncalled = defaultEntryPointCollector.getAppUncalledMethods();
		for (Pair<String, String> entry : uncalled) {
			if (!Globals.EXTRA_ENTRY_POINT_FILTER.shouldIgnoreEntryPoint(entry)) {
				Globals.ENTRY_POINTS.add(entry);
				// Log.dumpln(String.format("Entry: <%s: %s>", entry.getO1(), entry.getO2()));
			}
		}
	}

	/*
	 * use flowdroid to find reachable methods
	 */
	public static void getReachableMethods() {
		G.reset();
		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		config.getAnalysisFileConfig().setTargetAPKFile(Globals.APK_PATH);
		config.getAnalysisFileConfig().setAndroidPlatformDir(Globals.FRAMEWORK_DIR);
		config.setTaintAnalysisEnabled(false);

		Set<String> unused = new HashSet<String>();
		SetupApplication analyzer = new MySetupApplication(config, unused, unused, unused);

		SootConfigForAndroid sootConf = new SootConfigForAndroid() {
			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				super.setSootOptions(options, config);
				
				options.set_process_multiple_dex(true);
				options.set_output_format(Options.output_format_jimple);
				options.set_force_overwrite(true);
				options.set_output_dir(Globals.JIMPLE_SUBDIR);
			}
		};

		analyzer.setSootConfig(sootConf);
		analyzer.constructCallgraph();

		ReachableMethods rm = Scene.v().getReachableMethods();
		for (Iterator rmIterator = Scene.v().getReachableMethods().listener(); rmIterator.hasNext();) {
			SootMethod m = (SootMethod) rmIterator.next();
			Globals.REACHABLE_METHODS.add(m.getSignature());
		}

		// add fragments as reachable (over-estimation)
		// https://github.com/secure-software-engineering/FlowDroid/issues/188
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		Chain<SootClass> clazzes = Scene.v().getClasses();
		for (SootClass clazz : clazzes) {
			try {
				for (SootClass superClazz : hierarchy.getSuperclassesOf(clazz)) {
					String superClazzName = superClazz.getName();
					if (superClazzName.contains("android.support.v4.app.Fragment")
							|| superClazzName.contains("android.app.Fragment")) {
						for (SootMethod method : clazz.getMethods()) {
							Globals.REACHABLE_METHODS.add(method.getSignature());
						}
						break;
					}
				}
			} catch (Exception e) {
			}
		}

		/*
		 * List<SootMethod> dummyEntries = Scene.v().getEntryPoints(); for (SootMethod m
		 * : dummyEntries) { Body body = m.getActiveBody();
		 * 
		 * Iterator<Unit> iter = body.getUnits().iterator(); while (iter.hasNext()) {
		 * Stmt s = (Stmt) iter.next(); if (!s.containsInvokeExpr()) { continue; }
		 * 
		 * SootMethod calleeMethod = s.getInvokeExpr().getMethod(); Pair<String, String>
		 * entry = new Pair<String, String>(calleeMethod.getDeclaringClass().getName(),
		 * calleeMethod.getSubSignature()); Globals.ENTRY_POINTS.add(entry);
		 * Log.dumpln(String.format("Entry: <%s: %s>", entry.getO1(), entry.getO2())); }
		 * }
		 */
	}

	public static void runCustomPack(String packName, Transform[] transforms) {
		long startTime = 0;
		long endTime = 0;
		startTime = System.currentTimeMillis();

		G.reset();
		for (Transform transform : transforms) {
			PackManager.v().getPack(packName).add(transform);
		}
		Options.v().set_src_prec(soot.options.Options.src_prec_apk);
		Options.v().set_process_dir(Collections.singletonList(Globals.APK_PATH));
		Options.v().set_android_jars(Globals.FRAMEWORK_DIR);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_force_overwrite(true);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_exclude(Globals.EXCLUDED_ANDROID);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_output_dir(Globals.JIMPLE_SUBDIR);
		Options.v().set_no_writeout_body_releasing(true);

		soot.Main.main(new String[] { "-output-format", "J" });

		endTime = System.currentTimeMillis();
		System.out.println(String.format("Pack %s costs %d seconds", packName, (endTime - startTime) / 1000));
	}

	public static InfoflowResults taintPropagation(Collection<String> srcAPIs, Collection<String> dstAPIs,
			TaintPropagationHandler taintPropHandler, TaintPropagationHandler backwardPropHandler,
			Set<String> stringSourcesSigs, Set<String> fieldSourceSigs, Set<String> stmtSourceSigs,
			ResultsAvailableHandler handler) {
		InfoflowResults results = null;

		long startTime = 0;
		long endTime = 0;
		startTime = System.currentTimeMillis();

		try {
			FileWriter fileWriter = new FileWriter(Globals.SRC_SINK_FILE);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			for (String sc : srcAPIs) {
				printWriter.printf("%s -> _SOURCE_\n", sc);
			}
			for (String sk : dstAPIs) {
				printWriter.printf("%s -> _SINK_\n", sk);
			}
			printWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			G.reset();
			InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
			config.getAnalysisFileConfig().setTargetAPKFile(Globals.APK_PATH);
			config.getAnalysisFileConfig().setAndroidPlatformDir(Globals.FRAMEWORK_DIR);
			config.getAnalysisFileConfig().setSourceSinkFile(Globals.SRC_SINK_FILE);
			// config.setImplicitFlowMode(ImplicitFlowMode.AllImplicitFlows);
			// config.getAccessPathConfiguration().setAccessPathLength(4);

			SetupApplication analyzer = new MySetupApplication(config, stringSourcesSigs, fieldSourceSigs,
					stmtSourceSigs);

			if (taintPropHandler != null) {
				analyzer.setTaintPropagationHandler(taintPropHandler);
			}

			if (backwardPropHandler != null) {
				analyzer.setBackwardsPropagationHandler(backwardPropHandler);
			}

			SootConfigForAndroid sootConf = new SootConfigForAndroid() {
				@Override
				public void setSootOptions(Options options, InfoflowConfiguration config) {
					super.setSootOptions(options, config);
					options.set_process_multiple_dex(true);
					options.set_exclude(Globals.EXCLUDED_ANDROID);
					options.set_no_bodies_for_excluded(true);
					options.set_output_format(Options.output_format_jimple);
					options.set_force_overwrite(true);
					options.set_output_dir(Globals.JIMPLE_SUBDIR);
					Options.v().set_no_writeout_body_releasing(true);
				}
			};

			analyzer.setSootConfig(sootConf);

			/*
			 * Here we use EasyTaintWrapper by default. This may lose track of some data
			 * flows. Check out the other taint wrappers in the following link or implement
			 * our own if necessary.
			 * 
			 * https://github.com/secure-software-engineering/FlowDroid/tree/develop/soot-
			 * infoflow/src/soot/jimple/infoflow/taintWrappers
			 */
			TaintWrapperSet wrapperSet = new TaintWrapperSet();
			wrapperSet.addWrapper(new IdentityTaintWrapper());
			EasyTaintWrapper easyTaintWrapper = new EasyTaintWrapper(
					new File(Globals.CONFIG_DIR + "EasyTaintWrapperSource.txt"));
			wrapperSet.addWrapper(easyTaintWrapper);
			analyzer.setTaintWrapper(wrapperSet);

			if (handler != null) {
				analyzer.addResultsAvailableHandler(handler);
			}
			results = analyzer.runInfoflow();
		} catch (Exception e) {
			e.printStackTrace();
		}

		endTime = System.currentTimeMillis();
		System.out.println(String.format("Taint propagation costs %d seconds", (endTime - startTime) / 1000));
		return results;
	}
}
