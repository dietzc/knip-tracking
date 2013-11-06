package org.knime.knip.trackingrevised.trackmate;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.knip.trackingrevised.util.PartitionComparator;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * Splits & Merges Tracks
 * 
 * @author Raffael Wagner, Jonas Zinn
 */
public class LAPMergeSplitterModel extends KPartiteGraphNodeModel {
	NodeLogger logger = NodeLogger.getLogger(LAPMergeSplitterModel.class);

	private final static String CFGKEY_MTHRESHOLD = "mthreshold";

	static SettingsModelDoubleBounded createMThresholdSetting() {
		return new SettingsModelDoubleBounded(CFGKEY_MTHRESHOLD, 15.0, 1, 100);
	}

	private final static String CFGKEY_THRESHOLD = "threshold";

	static SettingsModelDoubleBounded createThresholdSetting() {
		return new SettingsModelDoubleBounded(CFGKEY_THRESHOLD, 50.0, 1, 100);
	}

	private final static String CFGKEY_STHRESHOLD = "sthreshold";

	static SettingsModelDoubleBounded createSThresholdSetting() {
		return new SettingsModelDoubleBounded(CFGKEY_STHRESHOLD, 15.0, 1, 100);
	}

	private final static String CFGKEY_DOMERGE = "merge";

	static SettingsModelBoolean createMergeSetting() {
		return new SettingsModelBoolean(CFGKEY_DOMERGE, true);
	}

	private final static String CFGKEY_DOSPLIT = "split";

	static SettingsModelBoolean createSplitSetting() {
		return new SettingsModelBoolean(CFGKEY_DOSPLIT, true);
	}

	private SettingsModelDoubleBounded m_mThresholdSetting = createMThresholdSetting();
	private SettingsModelDoubleBounded m_sThresholdSetting = createSThresholdSetting();
	private SettingsModelDoubleBounded m_thresholdSetting = createThresholdSetting();
	private SettingsModelBoolean m_merge = createMergeSetting();
	private SettingsModelBoolean m_split = createSplitSetting();

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		return spec;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {

		double mthreshold = m_mThresholdSetting.getDoubleValue();
		double sthreshold = m_sThresholdSetting.getDoubleValue();
		double threshold = m_thresholdSetting.getDoubleValue();
		boolean doMerge = m_merge.getBooleanValue();
		boolean doSplit = m_split.getBooleanValue();

		double sigma = net.getDoubleFeature(net,
				TrackingConstants.NETWORK_FEATURE_STDEV);

		List<Partition> partitions = new LinkedList<Partition>(
				net.getPartitions(PartitionType.NODE));

		// final tracklet edges
		Partition trackletEdgePartition = net
				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);

		// sort to ensure correct order
		Collections.sort(partitions, new PartitionComparator());

		List<String> starts = (List<String>) net.getListFeature(net,
				TrackMateConstants.START_NODES);
		List<String> ends = (List<String>) net.getListFeature(net,
				TrackMateConstants.END_NODES);

		HashSet<PersistentObject> delEnd = new HashSet<PersistentObject>();
		HashSet<PersistentObject> delStart = new HashSet<PersistentObject>();

