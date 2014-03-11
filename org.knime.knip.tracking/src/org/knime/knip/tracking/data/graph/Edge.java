package org.knime.knip.tracking.data.graph;

import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

import fiji.plugin.trackmate.tracking.TrackingUtils;

public class Edge extends GraphObject implements Comparable<Edge> {

	private TrackedNode start, end;

	public Edge(KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject pObj) {
		super(net, pObj);
	}

	public Edge(KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject pObj, TrackedNode start, TrackedNode end) {
		this(net, pObj);
		this.start = start;
		this.end = end;
	}

	public TrackedNode getStartNode() {
		return start;
	}

	public TrackedNode getEndNode() {
		return end;
	}

	@Override
	public int compareTo(Edge o) {
		return this.getID().compareTo(o.getID());
	}

	public double distanceTo(Edge otherEdge) {
		// 1. feature dist (euclidean)
		double featureDist = featureDistance(otherEdge);
		// 2. distance of end nodes
		double nodeDist = Math.sqrt(TrackingUtils.squareDistanceTo(
				this.getEndNode(), otherEdge.getEndNode()));
		return featureDist + nodeDist;
	}
}
