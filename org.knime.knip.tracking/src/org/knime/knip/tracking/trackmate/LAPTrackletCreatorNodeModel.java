package org.knime.knip.tracking.trackmate;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.util.PartitionComparator;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

import fiji.plugin.trackmate.TrackableObjectCollection;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackingUtils;

public class LAPTrackletCreatorNodeModel extends KPartiteGraphNodeModel {

	enum LAPTrackerVariant {
		Hungarian, MunkresKuhnAlgorithm;
	}

	NodeLogger logger = NodeLogger.getLogger(LAPTrackletCreatorNodeModel.class);

	private final static String CFGKEY_MAXRADIUS = "maxradius";

	static SettingsModelDoubleBounded createMaxRadiusSetting() {
		return new SettingsModelDoubleBounded(CFGKEY_MAXRADIUS, 15.0, 1, 100);
	}

	private final static String CFGKEY_THRESHOLD = "threshold";

	static SettingsModelDouble createThresholdSetting() {
		return new SettingsModelDouble(CFGKEY_THRESHOLD, 50);
	}

	private final static String CFGKEY_GAPS = "gaps";

	static SettingsModelBoolean createGapsSetting() {
		return new SettingsModelBoolean(CFGKEY_GAPS, true);
	}

	private SettingsModelDoubleBounded m_maxRadius = createMaxRadiusSetting();

	private SettingsModelDouble m_threshold = createThresholdSetting();

	private SettingsModelBoolean m_gaps = createGapsSetting();

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		return spec;
	}

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
		List<Partition> partitions = new LinkedList<Partition>(
				net.getPartitions(PartitionType.NODE));

		// sort to ensure correct order
		Collections.sort(partitions, new PartitionComparator());

		exec.setMessage("Search for linear edges ...");

		// some network stuff
		Partition trackletEdgePartition = net.createPartition(
				TrackingConstants.TRACKLET_EDGE_PARTITION, PartitionType.EDGE);

		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.FEATURE_TRACKLETSTARTNODE);

		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.FEATURE_TRACKLET_SIZE);

		KNIPLapTracker tracker = new KNIPLapTracker();

		// Set the tracking settings
		final Map<String, Object> trackerSettings = LAPUtils
				.getDefaultLAPSettingsMap();
		trackerSettings.put(KEY_LINKING_MAX_DISTANCE, 20d);
		trackerSettings.put(KEY_ALLOW_GAP_CLOSING, true);

		TrackableObjectCollection<TrackedNode> nodes = new TrackableObjectCollection<TrackedNode>();

		for (Partition p : net.getPartitions()) {
			if (p.getId().startsWith("tra"))
				continue;
			Iterator<PersistentObject> iterator = net.getNodes(p).iterator();
			while (iterator.hasNext()) {
				TrackedNode trackedNode = new TrackedNode(net, iterator.next());
				nodes.add(trackedNode, trackedNode.frame());
			}
		}

		tracker.setTarget(nodes, trackerSettings);
		tracker.setNumThreads(1);
		tracker.process();

		// create tracks
		final ConnectivityInspector<TrackedNode, DefaultWeightedEdge> inspector = new ConnectivityInspector<TrackedNode, DefaultWeightedEdge>(
				tracker.getResult());
		final List<Set<TrackedNode>> unsortedSegments = inspector
				.connectedSets();
		final ArrayList<SortedSet<TrackedNode>> trackSegments = new ArrayList<SortedSet<TrackedNode>>(
				unsortedSegments.size());

		for (final Set<TrackedNode> set : unsortedSegments) {
			final SortedSet<TrackedNode> sortedSet = new TreeSet<TrackedNode>(
					TrackingUtils.frameComparator());
			sortedSet.addAll(set);
			trackSegments.add(sortedSet);
		}

		for (SortedSet<TrackedNode> track : trackSegments) {

			// process first as source
			PersistentObject startNode = track.first().getPersistentObject();
			String startNodeId = startNode.getId();

			// net.addFeatureString(startNode,
			// TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNodeId);
			net.addFeature(startNode, TrackingConstants.FEATURE_ISTRACKLETEND,
					false);
			net.addFeature(startNode, TrackingConstants.FEATURE_TRACKLET_SIZE,
					track.size());
			
			// process first as sink
			// process middle nodes
			PersistentObject endNode = track.last().getPersistentObject();

			if (startNode != endNode)
				net.addFeatureString(endNode,
						TrackingConstants.FEATURE_TRACKLETSTARTNODE,
						startNodeId);

			net.addFeature(endNode, TrackingConstants.FEATURE_ISTRACKLETEND,
					true);

			// skip first
			// Iterator
			Iterator<TrackedNode> iterator = track.iterator();
			iterator.next();
			int ctr = 1;

			PersistentObject prev = startNode;
			while (ctr < track.size() - 1) {
				TrackedNode next = iterator.next();

				net.createEdge(prev + "-" + next, trackletEdgePartition, prev,
						next.getPersistentObject());

				net.addFeature(next.getPersistentObject(),
						TrackingConstants.FEATURE_ISTRACKLETEND, false);

				net.addFeatureString(next.getPersistentObject(),
						TrackingConstants.FEATURE_TRACKLETSTARTNODE,
						startNodeId);

				prev = next.getPersistentObject();
				ctr++;
			}

			// add endnode
			net.createEdge(prev + "-" + endNode, trackletEdgePartition, prev,
					endNode);

		}

		net.commit();
		return net;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_maxRadius.saveSettingsTo(settings);
		m_threshold.saveSettingsTo(settings);
		m_gaps.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_maxRadius.validateSettings(settings);
		m_threshold.validateSettings(settings);
		m_gaps.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_maxRadius.loadSettingsFrom(settings);
		m_threshold.loadSettingsFrom(settings);
		m_gaps.loadSettingsFrom(settings);
	}
}