package org.knime.knip.trackingrevised.nodes.trackletcombiner;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelLongBounded;
import org.knime.knip.trackingrevised.nodes.trackletcombiner.hypothesis.HypothesesHandler;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of TrackletCombiner. Combines tracklets to
 * complete tracks.
 * 
 * @author Stephan Sellien
 */
public class TrackletCombinerNodeModel extends KPartiteGraphNodeModel {

	private final static String CFGKEY_INTERVALCOL = "intervalColumn";

	static SettingsModelColumnName createIntervalColumnSettings() {
		return new SettingsModelColumnName(CFGKEY_INTERVALCOL, "");
	}

	private final static String CFGKEY_LAMDA1 = "lamda1";
	private final static String CFGKEY_LAMDA2 = "lamda2";
	private final static String CFGKEY_LAMDA3 = "lamda3";
	private final static String CFGKEY_DELTAS = "deltaS";
	private final static String CFGKEY_DELTAT = "deltaT";
	private final static String CFGKEY_ALPHA = "alpha";

	static SettingsModelDouble createLamda1Settings() {
		return new SettingsModelDouble(CFGKEY_LAMDA1, TrackingConstants.LAMDA_1);
	}

	static SettingsModelDouble createLamda2Settings() {
		return new SettingsModelDouble(CFGKEY_LAMDA2, TrackingConstants.LAMDA_2);
	}

	static SettingsModelDouble createLamda3Settings() {
		return new SettingsModelDouble(CFGKEY_LAMDA3, TrackingConstants.LAMDA_3);
	}

	static SettingsModelDouble createDeltaSSettings() {
		return new SettingsModelDouble(CFGKEY_DELTAS, TrackingConstants.DELTA_S);
	}

	static SettingsModelDouble createDeltaTSettings() {
		return new SettingsModelDouble(CFGKEY_DELTAT, TrackingConstants.DELTA_T);
	}

	static SettingsModelDouble createAlphaSettings() {
		return new SettingsModelDouble(CFGKEY_ALPHA, TrackingConstants.ALPHA);
	}

	private SettingsModelDouble cfgLamda1 = createLamda1Settings();
	private SettingsModelDouble cfgLamda2 = createLamda2Settings();
	private SettingsModelDouble cfgLamda3 = createLamda3Settings();
	private SettingsModelDouble cfgDeltaS = createDeltaSSettings();
	private SettingsModelDouble cfgDeltaT = createDeltaTSettings();
	private SettingsModelDouble cfgAlpha = createAlphaSettings();

	private final static String CFG_TIMEOUT = "timeout";

	static SettingsModelLongBounded createTimeoutSettings() {
		return new SettingsModelLongBounded(CFG_TIMEOUT, 0, 0, Long.MAX_VALUE);
	}

	private SettingsModelLongBounded timeout = createTimeoutSettings();

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		cfgLamda1.saveSettingsTo(settings);
		cfgLamda2.saveSettingsTo(settings);
		cfgLamda3.saveSettingsTo(settings);
		cfgDeltaS.saveSettingsTo(settings);
		cfgDeltaT.saveSettingsTo(settings);
		cfgAlpha.saveSettingsTo(settings);
		timeout.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		cfgLamda1.validateSettings(settings);
		cfgLamda2.validateSettings(settings);
		cfgLamda3.validateSettings(settings);
		cfgDeltaS.validateSettings(settings);
		cfgDeltaT.validateSettings(settings);
		cfgAlpha.validateSettings(settings);
		timeout.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		cfgLamda1.loadSettingsFrom(settings);
		cfgLamda2.loadSettingsFrom(settings);
		cfgLamda3.loadSettingsFrom(settings);
		cfgDeltaS.loadSettingsFrom(settings);
		cfgDeltaT.loadSettingsFrom(settings);
		cfgAlpha.loadSettingsFrom(settings);
		timeout.loadSettingsFrom(settings);
	}

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		return spec;
	}

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
		//
		// final KPartiteGraph<PersistentObject, Partition> myNet =
		// GraphFactory.createIncrementalNet(net);
		//
		// PersistentObject source = myNet.createNode("source");
		// PersistentObject sink = myNet.createNode("sink");
		//
		// Map<PersistentObject, Number> eFM = new HashMap<>();
		//
		// //test
		// JungDirectedGraphAdapter jroa = new JungDirectedGraphAdapter(myNet);
		//
		// Transformer<PersistentObject, Number> transformer = new
		// Transformer<PersistentObject, Number>() {
		//
		// @Override
		// public Double transform(PersistentObject arg0) {
		// try {
		// return myNet.getWeight(arg0);
		// } catch (PersistenceException e) {
		//
		// }
		// return 0.0;
		// }
		// };
		//
		// Factory<PersistentObject> edgeFactory = new
		// Factory<PersistentObject>() {
		//
		// @Override
		// public PersistentObject create() {
		// throw new RuntimeException("FUUU");
		// }
		// };
		//
		// EdmondsKarpMaxFlow<PersistentObject, PersistentObject> mf = new
		// EdmondsKarpMaxFlow<PersistentObject,PersistentObject>(jroa, source,
		// sink, transformer, eFM, edgeFactory);
		//
		// mf.evaluate();
		//
		// System.out.println("blubb");

		new HypothesesHandler(net, exec, cfgLamda1.getDoubleValue(),
				cfgLamda2.getDoubleValue(), cfgLamda3.getDoubleValue(),
				cfgDeltaS.getDoubleValue(), cfgDeltaT.getDoubleValue(),
				cfgAlpha.getDoubleValue(), timeout.getLongValue());
		exec.checkCanceled();
		net.commit();
		return net;
	}
}
