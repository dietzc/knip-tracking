package org.knime.knip.trackingrevised.nodes.adddistanceedges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.Pair;
import org.knime.knip.trackingrevised.util.PartitionComparator;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.Feature;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;

/**
 * This is the model implementation of AddDistanceEdges. Adds edges with
 * distances to the given tracklet network.
 * 
 * @author Stephan Sellien
 */
public class AddDistanceEdgesNodeModel extends KPartiteGraphNodeModel {

	private static final String CFG_EXPRESSION = "expression";
	private static final String CFG_MAXDISTANCE = "maxDistance";

	private SettingsModelString m_expression = createExpressionModel();

	private GraphPortObjectSpec spec;

	static SettingsModelString createExpressionModel() {
		return new SettingsModelString(CFG_EXPRESSION,
				"sqrt($CentroidX$^2+$CentroidY$^2)");
	}

	private SettingsModelDouble m_maxDistance = createMaxDistanceModel();

	static SettingsModelDouble createMaxDistanceModel() {
		return new SettingsModelDouble(CFG_MAXDISTANCE, 500);
	}

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		this.spec = spec;
		return spec;
	}

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
		List<Partition> partitions = new ArrayList<Partition>(
				net.getPartitions(PartitionType.NODE));
		Partition edgePartition = net.createPartition(
				TrackingConstants.DISTANCE_EDGE_PARTITION, PartitionType.EDGE);
		// sort by time
		Collections.sort(partitions, new PartitionComparator());

		// Pair: correct -> encoded
		List<Pair<String, String>> usedFeatures = new LinkedList<Pair<String, String>>();

		JEP jep = JEPHelper.generateJEPwithVariables(spec);
		Node jepnode = jep.parseExpression(m_expression.getStringValue());
		System.out.println(jep.evaluate(jepnode) + " <- "
				+ m_expression.getStringValue());

		// look up correct feature names
		for (String feat : JEPHelper.encodeNumericColumns(spec)) {
			if (m_expression.getStringValue().contains(feat)) {
				for (Feature feature : net.getFeatures()) {
					if (JEPHelper.encode(feature).equals(feat)) {
						usedFeatures.add(new Pair<String, String>(feat, feature
								.getName()));
						break;
					}
				}
			}
		}

		DescriptiveStatistics stats = new DescriptiveStatistics();

		// no need to go to real last partition
		for (int p = 0; p < partitions.size() - 1; p++) {
			exec.checkCanceled();
			exec.setProgress((double) p / partitions.size(), "Partition #" + p);
			Partition partition = partitions.get(p);
			for (PersistentObject node : net.getNodes(partition)) {
				Partition nextPartition = partitions.get(p + 1);

				org.knime.knip.trackingrevised.data.graph.Node myNode = new org.knime.knip.trackingrevised.data.graph.Node(
						net, node);
				for (PersistentObject nextNode : net.getNodes(nextPartition)) {
					double dist = 0;

					// insert correct values into jep
					for (Pair<String, String> pair : usedFeatures) {
						double val1 = net.getDoubleFeature(node,
								pair.getSecond());
						double val2 = net.getDoubleFeature(nextNode,
								pair.getSecond());
						jep.addVariable(pair.getFirst(), Math.abs(val1 - val2));
					}

					dist = jep.getValue();

					org.knime.knip.trackingrevised.data.graph.Node myNextNode = new org.knime.knip.trackingrevised.data.graph.Node(
							net, nextNode);

					double euclidean = myNode.euclideanDistanceTo(myNextNode);

					stats.addValue(dist);

					if (euclidean > m_maxDistance.getDoubleValue())
						continue;

					PersistentObject edge = net.createEdge(
							"sqrdist" + node.getId() + "_" + nextNode.getId(),
							edgePartition, node, nextNode);
					net.setEdgeWeight(edge, dist);
				}
			}
		}

		net.defineFeature(FeatureTypeFactory.getDoubleType(),
				TrackingConstants.NETWORK_FEATURE_STDEV);
		net.addFeature(net, TrackingConstants.NETWORK_FEATURE_STDEV,
				stats.getStandardDeviation());

		System.out.println(stats);

		net.commit();
		return net;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_expression.saveSettingsTo(settings);
		m_maxDistance.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_expression.validateSettings(settings);
		m_maxDistance.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_expression.loadSettingsFrom(settings);
		m_maxDistance.loadSettingsFrom(settings);
	}

}
