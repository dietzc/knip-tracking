package org.knime.knip.trackingrevised.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class LinkHypothesis extends Hypothesis {

	private KPartiteGraph<PersistentObject, Partition> net;
	private PersistentObject trackletEnd, trackletStart;

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

		for (PersistentObject edge : net.getIncidentEdges(node,
				distanceEdgePartition)) {
			PersistentObject otherNode = null;
			for (PersistentObject n : net.getIncidentNodes(edge)) {
				if (!n.equals(node)) {
					otherNode = n;
				}
			}
			// assure it's a tracklet end and it's before node
			if (!net.getBooleanFeature(otherNode,
					TrackingConstants.FEATURE_ISTRACKLETEND)
					|| partitions.indexOf(net.getPartitions(otherNode)
							.iterator().next()) >= currentPartitionIdx)
				continue;
			// so we got a start node -> create hypothesis
			Integer endNodeNo = net.getIntegerFeature(otherNode,
					TrackingConstants.FEATURE_TRACKLET_NUMBER);
			if (endNodeNo == null) {
				// first tracklet has more than one node, so get number of first
				// node
				endNodeNo = net.getIntegerFeature(net.getNode(net
						.getStringFeature(otherNode,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE)),
						TrackingConstants.FEATURE_TRACKLET_NUMBER);
			}
			LinkHypothesis lh = new LinkHypothesis();
			lh.setBounds(bounds);
			lh.setNumberOfTracklets(nrTracklets);

			// endnode on the left side, start node on the right matrix side
			lh.indices.add(endNodeNo);
			lh.indices.add(nrTracklets + startNodeNo);

			double dist = net.getEdgeWeight(edge);
			double plink = Math.exp(-dist / lamda3);

			if (plink == 0.0) {
				// avoid log(0)
				plink = 0.00000000000000001;
			}

			lh.propability = log(plink) + 0.5 * Math.log(p_tp(net, node)) + 0.5
					* log(p_tp(net, otherNode));

			lh.net = net;
			// remember, we looked backwards
			lh.trackletEnd = otherNode;
			lh.trackletStart = node;

			list.add(lh);
		}

		return list;
	}

	@Override
	public void apply() throws Exception {
		Partition partition = net
				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);
		net.createEdge(trackletEnd.getId() + "_" + trackletStart.getId(),
				partition, trackletEnd, trackletStart);
		// adjust isTrackletEnd
		net.addFeature(trackletEnd, TrackingConstants.FEATURE_ISTRACKLETEND,
				false);
		// adjust startNodes
		String startNode = getTrackletStart(net, trackletEnd);
		net.addFeature(trackletStart,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE, startNode);
	}

	@Override
	public String toString() {
		return super.toString() + " " + getTrackletStart(net, trackletEnd)
				+ "->" + trackletStart.getId();
	}

	protected PersistentObject getStartingTracklet() {
		return trackletStart;
	}

	protected PersistentObject getEndingTracklet() {
		return trackletEnd;
	}

}
