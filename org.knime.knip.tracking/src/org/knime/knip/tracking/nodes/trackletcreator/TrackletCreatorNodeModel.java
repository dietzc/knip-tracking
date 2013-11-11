package org.knime.knip.tracking.nodes.trackletcreator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.imglib2.collection.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.util.PartitionComparator;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of TrackletCreator. Creates Tracklets (parts
 * of a trajectory) out of a given segmentation as graphs.
 * 
 * @author Stephan Sellien
 */
public class TrackletCreatorNodeModel extends KPartiteGraphNodeModel {
	NodeLogger logger = NodeLogger.getLogger(TrackletCreatorNodeModel.class);

	private final static String CFGKEY_TRESHOLD = "treshold";

	static SettingsModelIntegerBounded createTresholdSetting() {
		return new SettingsModelIntegerBounded(CFGKEY_TRESHOLD,
				TrackingConstants.MIN_TRACKLET_PROBABILITY, 1, 100);
	}

	private final static String CFGKEY_SAFETY_RADIUS = "safetyRadius";

	static SettingsModelDouble createSafetyRadiusSetting() {
		return new SettingsModelDouble(CFGKEY_SAFETY_RADIUS, 50);
	}

	private SettingsModelIntegerBounded tresholdSetting = createTresholdSetting();

	private SettingsModelDouble safetyRadius = createSafetyRadiusSetting();

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

		Partition distanceEdgePartition = net
				.getPartition(TrackingConstants.DISTANCE_EDGE_PARTITION);

		// sort to ensure correct order
		Collections.sort(partitions, new PartitionComparator());

		exec.setMessage("Search for edge candidates..");

		// target -> source for lookup
		// LinkedHashMap to persist temporal context.
		Map<PersistentObject, List<EdgeCandidate>> edgeCandidates = new LinkedHashMap<PersistentObject, List<EdgeCandidate>>();
		KDTree<TrackedNode> tree = null, nextTree = null;
		for (int p = 0; p < partitions.size() - 1; p++) {
			Partition partition = partitions.get(p);
			Partition nextPartition = partitions.get(p + 1);
			if (nextTree != null) {
				tree = nextTree;
			} else {
				tree = createKDTree(net, partition);
			}
			nextTree = createKDTree(net, nextPartition);
			for (PersistentObject target : net.getNodes(nextPartition)) {
				List<EdgeCandidate> candidates = new LinkedList<EdgeCandidate>();
				edgeCandidates.put(target, candidates);
				for (PersistentObject edge : net.getIncomingEdges(target)) {
					if (!net.getEdgePartitions(edge).contains(
							distanceEdgePartition))
						continue;
					Collection<PersistentObject> collection = net
							.getIncidentNodes(edge);
					PersistentObject source = null;
					for (PersistentObject node : collection) {
						if (node != target) {
							source = node;
						}
					}
					// source found and in previous partition
					if (source == null
							|| !net.getPartitions(source).contains(partition)) {
						continue;
					}

					// edge weight contains dist
					double dist = net.getEdgeWeight(edge);

					// is this really a unique candidate? needed to ensure
					// splits / merges aren't undetectable after
					// only is a candidate if nothing is in 2*euclidean_dist
					// radius
					TrackedNode targetNode = new TrackedNode(net, target);
					TrackedNode sourceNode = new TrackedNode(net, source);

					// if(sourceNode.getID().matches("670")) {
					// System.out.println(edge + " " + dist);
					// }

					double searchRadius = sourceNode
							.euclideanDistanceTo(targetNode) * 2;

					// safety margin
					searchRadius = Math.max(searchRadius,
							safetyRadius.getDoubleValue());

					// around target in partition -> merges
					RadiusNeighborSearchOnKDTree<TrackedNode> search = new RadiusNeighborSearchOnKDTree<TrackedNode>(
							tree);
					search.search(targetNode, searchRadius, false);
					int noTargetNeighbors = search.numNeighbors();
					boolean found = false;
					for (int i = 0; i < search.numNeighbors(); i++) {
						if (sourceNode.getID().equals(
								search.getSampler(i).get().getID())) {
							found = true;
							break;
						}
					}
					if (!found) {
						System.err.println("target partner not in radius!"
								+ target);
					}
					// around source in nextpartition -> splits
					search = new RadiusNeighborSearchOnKDTree<TrackedNode>(nextTree);
					search.search(sourceNode, searchRadius, false);
					int noSourceNeighbors = search.numNeighbors();

					found = false;
					for (int i = 0; i < search.numNeighbors(); i++) {
						if (targetNode.getID().equals(
								search.getSampler(i).get().getID())) {
							found = true;
							break;
						}
					}
					if (!found) {
						System.err.println("source partner not in radius!"
								+ target);
					}

					if (noTargetNeighbors == 0 || noSourceNeighbors == 0)
						throw new Exception("shit happened: "
								+ noTargetNeighbors + " and "
								+ noSourceNeighbors);

					// nothing else in range -> connect that stuff
					if (noTargetNeighbors == 1 && noSourceNeighbors == 1)
						candidates.add(new EdgeCandidate(source, target, dist));
				}
			}

			exec.checkCanceled();
			exec.setProgress((double) p / partitions.size());
		}

