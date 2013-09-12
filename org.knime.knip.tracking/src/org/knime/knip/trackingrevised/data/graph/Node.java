package org.knime.knip.trackingrevised.data.graph;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.RealPoint;
import net.imglib2.type.logic.BitType;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.trackingrevised.util.OffsetHandling;
import org.knime.knip.trackingrevised.util.Permutation;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.Feature;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

public class Node extends GraphObject implements Comparable<Node> {

	private final static String CENTROID_X = "Centroid X";
	private final static String CENTROID_Y = "Centroid Y";
	private final static String CENTROID_Z = "Centroid Z";

	private final static String CENTROID_TIME = "Centroid Time";

	private final List<Edge> outgoing = new LinkedList<Edge>();
	private final List<Edge> incoming = new LinkedList<Edge>();

	public Node(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject pObj) {
		super(net, pObj);
	}

	public RealPoint getPosition() {
		double x = getDoubleFeature(CENTROID_X);
		double y = getDoubleFeature(CENTROID_Y);
		if (!hasFeature(CENTROID_Z))
			return new RealPoint(x, y);
		double z = 0.0;
		try {
			z = getDoubleFeature(CENTROID_Z);
		} catch (Exception e) {
			// no z needed if not available
			// TODO: maybe create setting for coords
		}
		return new RealPoint(x, y, z);
	}

	public void addEdge(Edge edge) {
		if (edge.getStartNode() == this) {
			outgoing.add(edge);
		} else if (edge.getEndNode() == this) {
			incoming.add(edge);
		} else {
			throw new IllegalArgumentException("Edge " + edge
					+ " does not contain node " + this);
		}
	}

	public String getPartition() {
		return "t" + (int) getTime();
	}

	/**
	 * Creates a copy of this node in the {@link TransitionGraph}. The copy is
	 * returned.
	 * 
	 * @param target
	 *            the {@link TransitionGraph}
	 * @return the copy of target.
	 */
	public Node createCopyIn(TransitionGraph target) {
		try {
			// create node in net, but do not add to graph yet
			Node node = target.createNode(this.getID(), false);
			PersistentObject targetnode = node.getPersistentObject();
			for (Feature feature : this.getNetwork().getFeatures()) {
				Object value = this.getNetwork().getFeatureValue(
						this.getPersistentObject(), feature);
				// System.out.println(feature + " = " + value);
				if (value != null) {
					target.getNet().defineFeature(feature);
					target.getNet().addFeature(targetnode, feature.getName(),
							value);
				}
			}
			// now with all features node could finally be added
			target.addNode(node);
			return node;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Edge> getOutgoingEdges() {
		return outgoing;
	}

	@Override
	public int compareTo(Node o) {
		return this.getID().compareTo(o.getID());
	}

	public double distanceTo(Node otherNode) {
		double minDist = Double.MAX_VALUE;

		// check if # followers is equal
		if (getOutgoingEdges().size() != otherNode.getOutgoingEdges().size()) {
			return Double.NaN;
		}

		// no further edges -> no further distance
		if (getOutgoingEdges().size() == 0)
			minDist = 0;

		// 1. feature dist (euclidean)
		double featureDist = featureDistance(otherNode);
		// 2. permutate otherEdges for all combinations
		List<Edge[]> otherEdgesPermutations = Permutation
				.getAllPermutations(otherNode.getOutgoingEdges());
		for (Edge[] permutation : otherEdgesPermutations) {
			int index = 0;
			double dist = 0.0;
			for (Edge edge : getOutgoingEdges()) {
				Edge otherEdge = permutation[index++];
				dist += edge.distanceTo(otherEdge);
			}
			if (dist < minDist) {
				minDist = dist;
			}
		}
		return featureDist + minDist;
	}

	public double getTime() {
		return getDoubleFeature(CENTROID_TIME);
	}

	public long[] getOffset() {
		return OffsetHandling
				.decode(getStringFeature(TrackingConstants.FEATURE_BITMASK_OFFSET));
	}

	public Rectangle2D getImageRectangle() {
		try {
			ImgPlusValue<?> ipv = (ImgPlusValue<?>) getNetwork()
					.getFeatureCell(
							FileStoreFactory
									.createNotInWorkflowFileStoreFactory(),
							getPersistentObject(),
							TrackingConstants.FEATURE_BITMASK);
			long[] offset = getOffset();
			return new Rectangle2D.Double(ipv.getMinimum()[0] + offset[0],
					ipv.getMinimum()[1] + offset[1], ipv.getDimensions()[0],
					ipv.getDimensions()[1]);
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (InvalidFeatureException e) {
			e.printStackTrace();
		}
		return new Rectangle2D.Double();
	}

	@SuppressWarnings("unchecked")
	public ImgPlusValue<BitType> getBitmask() {
		try {
			return (ImgPlusValue<BitType>) getNetwork().getFeatureCell(
					FileStoreFactory.createNotInWorkflowFileStoreFactory(),
					getPersistentObject(), TrackingConstants.FEATURE_BITMASK);
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (InvalidFeatureException e) {
			e.printStackTrace();
		}
		return null;
	}

	public double euclideanDistanceTo(Node targetNode) {
		RealPoint myPosition = this.getPosition();
		RealPoint targetPosition = targetNode.getPosition();
		double dist = 0.0;
		for (int d = 0; d < myPosition.numDimensions(); d++) {
			dist += (myPosition.getDoublePosition(d) - targetPosition
					.getDoublePosition(d))
					* (myPosition.getDoublePosition(d) - targetPosition
							.getDoublePosition(d));
		}
		return Math.sqrt(dist);
	}

	public double[] getDistanceVector() {
		double[] vec = new double[getDoubleFeatures().size()];
		Iterator<String> it = getDoubleFeatures().iterator();
		for (int i = 0; i < vec.length; i++) {
			String featName = it.next();
			vec[i] = getDoubleFeature(featName);
			for (Edge edge : outgoing) {
				Node endNode = edge.getEndNode();
				vec[i] -= endNode.getDoubleFeature(featName);
			}
		}
		return vec;
	}

	// Nodes might exist as copy in different transition graphs, make them
	// comparable
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
			return getID().equals(((Node) obj).getID());
		}
		return super.equals(obj);
	}

	// same as above
	@Override
	public int hashCode() {
		return getID().hashCode();
	}

	public List<Edge> getIncomingEdges() {
		return incoming;
	}
}
