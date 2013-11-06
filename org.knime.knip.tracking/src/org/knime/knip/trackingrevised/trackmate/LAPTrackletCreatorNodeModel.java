package org.knime.knip.trackingrevised.trackmate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.knip.trackingrevised.util.Hungarian;
import org.knime.knip.trackingrevised.util.PartitionComparator;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

import Jama.Matrix;

/**
 * This is the model implementation of TrackletCreator. Creates Tracklets (parts
 * of a trajectory) out of a given segmentation as graphs.
 * 
 * @author Raffael Wagner, Jonas Zinn
 */
public class LAPTrackletCreatorNodeModel extends KPartiteGraphNodeModel {
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

	private SettingsModelDoubleBounded m_MaxRadius = createMaxRadiusSetting();

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

		double maxRadius = m_MaxRadius.getDoubleValue();
		double threshold = m_threshold.getDoubleValue();
		boolean gapClosing = m_gaps.getBooleanValue();

		List<Partition> partitions = new LinkedList<Partition>(
				net.getPartitions(PartitionType.NODE));

		// sort to ensure correct order
		Collections.sort(partitions, new PartitionComparator());

		exec.setMessage("Search for linear edges ...");

		// final tracklet edges
		Partition trackletEdgePartition = net.createPartition(
				TrackingConstants.TRACKLET_EDGE_PARTITION, PartitionType.EDGE);

