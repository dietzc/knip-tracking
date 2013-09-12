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

public class DividingHypothesis extends Hypothesis {

	private class Edge {
		public Edge(PersistentObject node, double dist) {
			this.node = node;
			this.dist = dist;
		}

		PersistentObject node;
		double dist;
	}

	private KPartiteGraph<PersistentObject, Partition> net;
	private PersistentObject trackletEnd;
	private PersistentObject trackletStart;
	private PersistentObject trackletStart2;

	@Override
	public List<Hypothesis> create(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, List<Partition> partitions,
			int currentPartitionIdx, IntervalValue intervalValue)
			throws Exception {
		if (net.getBooleanFeature(node, TrackingConstants.FEATURE_ISTRACKLETEND) != true) {
			return new LinkedList<Hypothesis>();
		}
		List<Hypothesis> list = new LinkedList<Hypothesis>();

		Partition distanceEdgePartition = net
				.getPartition(TrackingConstants.DISTANCE_EDGE_PARTITION);

		String endNodeString = net.getStringFeature(node,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE);
		PersistentObject endNode = node;
		if (endNodeString != null)
			endNode = net.getNode(endNodeString);
		int endNodeNo = net.getIntegerFeature(endNode,
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
			if (net.getStringFeature(otherNode,
					TrackingConstants.FEATURE_TRACKLETSTARTNODE) != null
					|| partitions.indexOf(net.getPartitions(otherNode)
							.iterator().next()) <= currentPartitionIdx)
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
				int startNodeNo1 = net.getIntegerFeature(edge.node,
						TrackingConstants.FEATURE_TRACKLET_NUMBER);
				int startNodeNo2 = net.getIntegerFeature(edge2.node,
						TrackingConstants.FEATURE_TRACKLET_NUMBER);
				DividingHypothesis dh = new DividingHypothesis();
				dh.setBounds(bounds);
				dh.setNumberOfTracklets(nrTracklets);

				dh.indices.add(endNodeNo);
				dh.indices.add(nrTracklets + startNodeNo1);
				dh.indices.add(nrTracklets + startNodeNo2);

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
				dh.trackletEnd = node;
				dh.trackletStart = edge.node;
				dh.trackletStart2 = edge2.node;

				list.add(dh);
			}
		}

		return list;
	}

	@Override
	public void apply() throws Exception {
		Partition trackletEdgePartition = net
				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);
		net.createEdge(trackletEnd.getId() + "_" + trackletStart.getId(),
				trackletEdgePartition, trackletEnd, trackletStart);
		net.createEdge(trackletEnd.getId() + "_" + trackletStart2.getId(),
				trackletEdgePartition, trackletEnd, trackletStart2);
		// adjust isTrackletEnd
		net.addFeature(trackletEnd, TrackingConstants.FEATURE_ISTRACKLETEND,
				false);
		// adjust startNodes
		String startNode = getTrackletStart(net, trackletEnd);
		net.addFeature(trackletStart,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);
		net.addFeature(trackletStart2,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);
	}

	@Override
	public String toString() {
		return super.toString() + " " + getTrackletStart(net, trackletEnd)
				+ "->" + trackletStart.getId() + "+" + trackletStart2.getId();
	}

}