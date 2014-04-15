package org.knime.knip.tracking.nodes.addfeature;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.tracking.data.featuresnew.FeatureHandler;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.network.core.api.Feature;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphMetaData;
import org.knime.network.core.core.feature.FeatureMetaData;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of AddFeaturesToTrackingNetwork.
 * Preprocessing node adding feature values to each node. Use as preprocessing step for transition graphs.
 *
 * @author Stephan Sellien
 */
public class AddFeaturesToTrackingNetworkNodeModel extends KPartiteGraphNodeModel {

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		List<Feature> features = new LinkedList<Feature>();
		for(String featName : FeatureHandler.getFeatureNames()) {
			featName = "tnfNetPrefix_" + featName;
			features.add(new FeatureMetaData(featName, FeatureTypeFactory.getStringType()));
		}
		GraphMetaData metaData = new GraphMetaData(spec.getMetaData(), features);
		return new GraphPortObjectSpec(metaData);
	}

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
		long noOfNodes = net.getNoOfNodes();
		long count = 1;
		for(PersistentObject pObj : net.getNodes()) {
			exec.setProgress((double)count/noOfNodes);
			TrackedNode node = new TrackedNode(net, pObj);
			FeatureHandler.getFeatureVector(node);
			count++;
		}
		return net;
	}

	@Override
	protected void resetInternal() {
		
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}
    
}

