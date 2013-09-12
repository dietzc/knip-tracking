package org.knime.knip.trackingrevised.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class FalsePositiveHypothesis extends Hypothesis {

	private KPartiteGraph<PersistentObject, Partition> net;
	private PersistentObject node;

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

		Integer trackletNo = net.getIntegerFeature(node,
				TrackingConstants.FEATURE_TRACKLET_NUMBER);
		if (trackletNo == null) {
			trackletNo = net.getIntegerFeature(net.getNode(net
					.getStringFeature(node,
							TrackingConstants.FEATURE_TRACKLETSTARTNODE)),
					TrackingConstants.FEATURE_TRACKLET_NUMBER);
		}

		this.indices.add(trackletNo);
		this.indices.add(nrTracklets + trackletNo);

		this.propability = log(p_fp(net, node));

		// remember net and node
		this.net = net;
		this.node = node;

		List<Hypothesis> list = new LinkedList<Hypothesis>();
		list.add(this);
		return list;
	}

	@Override
	public void apply() throws Exception {
		net.addFeature(node, TrackingConstants.FEATURE_TRACKLET_NUMBER, -1);
	}

	@Override
	public String toString() {
		return super.toString() + " " + getTrackletStart(net, node);
	}

}