		int numberPart = partitions.size() - 1;
		// merging
		if (doMerge) {
			// Loop over the end nodes of each tracklet
			for (String o : ends) {
				PersistentObject end = net.getNode(o);
				// check if the found end node is in the last frame
				int timeFrame = net.getIntegerFeature(end,
						TrackMateConstants.TIME_STAMPS);
				timeFrame += 1;
				if (!(timeFrame <= numberPart))
					continue;
				// find all nodes close to the chosen end node
				double min = Double.POSITIVE_INFINITY;
				PersistentObject mergeObj = null;
				for (PersistentObject obj : net.getOutgoingEdges(end)) {
					PersistentObject merge = null;
					// filter out nodes that are in former frames
					for (PersistentObject x : net.getIncidentNodes(obj))
						if (!end.equals(x))
							merge = x;
					if (timeFrame > net.getIntegerFeature(merge,
							TrackMateConstants.TIME_STAMPS))
						continue;
					// find the closest node that is within the max Radius
					double weight = net.getEdgeWeight(obj);
					if (weight < min && weight < mthreshold) {
						min = weight;
						mergeObj = merge;
					}
				}
				// is the found edge valid
				double prob = (Math.exp(-min / sigma) * 100);
				if (null == mergeObj || prob < threshold)
					continue;

				// add the found edge and merge the two tracklets and change the
				// entries in the nodes
				PersistentObject edge = net.createEdge(end + "-" + mergeObj,
						trackletEdgePartition, end, mergeObj);
				net.setEdgeWeight(edge, (int) prob);
				net.addFeature(end, TrackingConstants.FEATURE_ISTRACKLETEND,
						false);
				delEnd.add(end);
				PersistentObject start = net.getNode(net.getFeatureString(end,
						TrackingConstants.FEATURE_TRACKLETSTARTNODE));
				delStart.add(start);
				PersistentObject newStart = net.getNode(net.getFeatureString(
						mergeObj, TrackingConstants.FEATURE_TRACKLETSTARTNODE));
				Integer trackletSizeNew = net.getIntegerFeature(start,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				Integer trackletSizeOld = net.getIntegerFeature(newStart,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				net.addFeature(newStart,
						TrackingConstants.FEATURE_TRACKLET_SIZE,
						trackletSizeNew + trackletSizeOld);
				// switch the startNodes of the one tracklet to the startNode of
				// the other tracklet to have the same
				// startNode in the new bigger tracklet
				boolean found = true;
				for (int i = timeFrame; i >= 0 && found; i--) {
					found = false;
					for (PersistentObject n : net.getNodes(partitions.get(i))) {

						String startNode = net.getFeatureString(n,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE);
						if (start.getId().equals(startNode)) {
							found = true;
							net.addFeature(
									n,
									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
									newStart.getId());
						}
					}
				}
			}
			ends.removeAll(delEnd);
			starts.removeAll(delStart);
			delEnd.clear();
			delStart.clear();
		}

		// spliting
		if (doSplit) {
			// Loop over the start nodes of each tracklet
			for (String o : starts) {
				PersistentObject start = net.getNode(o);
				// check if the found start node is in the first frame
				int timeFrame = net.getIntegerFeature(start,
						TrackMateConstants.TIME_STAMPS);
				timeFrame -= 1;
				if (!(timeFrame >= 0))
					continue;
				// find all nodes close to the chosen start node
				double min = Double.POSITIVE_INFINITY;
				PersistentObject splitObj = null;

				for (PersistentObject obj : net.getOutgoingEdges(start)) {
					PersistentObject split = null;
					// filter out nodes that are in later frames
					for (PersistentObject x : net.getIncidentNodes(obj))
						if (!start.equals(x))
							split = x;
					if (timeFrame < net.getIntegerFeature(split,
							TrackMateConstants.TIME_STAMPS))
						continue;
					// find the closest node that is within the max Radius
					double weight = net.getEdgeWeight(obj);
					if (weight < min && weight < sthreshold) {
						min = weight;
						splitObj = split;
					}
				}
				// is the found edge valid
				double prob = (Math.exp(-min / sigma) * 100);
				if (null == splitObj || prob < threshold)
					continue;

				// add the found edge and merge the two tracklets and change the
				// entries in the nodes
				PersistentObject edge = net.createEdge(splitObj + "-" + start,
						trackletEdgePartition, splitObj, start);
				net.setEdgeWeight(edge, (int) prob);

				delStart.add(start);
				PersistentObject newStart = net.getNode(net.getFeatureString(
						splitObj, TrackingConstants.FEATURE_TRACKLETSTARTNODE));
				Integer trackletSizeNew = net.getIntegerFeature(start,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				Integer trackletSizeOld = net.getIntegerFeature(newStart,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				net.addFeature(newStart,
						TrackingConstants.FEATURE_TRACKLET_SIZE,
						trackletSizeNew + trackletSizeOld);
				// switch the startNodes of the one tracklet to the startNode of
				// the other tracklet to have the same
				// startNode in the new bigger tracklet
				boolean found = true;
				for (int i = timeFrame; i < numberPart && found; i++) {
					found = false;
					for (PersistentObject n : net.getNodes(partitions.get(i))) {

						String startNode = net.getFeatureString(n,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE);
						if (start.getId().equals(startNode)) {
							found = true;
							net.addFeature(
									n,
									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
									newStart.getId());
						}
					}
				}
			}
			ends.removeAll(delEnd);
			starts.removeAll(delStart);
		}

		net.addFeature(net, TrackMateConstants.START_NODES, starts);
		net.addFeature(net, TrackMateConstants.END_NODES, ends);

		net.commit();
		return net;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_thresholdSetting.saveSettingsTo(settings);
		m_mThresholdSetting.saveSettingsTo(settings);
		m_sThresholdSetting.saveSettingsTo(settings);
		m_merge.saveSettingsTo(settings);
		m_split.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_thresholdSetting.validateSettings(settings);
		m_mThresholdSetting.validateSettings(settings);
		m_sThresholdSetting.validateSettings(settings);
		m_merge.validateSettings(settings);
		m_split.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_thresholdSetting.loadSettingsFrom(settings);
		m_mThresholdSetting.loadSettingsFrom(settings);
		m_sThresholdSetting.loadSettingsFrom(settings);
		m_merge.loadSettingsFrom(settings);
		m_split.loadSettingsFrom(settings);
	}
}