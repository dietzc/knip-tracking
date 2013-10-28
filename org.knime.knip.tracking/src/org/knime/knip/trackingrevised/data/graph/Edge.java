package org.knime.knip.trackingrevised.data.graph;

import net.imglib2.type.numeric.RealType;

import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;

public class Edge<T extends RealType<T>> extends GraphObject implements Comparable<Edge<T>> {

	private Node<T> start, end;

	public Edge(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject pObj) {
		super(net, pObj);
	}

	public Edge(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject pObj, Node<T> start, Node<T> end) {
		this(net, pObj);
		this.start = start;
		this.end = end;
	}

	public Node<T> getStartNode() {
		return start;
	}

	public Node<T> getEndNode() {
		return end;
	}

	@Override
	public int compareTo(Edge<T> o) {
		return this.getID().compareTo(o.getID());
	}

	public double distanceTo(Edge<T> otherEdge) {
		// 1. feature dist (euclidean)
		double featureDist = featureDistance(otherEdge);
		// 2. distance of end nodes
		double nodeDist = this.getEndNode().distanceTo(otherEdge.getEndNode());
		return featureDist + nodeDist;
	}
}
