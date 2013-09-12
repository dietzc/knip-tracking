package org.knime.knip.trackingrevised.data.graph;

import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class Edge extends GraphObject implements Comparable<Edge> {

	private Node start, end;

	public Edge(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject pObj) {
		super(net, pObj);
	}

	public Edge(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject pObj, Node start, Node end) {
		this(net, pObj);
		this.start = start;
		this.end = end;
	}

	public Node getStartNode() {
		return start;
	}

	public Node getEndNode() {
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
		double nodeDist = this.getEndNode().distanceTo(otherEdge.getEndNode());
		return featureDist + nodeDist;
	}
}
