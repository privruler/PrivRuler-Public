package analysisutils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
//import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.toolkits.scalar.Pair;

public class MySetupApplication extends SetupApplication {
	private Set<String> stringSourcesSigs;
	private Set<String> fieldSourceSigs;
	private Set<String> stmtSourceSigs;

	public MySetupApplication(InfoflowAndroidConfiguration config, Set<String> stringSourcesSigs,
			Set<String> fieldSourceSigs, Set<String> stmtSourceSigs) {
		super(config);

		this.stringSourcesSigs = stringSourcesSigs;
		this.fieldSourceSigs = fieldSourceSigs;
		this.stmtSourceSigs = stmtSourceSigs;
	}

	@Override
	protected void parseAppResources() throws IOException, XmlPullParserException {
		super.parseAppResources();

		for (Pair<String, String> entry_item : Globals.ENTRY_POINTS) {
			try {
				SootClass c = Scene.v().forceResolve(entry_item.getO1(), SootClass.BODIES);
				c.setApplicationClass();
				Scene.v().loadNecessaryClasses();
				this.entrypoints.add(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		entryPointCreator = new MyAndroidEntryPointCreator(this.manifest, this.entrypoints);
	}

	@Override
	public ISourceSinkManager createSourceSinkManager(LayoutFileParser lfp, Set<AndroidCallbackDefinition> callbacks/*
			LayoutFileParser lfp,
            Set<CallbackDefinition> callbacks*/) {

		Map userControlsByID;
		Set sources = this.sourceSinkProvider.getSources();
		Set sinks = this.sourceSinkProvider.getSinks();
		InfoflowAndroidConfiguration infoflowAndroidConfiguration = this.config;
		if (lfp == null) {
			userControlsByID = null;
		} else {
			userControlsByID = lfp.getUserControlsByID();
		}
		AccessPathBasedSourceSinkManager sourceSinkManager2 = new MyAccessPathBasedSourceSinkManager(sources, sinks,
				callbacks, infoflowAndroidConfiguration, userControlsByID, this.stringSourcesSigs, this.fieldSourceSigs,
				this.stmtSourceSigs);
		sourceSinkManager2.setAppPackageName(this.manifest.getPackageName());
		sourceSinkManager2.setResourcePackages(this.resources.getPackages());
		return sourceSinkManager2;
	}
}
