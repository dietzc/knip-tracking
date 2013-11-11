package org.knime.knip.tracking.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;

import net.imglib2.RealLocalizable;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class InitalizationHypothesis extends Hypothesis {

	private String trackletStart;

	@Override
	public List<Hypothesis> create(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, List<Partition> partitions,
			int currentPartitionIdx, IntervalValue intervalValue)
			throws Exception {
		if (net.getStringFeature(node,
				TrackingConstants.FEATURE_TRACKLETSTARTNODE) != null)
			return new LinkedList<Hypothesis>();

		int timeAxisIndex = net.getIntegerFeature(net,
				TrackingConstants.NETWORK_FEATURE_TIME_AXIS);

		boolean possible = currentPartitionIdx <= deltaT;
		if (!possible) {
			long[] min = intervalValue.getMinimum();
			long[] max = intervalValue.getMaximum();

			RealLocalizable position = new TrackedNode(net, node);

			for (int d = 0; d < min.length; d++) {
				if (d == timeAxisIndex)
					continue;
				if (Math.abs(position.getDoublePosition(d) - min[d]) <= deltaS) {
					possible = true;
					break;
				}
				if (Math.abs(position.getDoublePosition(d) - max[d]) <= deltaS) {
					possible = true;
					break;
				}
			}

		}
		if (possible) {
			int trackletNo;
			trackletNo = net.getIntegerFeature(node,
					TrackingConstants.FEATURE_TRACKLET_NUMBER);

			this.trackletStart = getTrackletStart(net, node);
			this.indices.add(nrTracklets + trackletNo);

			// log(P_ini(X_k))+0.5*log(P_TP(X_k))
			this.propability = log(p_ini(fac, net, node, intervalValue)) + 0.5
					* log(p_tp(net, node));

		}
		List<Hypothesis> list = new LinkedList<Hypothesis>();
		if (possible)
			list.add(this);
		return list;
	}

	@Override
	public void apply() throws Exception {
		// do nothing
	}

	public String toString() {
		return super.toString() + " " + trackletStart;
	};
}