		// trackletstartnode
		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.FEATURE_TRACKLETSTARTNODE);
		// tracklet size
		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.FEATURE_TRACKLET_SIZE);

		// the current and the next Partion
		ArrayList<PersistentObject> partitionNodes;
		ArrayList<PersistentObject> nPartitionNodes;

		// a set of all start and end nodes from the Tracklet
		Set<PersistentObject> starts = new HashSet<PersistentObject>();
		Set<PersistentObject> ends = new HashSet<PersistentObject>();
		// time stamp from all nodes
		HashMap<PersistentObject, Integer> timeReference = new HashMap<PersistentObject, Integer>();

		double sigma = net.getDoubleFeature(net,
				TrackingConstants.NETWORK_FEATURE_STDEV);

		// do we already have this source or target
		Set<PersistentObject> sources = new HashSet<PersistentObject>();
		Set<PersistentObject> targets = new HashSet<PersistentObject>();

		// go over all partitions
		for (int p = 0; p < partitions.size() - 1; p++) {
			Partition partition = partitions.get(p);
			Partition nextPartition = partitions.get(p + 1);
			GraphObjectIterator<PersistentObject> t0 = net.getNodes(partition);
			GraphObjectIterator<PersistentObject> t1 = net
					.getNodes(nextPartition);

			// store the nodes in both partitions so we don't
			// need to iterate all time over the partitions
			partitionNodes = new ArrayList<PersistentObject>();
			nPartitionNodes = new ArrayList<PersistentObject>();
			int countp1 = 0;
			for (PersistentObject obj : t0) {
				partitionNodes.add(obj);
				timeReference.put(obj, p);
				countp1++;
			}
			int countp2 = 0;
			for (PersistentObject obj : t1) {
				nPartitionNodes.add(obj);
				countp2++;
			}
			int anz1 = countp1, anz2 = countp2;
			// create cost matrix
			Matrix m = new Matrix(anz1, anz2);
			countp1 = 0;
			// fill cost matrix with edges weight
			t0 = net.getNodes(partition);
			for (PersistentObject obj : t0) {
				countp2 = 0;
				t1 = net.getNodes(nextPartition);
				for (PersistentObject obj2 : t1) {
					PersistentObject edge = null;
					for (PersistentObject t : net.getOutgoingEdges(obj)) {
						if (net.getIncidentNodes(t).contains(obj2)) {
							edge = t;
							break;
						}
					}

					m.set(countp1,
							countp2,
							edge == null ? Double.POSITIVE_INFINITY
									: net.getEdgeWeight(edge) > maxRadius ? Double.POSITIVE_INFINITY
											: net.getEdgeWeight(edge));
					countp2++;
				}
				countp1++;
			}

			exec.checkCanceled();

			boolean allBlocked = true;
			for (int i = 0; i < anz1; i++) {
				for (int j = 0; j < anz2; j++) {
					if (m.get(i, j) != Double.POSITIVE_INFINITY) {
						allBlocked = false;
						break;
					}
				}
				if (!allBlocked)
					break;
			}

			// can we find something in the matrix?
			if (!allBlocked) {
				// apply the Hungarian algo to it
				Hungarian hungarian = new Hungarian(m, 15);
				int[][] solutions = hungarian.solve();

				// Extend track segments using solutions: we update the graph
				// edges
				for (int j = 0; j < solutions.length; j++) {
					if (solutions[j].length == 0)
						continue;
					int i0 = solutions[j][0];
					int i1 = solutions[j][1];

					// connect the found solutions to each other
					// frame to frame linking
					if (i0 < anz1 && i1 < anz2) {
						PersistentObject source = partitionNodes.get(i0);
						PersistentObject target = nPartitionNodes.get(i1);

						// check if this is a valid edge
						double prob = (Math.exp(-m.get(i0, i1) / sigma) * 100);
						if (prob < threshold)
							break;
						// we don't link on of the source or the target in this
						// frame
						// if they are linked already in this frame
						if (!sources.contains(source)
								&& !targets.contains(target)) {
							// create the new edge and change the tracklet
							PersistentObject newEdge = net.createEdge(source
									+ "-" + target, trackletEdgePartition,
									source, target);
							net.setEdgeWeight(newEdge, prob);

							net.addFeature(source,
									TrackingConstants.FEATURE_ISTRACKLETEND,
									false);
							ends.add(target);
							ends.remove(source);
							String startNode = net
									.getFeatureString(
											source,
											TrackingConstants.FEATURE_TRACKLETSTARTNODE);
							// if we don't already have a start node take the
							// current and save it
							if (startNode == null) {
								startNode = source.getId();
								starts.add(source);
							}
							net.addFeature(
									target,
									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
									startNode);
							// count tracklet size
							PersistentObject trackletStartNode = net
									.getNode(startNode);
							Integer trackletSize = net.getIntegerFeature(
									trackletStartNode,
									TrackingConstants.FEATURE_TRACKLET_SIZE);
							if (trackletSize == null) {
								trackletSize = 2;
							} else {
								trackletSize++;
							}
							net.addFeature(trackletStartNode,
									TrackingConstants.FEATURE_TRACKLET_SIZE,
									trackletSize);

							sources.add(source);
							targets.add(target);
						}
					}
				}
			}
		}
		// store the number of frames we got.
		int numberPart = partitions.size() - 1;
		// create the time reference table
		for (PersistentObject obj : net.getNodes(partitions.get(numberPart)))
			timeReference.put(obj, numberPart);
		exec.checkCanceled();
		// do gap closing
		if (gapClosing) {
			// if we have to delete start or end nodes from the global list, we
			// have to store them temporaly
			HashSet<PersistentObject> delEnd = new HashSet<PersistentObject>();
			HashSet<PersistentObject> delStart = new HashSet<PersistentObject>();
			// Go throw all ends
			for (PersistentObject end : ends) {
				// if the endpoints is not the final endpoint of time
				int timeFrame = timeReference.get(end) + 2;

				double x = (Double) net.getFeatureValue(end, "Centroid X");
				double y = (Double) net.getFeatureValue(end, "Centroid Y");

				// have a look at the Frame after to frames
				double min = Double.POSITIVE_INFINITY;
				PersistentObject gapObj = null;
				for (PersistentObject obj : starts) {
					// filter out nodes that are in former frames
					if (timeReference.get(obj) < timeFrame)
						continue;
					// find the closest node that is within the max Radius
					double x_gap = (Double) net.getFeatureValue(obj,
							"Centroid X");
					double y_gap = (Double) net.getFeatureValue(obj,
							"Centroid Y");
					double weight = Math.sqrt(Math.pow(x - x_gap, 2)
							+ Math.pow(y - y_gap, 2));
					if (weight < min && weight < maxRadius) {
						min = weight;
						gapObj = obj;
					}
				}
				double prob = (Math.exp(-min / sigma) * 100);
				if (null == gapObj || prob < threshold)
					continue;
				// we found a Node, which is similar to our endpoint, but 2
				// Frames away
				// so we craete a new edge between them and change their
				// tracklets
				PersistentObject edge = net.createEdge(end + "-" + gapObj,
						trackletEdgePartition, end, gapObj);
				net.setEdgeWeight(edge, (int) prob);
				net.addFeature(end, TrackingConstants.FEATURE_ISTRACKLETEND,
						false);
				delEnd.add(end);
				delStart.add(gapObj);
				PersistentObject start = net.getNode(net.getFeatureString(end,
						TrackingConstants.FEATURE_TRACKLETSTARTNODE));
				Integer trackletSizeNew = net.getIntegerFeature(start,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				Integer trackletSizeOld = net.getIntegerFeature(gapObj,
						TrackingConstants.FEATURE_TRACKLET_SIZE);
				net.addFeature(start, TrackingConstants.FEATURE_TRACKLET_SIZE,
						trackletSizeNew + trackletSizeOld);
				// for the later tracklet we have to assign a new start node of
				// the tracklet
				boolean found = true;
				for (int i = timeFrame; i < numberPart && found; i++) {
					found = false;
					for (PersistentObject n : net.getNodes(partitions.get(i))) {

						String startNode = net.getFeatureString(n,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE);
						if (gapObj.getId().equals(startNode)) {
							found = true;
							net.addFeature(
									n,
									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
									start.getId());
						}
					}
				}
			}
			// now we can remove the start and end nodes we modified
			ends.removeAll(delEnd);
			starts.removeAll(delStart);
		}

		// store the start and end notes, timestamps to the graph so we can
		// later apply merging and splitting
		net.defineFeature(FeatureTypeFactory.getListType(FeatureTypeFactory
				.getStringType()), TrackMateConstants.START_NODES);
		net.defineFeature(FeatureTypeFactory.getListType(FeatureTypeFactory
				.getStringType()), TrackMateConstants.END_NODES);
		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackMateConstants.TIME_STAMPS);

		List<String> startList = new LinkedList<String>();
		for (PersistentObject obj : starts)
			startList.add(obj.getId());
		List<String> endList = new LinkedList<String>();
		for (PersistentObject obj : ends)
			endList.add(obj.getId());
		for (PersistentObject obj : timeReference.keySet()) {
			net.addFeature(obj, TrackMateConstants.TIME_STAMPS,
					timeReference.get(obj));
		}

		net.addFeature(net, TrackMateConstants.START_NODES, startList);
		net.addFeature(net, TrackMateConstants.END_NODES, endList);

		net.commit();
		return net;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_MaxRadius.saveSettingsTo(settings);
		m_threshold.saveSettingsTo(settings);
		m_gaps.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_MaxRadius.validateSettings(settings);
		m_threshold.validateSettings(settings);
		m_gaps.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_MaxRadius.loadSettingsFrom(settings);
		m_threshold.loadSettingsFrom(settings);
		m_gaps.loadSettingsFrom(settings);
	}
}