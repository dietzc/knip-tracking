package org.knime.knip.trackingrevised.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

public class MergeHypothesis extends Hypothesis {

	private class Edge {
		public Edge(PersistentObject node, double dist) {
			this.node = node;
			this.dist = dist;
		}

		PersistentObject node;
		double dist;
	}

	private KPartiteGraph<PersistentObject, Partition> net;
	private PersistentObject trackletStart;
	private PersistentObject trackletEnd1;
	private PersistentObject trackletEnd2;

	@Override
	public List<Hypothesis> create(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, List<Partition> partitions,
			int currentPartitionIdx, IntervalValue intervalValue)
			throws Exception {
		if (net.getStringFeature(node,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE) != null) {
			return new LinkedList<Hypothesis>();
		}
		List<Hypothesis> list = new LinkedList<Hypothesis>();

		Partition distanceEdgePartition = net
				.getPartition(TrackingConstants.DISTANCE_EDGE_PARTITION);

		int startNodeNo = net.getIntegerFeature(node,
				TrackingConstants.FEATURE_TRACKLET_NUMBER);

		List<Edge> candidates = new LinkedList<Edge>();

		for (PersistentObject edge : net.getIncidentEdges(node,
				distanceEdgePartition)) {
			PersistentObject otherNode = null;
			for (PersistentObject n : net.getIncidentNodes(edge)) {
				if (!n.equals(node)) {
					otherNode = n;
				}
			}
			if (!net.getBooleanFeature(otherNode,
					TrackingConstants.FEATURE_ISTRACKLETEND)
					|| partitions.indexOf(net.getPartitions(otherNode)
							.iterator().next()) >= currentPartitionIdx)
				continue;
			// so we got a start node -> candidate
			// TODO: range check
			double dist = net.getEdgeWeight(edge);
			candidates.add(new Edge(otherNode, dist));
		}

		ListIterator<Edge> it = candidates.listIterator();

		while (it.hasNext()) {
			Edge edge = it.next();
			ListIterator<Edge> it2 = candidates.listIterator(it.nextIndex());
			while (it2.hasNext()) {
				Edge edge2 = it2.next();
				if (edge == edge2)
					continue;
				int endNodeNo1 = getTrackletNumber(net, edge.node);
				int endNodeNo2 = getTrackletNumber(net, edge2.node);
				MergeHypothesis dh = new MergeHypothesis();
				dh.setBounds(bounds);
				dh.setNumberOfTracklets(nrTracklets);

				// end on the left, start on the right
				dh.indices.add(endNodeNo1);
				dh.indices.add(endNodeNo2);
				dh.indices.add(nrTracklets + startNodeNo);

				double dist = edge.dist;
				double dist2 = edge2.dist;

				// some parameter 'tweaking'
				double pdiv = Math.exp(-(dist + dist2) / (2 * lamda3));

				if (pdiv == 0.0) {
					// avoid log(0)
					pdiv = 0.00000000000000001;
				}

				dh.propability = log(pdiv) + 0.5 * log(p_tp(net, node)) + 0.5
						* log(p_tp(net, edge.node)) + 0.5
						* log(p_tp(net, edge2.node));

				dh.net = net;
				dh.trackletStart = node;
				dh.trackletEnd1 = edge.node;
				dh.trackletEnd2 = edge2.node;

				list.add(dh);
			}
		}

		return list;
	}

	@Override
	public void apply() throws Exception {
		Partition trackletEdgePartition = net
				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);
		net.createEdge(trackletEnd1.getId() + "_" + trackletStart.getId(),
				trackletEdgePartition, trackletEnd1, trackletStart);
		net.createEdge(trackletEnd2.getId() + "_" + trackletStart.getId(),
				trackletEdgePartition, trackletEnd2, trackletStart);
		// adjust isTrackletEnd
		net.addFeature(trackletEnd1, TrackingConstants.FEATURE_ISTRACKLETEND,
				false);
		net.addFeature(trackletEnd2, TrackingConstants.FEATURE_ISTRACKLETEND,
				false);
		// adjust startNodes
		// we take the lexicographical smaller one for everything!
		String startNode = getTrackletStart(net, trackletEnd1);
		String startNode2 = getTrackletStart(net, trackletEnd2);
		if (startNode2.compareTo(startNode) < 0) {
			startNode = startNode2;
			// end2 < end1, so adjust end1
			net.addFeature(trackletEnd1,
					TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);
		} else {
			// end1 < end2, so adjust end2
			net.addFeature(trackletEnd2,
					TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);
		}
		// for the end part
		net.addFeature(trackletStart,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);

	}

	@Override
	public String toString() {
		try {
			return super.toString() + " " + getTrackletStart(net, trackletEnd1)
					+ "+" + getTrackletStart(net, trackletEnd2) + "->"
					+ trackletStart.getId() + "      TrackletEnd1: "
					+ net.getDoubleFeature(trackletEnd1, "Centroid Time")
					+ " TrackletEnd2: "
					+ net.getDoubleFeature(trackletEnd2, "Centroid Time")
					+ " TrackletStart: "
					+ net.getDoubleFeature(trackletStart, "Centroid Time");
		} catch (InvalidFeatureException e) {
			e.printStackTrace();
			return "aasdfg";
		} catch (PersistenceException e) {
			e.printStackTrace();
			return "aasdfg";
		}
	}

}