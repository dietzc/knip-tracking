package org.knime.knip.tracking.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class TerminationHypothesis extends Hypothesis {

	private String trackletStart;

	@Override
	public List<Hypothesis> create(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, List<Partition> partitions,
			int currentPartitionIdx, IntervalValue intervalValue)
			throws Exception {
		if (net.getBooleanFeature(node, TrackingConstants.FEATURE_ISTRACKLETEND) != true)
			return new LinkedList<Hypothesis>();

		int timeAxisIndex = net.getIntegerFeature(net,
				TrackingConstants.NETWORK_FEATURE_TIME_AXIS);

		int maxTime = (int) intervalValue.getMaximum()[timeAxisIndex];

		debug("maxtime: " + maxTime);

		boolean possible = currentPartitionIdx >= (maxTime - deltaT);
		debug("possible:" + possible + " " + currentPartitionIdx + ">="
				+ (maxTime - deltaT) + " " + deltaT);
		if (!possible) {
			long[] min = intervalValue.getMinimum();
			long[] max = intervalValue.getMaximum();

			TrackedNode position = new TrackedNode(net, node);

			for (int d = 0; d < position.numDimensions(); d++) {
				if (Math.abs(position.getDoublePosition(d) - min[d]) <= deltaS) {
					possible = true;
					debug("a: " + position.getDoublePosition(d) + "-" + min[d]);
					break;
				}
				if (Math.abs(position.getDoublePosition(d) - max[d]) <= deltaS) {
					possible = true;
					debug("b");
					break;
				}
			}

		}
		if (possible) {
			Integer trackletNo = net.getIntegerFeature(node,
					TrackingConstants.FEATURE_TRACKLET_NUMBER);

			if (trackletNo == null) {
				trackletNo = net.getIntegerFeature(net.getNode(net
						.getStringFeature(node,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE)),
						TrackingConstants.FEATURE_TRACKLET_NUMBER);
			}

			this.trackletStart = getTrackletStart(net, node);

			this.indices.add(trackletNo);

			// log(P_term(X_k))+0.5*log(P_TP(X_k))
			this.propability = log(p_term(fac, net, node, intervalValue)) + 0.5
					* log(p_tp(net, node));
		}
		List<Hypothesis> list = new LinkedList<Hypothesis>();
		if (possible) {
			list.add(this);
		}
		return list;
	}

	@Override
	public void apply() throws Exception {
		// do nothing
	}

	@Override
	public String toString() {
		return super.toString() + " " + trackletStart;
	}

}
