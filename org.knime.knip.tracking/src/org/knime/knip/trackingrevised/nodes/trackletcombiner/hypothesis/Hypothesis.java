package org.knime.knip.trackingrevised.nodes.trackletcombiner.hypothesis;

import java.util.LinkedList;
import java.util.List;

import net.imglib2.RealPoint;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.trackingrevised.data.graph.Node;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

public abstract class Hypothesis {

	protected double propability = 0.0;
	protected List<Integer> indices = new LinkedList<Integer>();
	protected int nrTracklets;
	protected IntervalValue bounds;
	protected double lamda1, lamda2, lamda3, deltaS, deltaT, alpha;

	protected ExecutionContext exec;

	@Override
	public String toString() {
		List<Integer> end = new LinkedList<Integer>();
		List<Integer> start = new LinkedList<Integer>();
		for (Integer i : indices) {
			if (i < nrTracklets) {
				end.add(i);
			} else {
				start.add(i - nrTracklets);
			}
		}
		return /* Arrays.toString(this.row) + " " + */this.getClass()
				.getSimpleName()
				+ " start: "
				+ start
				+ " end: "
				+ end
				+ "( "
				+ propability + " )" + " debug[" + getDebugMessages() + "]";
	}

	public void setNumberOfTracklets(int nrTracklets) {
		this.nrTracklets = nrTracklets;
	}

	public void setBounds(IntervalValue bounds) {
		this.bounds = bounds;
	}

	public void setExecutionContext(ExecutionContext exec) {
		this.exec = exec;
	}

	public abstract List<Hypothesis> create(FileStoreFactory factory,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, List<Partition> partitions,
			int currentPartitionIdx, IntervalValue intervalValue)
			throws Exception;

	public abstract void apply() throws Exception;

	private double sanityCheck(double value) {
		if (value < 0 || value > 1) {
			throw new RuntimeException("Probability invalid: " + value);
		}
		// avoid log(0)
		if (value == 0) {
			return 0.00000000000000001;
		}
		return value;
	}

	protected double p_fp(KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node) throws Exception {
		Integer size = net.getIntegerFeature(node,
				TrackingConstants.FEATURE_TRACKLET_SIZE);
		if (size == null)
			try {
				size = net.getIntegerFeature(net.getNode(net.getStringFeature(
						node, TrackingConstants.FEATURE_TRACKLETSTARTNODE)),
						TrackingConstants.FEATURE_TRACKLET_SIZE);
			} catch (Exception e) {
				// only one node
			}
		if (size == null)
			size = 1;
		return sanityCheck(Math.pow(alpha, size));
	}

	protected double p_tp(KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node) throws Exception {
		return sanityCheck(1 - p_fp(net, node));
	}

	protected double p_ini(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, IntervalValue intervalValue)
			throws Exception {
		double prop = TrackingConstants.ETA;
		int dt = (int) new Node(net, node).getTime();
		RealPoint position = new Node(net, node).getPosition();
		if (dt <= deltaT) {
			prop = Math.max(prop, Math.exp(-dt / lamda1));
		}
		double ds = Double.MAX_VALUE;
		for (int d = 0; d < position.numDimensions(); d++) {
			double minDist = Math.min(
					position.getDoublePosition(d)
							- intervalValue.getMinimum()[d],
					intervalValue.getMaximum()[d]
							- position.getDoublePosition(d));
			// ds += minDist * minDist;
			System.out.println("mindist: " + minDist);
			ds = Math.min(ds, minDist);
		}
		if (ds <= deltaS) {
			prop = Math.max(prop, Math.exp(-ds / lamda2));
		}

		return sanityCheck(prop);
	}

	protected double p_term(FileStoreFactory fac,
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node, IntervalValue intervalValue)
			throws Exception {
		double prop = TrackingConstants.ETA;

		int timeIdx = net.getIntegerFeature(net,
				TrackingConstants.NETWORK_FEATURE_TIME_AXIS);

		int maxTime = (int) intervalValue.getMaximum()[timeIdx];

		int dt = maxTime - (int) new Node(net, node).getTime();
		if (dt <= deltaT) {
			prop = Math.max(prop, Math.exp(-dt / lamda1));
		}
		double ds = 0;
		RealPoint position = new Node(net, node).getPosition();
		for (int d = 0; d < position.numDimensions(); d++) {
			double minDist = Math.min(
					position.getDoublePosition(d)
							- intervalValue.getMinimum()[d],
					intervalValue.getMaximum()[d]
							- position.getDoublePosition(d));
			ds += minDist * minDist;
		}
		if (ds < deltaS) {
			prop = Math.max(prop, Math.exp(-ds / lamda2));
		}

		return sanityCheck(prop);
	}

	public void setParameters(double lamda1, double lamda2, double lamda3,
			double deltaS, double deltaT, double alpha) {
		this.lamda1 = lamda1;
		this.lamda2 = lamda2;
		this.lamda3 = lamda3;
		this.deltaS = deltaS;
		this.deltaT = deltaT;
		this.alpha = alpha;
	}

	private StringBuilder debug = new StringBuilder();

	protected void debug(String s) {
		debug.append(" " + s);
	}

	public String getDebugMessages() {
		return debug.toString().trim();
	}

	/**
	 * Get tracklet numbers. Needed because only start nodes have tracklet
	 * nodes.
	 * 
	 * @param net
	 *            the network
	 * @param node
	 *            the node
	 * @return the tracklet number
	 * @throws InvalidFeatureException
	 * @throws PersistenceException
	 */
	protected int getTrackletNumber(
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node) {
		Integer trackNo;
		try {
			trackNo = net.getIntegerFeature(node,
					TrackingConstants.FEATURE_TRACKLET_NUMBER);
			if (trackNo == null) {
				PersistentObject startNode = net.getNode(net.getStringFeature(
						node, TrackingConstants.FEATURE_TRACKLETSTARTNODE));
				return net.getIntegerFeature(startNode,
						TrackingConstants.FEATURE_TRACKLET_NUMBER);
			}
			return trackNo;
		} catch (InvalidFeatureException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Get tracklet start node. Needed to get the real start of a tracklet
	 * nodes.
	 * 
	 * @param net
	 *            the network
	 * @param node
	 *            the node
	 * @return the id of the tracklet start node
	 * @throws InvalidFeatureException
	 * @throws PersistenceException
	 */
	protected String getTrackletStart(
			KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject node) {
		String trackStartNo;
		try {
			trackStartNo = net.getStringFeature(node,
					TrackingConstants.FEATURE_TRACKLETSTARTNODE);
			while (trackStartNo != null) {
				node = net.getNode(trackStartNo);
				trackStartNo = net.getStringFeature(node,
						TrackingConstants.FEATURE_TRACKLETSTARTNODE);
			}
			return node.getId();
		} catch (InvalidFeatureException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Proxy for Math.log or identity for easy exchange.
	 * 
	 * @param number
	 *            the number to get log of
	 * @return log(number) or number
	 */
	protected double log(double number) {
		return Math.log(number);
	}
}