		double sigma = net.getDoubleFeature(net,
				TrackingConstants.NETWORK_FEATURE_STDEV);

		System.out.println(sigma);

		// final tracklet edges
		Partition trackletEdgePartition = net.createPartition(
				TrackingConstants.TRACKLET_EDGE_PARTITION, PartitionType.EDGE);

		// trackletstartnode
		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.FEATURE_TRACKLETSTARTNODE);
		// tracklet size
		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.FEATURE_TRACKLET_SIZE);

		Set<PersistentObject> sources = new HashSet<PersistentObject>();
		Set<PersistentObject> targets = new HashSet<PersistentObject>();

		for (PersistentObject target : edgeCandidates.keySet()) {
			List<EdgeCandidate> list = edgeCandidates.get(target);
			Collections.sort(list);
			ListIterator<EdgeCandidate> iterator = list.listIterator();
			while (iterator.hasNext()) {
				EdgeCandidate ec = iterator.next();
				// if source has no outgoing edges && target no incoming
				double prob = (Math.exp(-ec.dist / sigma) * 100);
				// System.out.println(ec.source + " -> " + ec.target + " ^= "
				// + prob + " \t\t" + ec.dist);
				if (prob < tresholdSetting.getIntValue())
					break;
				if (!sources.contains(ec.source)
						&& !targets.contains(ec.target)) {
					PersistentObject edge = net.createEdge(ec.source + "-"
							+ ec.target, trackletEdgePartition, ec.source,
							ec.target);
					net.setEdgeWeight(edge, (int) prob);
					net.addFeature(ec.source,
							TrackingConstants.FEATURE_ISTRACKLETEND, false);
					String startNode = net.getFeatureString(ec.source,
							TrackingConstants.FEATURE_TRACKLETSTARTNODE);
					if (startNode == null) {
						startNode = ec.source.getId();
					}
					net.addFeature(ec.target,
							TrackingConstants.FEATURE_TRACKLETSTARTNODE,
							startNode);
					// count tracklet size
					PersistentObject trackletStartNode = net.getNode(startNode);
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

					sources.add(ec.source);
					targets.add(ec.target);
				}
			}
		}
		net.commit();
		return net;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		tresholdSetting.saveSettingsTo(settings);
		safetyRadius.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		tresholdSetting.validateSettings(settings);
		safetyRadius.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		tresholdSetting.loadSettingsFrom(settings);
		safetyRadius.loadSettingsFrom(settings);
	}

	private KDTree<TrackedNode> createKDTree(
			KPartiteGraph<PersistentObject, Partition> net, Partition partition)
			throws PersistenceException {
		List<TrackedNode> objects = new LinkedList<TrackedNode>();
		List<TrackedNode> positions = new LinkedList<TrackedNode>();
		for (PersistentObject n : net.getNodes(partition)) {
			TrackedNode node = new TrackedNode(net, n);
			objects.add(node);
			positions.add(node);
		}
		return new KDTree<TrackedNode>(objects, positions);
	}
}

class EdgeCandidate implements Comparable<EdgeCandidate> {
	public EdgeCandidate(PersistentObject source, PersistentObject target,
			double dist) {
		this.source = source;
		this.target = target;
		this.dist = dist;
	}

	PersistentObject source, target;
	double dist, prop;

	@Override
	public int compareTo(EdgeCandidate o) {
		// prop version
		// return (int) Math.signum(prop-o.prop);
		return (int) Math.signum(dist - o.dist);
	}
}