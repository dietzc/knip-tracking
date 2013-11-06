package org.knime.knip.trackingrevised.data.graph;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.trackingrevised.util.OffsetHandling;
import org.knime.knip.trackingrevised.util.PartitionComparatorString;
import org.knime.knip.trackingrevised.util.Permutation;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.algorithm.search.dfs.WeakConnectedComponent;
import org.knime.network.core.api.Feature;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.core.feature.FeatureTypeFactory;

import fiji.plugin.trackmate.tracking.TrackingUtils;

/**
 * A transition graph based on a {@link KPartiteGraph}.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class TransitionGraph {
	private KPartiteGraph<PersistentObject, Partition> net;

	private static int counter = 0;

	private Map<String, List<TrackedNode>> nodes = new HashMap<String, List<TrackedNode>>();

	private LinkedList<String> partitions = new LinkedList<String>();

	private List<Edge> edges = new LinkedList<Edge>();

	public TransitionGraph() throws PersistenceException {
		this.net = GraphFactory.createNet("transGraph" + (counter++));
	}

	public TransitionGraph(KPartiteGraph<PersistentObject, Partition> net) {
		this.net = net;

		try {

			for (Partition partition : net.getPartitions()) {
				if (partition.getId().startsWith("t")) {
					addPartition(partition.getId());
				}
			}

			Map<String, TrackedNode> nodemap = new HashMap<String, TrackedNode>();
			for (PersistentObject node : net.getNodes()) {
				TrackedNode n = new TrackedNode(net, node);
				addNode(n);
				nodemap.put(n.getID(), n);
			}
			for (PersistentObject edge : net.getEdges()) {
				TrackedNode startNode = null;
				TrackedNode endNode = null;
				for (PersistentObject node : net.getIncidentNodes(edge)) {
					TrackedNode n = nodemap.get(node.getId());
					if (net.isDirected(edge)) {
						if (net.isSource(edge, node))
							startNode = n;
						if (net.isTarget(edge, node))
							endNode = n;
					} else {
						if (startNode == null) {
							startNode = n;
						} else {
							if (n.frame() < startNode.frame()) {
								endNode = startNode;
								startNode = n;
							} else {
								endNode = n;
							}
						}
					}
				}
				if (startNode == null || endNode == null)
					throw new IllegalArgumentException("Network " + net
							+ " seems broken.");
				Edge e = new Edge(net, edge, startNode, endNode);
				addEdge(e);
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
	}

	public TransitionGraph(KPartiteGraphView<PersistentObject, Partition> view)
			throws PersistenceException {
		this(GraphFactory.createIncrementalNet(view));
	}

	/**
	 * Copy constructor.
	 * 
	 * @param tg
	 *            the graph to copy
	 * @throws PersistenceException
	 *             from network.
	 */
	public TransitionGraph(TransitionGraph tg) throws PersistenceException {
		this();
		for (String partition : tg.partitions) {
			addPartition(partition);
			for (TrackedNode node : tg.getNodes(partition)) {
				node.createCopyIn(this);
			}
		}
		for (Edge e : tg.edges) {
			TrackedNode start = getCopiedNode(e.getStartNode());
			TrackedNode end = getCopiedNode(e.getEndNode());
			createEdge(start, end);
		}
	}

	public TransitionGraph(List<String> partitions) throws PersistenceException {
		this();
		for (String partition : partitions) {
			addPartition(partition);
		}
	}

	public TrackedNode createNode(String id, boolean add)
			throws PersistenceException {
		TrackedNode node = new TrackedNode(net, net.createNode(id));
		if (add)
			addNode(node);
		return node;
	}

	public TrackedNode createNode(String id) throws PersistenceException {
		return createNode(id, true);
	}

	public void addNode(TrackedNode node) {
		List<TrackedNode> nodelist = nodes.get(node.getPartition());
		if (nodelist == null) {
			// no list found => new partition
			addPartition(node.getPartition());
			nodelist = nodes.get(node.getPartition());
		}
		nodelist.add(node);
		try {
			net.assignNodeToPartition(node.getPersistentObject(),
					net.getPartition(node.getPartition()));
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		// System.out.println("added " + node + " @ " + node.getPartition());
	}

	public Edge createEdge(TrackedNode start, TrackedNode end)
			throws PersistenceException {
		String name = start.getID() + "_" + end.getID();
		PersistentObject edge = net.createEdge(name,
				start.getPersistentObject(), end.getPersistentObject());
		Edge e = new Edge(net, edge, start, end);
		addEdge(e);
		return e;
	}

	private void addEdge(Edge e) {
		edges.add(e);
		e.getStartNode().addEdge(e);
		e.getEndNode().addEdge(e);
	}

	public void addPartition(String partition) {
		if (partitions.contains(partition))
			return;
		List<TrackedNode> nodelist = new LinkedList<TrackedNode>();
		nodes.put(partition, nodelist);
		partitions.add(partition);
		Collections.sort(partitions, new PartitionComparatorString());
		try {
			if (net.getPartition(partition) == null)
				net.createPartition(partition, PartitionType.NODE);
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
	}

	public KPartiteGraph<PersistentObject, Partition> getNet() {
		return net;
	}

	public String getFirstPartition() {
		return partitions.getFirst();
	}

	public String getLastPartition() {
		return partitions.getLast();
	}

	public String toString() {
		StringBuilder output = new StringBuilder();
		for (String partition : partitions) {
			output.append(nodes.get(partition).size() + " ");
		}
		return output.toString();
	}

	public double distanceTo(final TransitionGraph otherGraph) {
		double minDist = Double.MAX_VALUE;
		// use otherGraph as 'pattern' which will be permutated
		List<TrackedNode[]> firstPartNodes = Permutation
				.getAllPermutations(otherGraph.nodes.get(otherGraph
						.getFirstPartition()));
		List<TrackedNode> tfpNodes = nodes.get(getFirstPartition());
		for (TrackedNode[] permutation : firstPartNodes) {
			double dist = 0.0;
			int index = 0;

			for (TrackedNode node : tfpNodes) {
				TrackedNode otherNode = permutation[index++];
				dist += TrackingUtils.squareDistanceTo(node,otherNode);
			}

			if (dist < minDist) {
				minDist = dist;
			}
		}
		return minDist;
	}

	/**
	 * Create the assumed sure edges, e.g. if there is a 1 1 connection.
	 * 
	 */
	public void addDefaultEdges() {
		// TODO: rethink
		return;
		/*
		 * if (nodes.get(getFirstPartition()).size() == 1) { Node startNode =
		 * nodes.get(getFirstPartition()).get(0); for (Node endNode :
		 * nodes.get(getLastPartition())) { try { this.createEdge(startNode,
		 * endNode); } catch (PersistenceException e) { e.printStackTrace(); } }
		 * }
		 */
	}

	public String[] getFeatureNames() {
		List<String> names = new LinkedList<String>();
		try {
			for (Feature feature : net.getFeatures()) {
				if (feature.getType().isNumeric()) {
					names.add(feature.getName());
				}
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return names.toArray(new String[names.size()]);
	}

	public double[] getDistanceVector() {
		double[] featureDist = new double[getFeatureNames().length];
		for (TrackedNode node : nodes.get(getFirstPartition())) {
			double[] distVec = node.getDistanceVector();
			for (int i = 0; i < featureDist.length; i++) {
				featureDist[i] += distVec[i];
			}
		}
		return featureDist;
	}

	public ImgPlus<UnsignedIntType> renderImage() {
		final int border = 10;

		Rectangle2D rect = null;
		for (String partition : partitions) {
			for (TrackedNode node : nodes.get(partition)) {
				if (rect == null) {
					rect = node.getImageRectangle();
				} else {
					rect.add(node.getImageRectangle());
				}
			}
		}
		if (rect == null)
			return new ImgPlus<UnsignedIntType>(
					new ArrayImgFactory<UnsignedIntType>().create(new long[] {
							0, 0 }, new UnsignedIntType()));
		int imgWidth = (int) Math.ceil(rect.getWidth()) + 1;
		int imgHeight = (int) Math.ceil(rect.getHeight()) + 1;
		long[] dim = new long[2];
		dim[0] = imgWidth * partitions.size() + border
				* (partitions.size() - 1);
		dim[1] = imgHeight;
		Img<UnsignedIntType> img = new ArrayImgFactory<UnsignedIntType>()
				.create(dim, new UnsignedIntType());
		RandomAccess<UnsignedIntType> ra = img.randomAccess();

		Map<TrackedNode, Point> positions = new HashMap<TrackedNode, Point>();

		long color = 1000;
		long[] rectDiff = new long[2];
		int partitionIndex = 0;
		for (String partition : partitions) {

			color = 0x1000 * (partitionIndex + 1);
			for (TrackedNode node : nodes.get(partition)) {
				ImgPlusValue<?> bitmask = node.getBitmask();
				Rectangle2D r = node.getImageRectangle();
				rectDiff[0] = (long) (r.getMinX() - rect.getMinX())
						+ (imgWidth + border) * partitionIndex;
				rectDiff[1] = (long) (r.getMinY() - rect.getMinY());
				@SuppressWarnings("unchecked")
				ImgPlus<BitType> ip = (ImgPlus<BitType>) bitmask.getImgPlus();

				Cursor<BitType> cursor = ip.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					for (int d = 0; d < cursor.numDimensions(); d++) {
						if (d == 2)
							continue;
						ra.setPosition(cursor.getLongPosition(d) + rectDiff[d],
								d);
						// System.out.println((cursor.getLongPosition(d) +
						// rectDiff[d] + " vs " + dim[d] + " [" + d + "]"));
					}
					ra.get().set(cursor.get().getInteger() * color);
				}
				color += 0x000f;

				// remember position for drawing edges later on
				Point position = new Point(dim.length);
				position.setPosition(
						(int) (node.getDoublePosition(0) - rect.getMinX() + (imgWidth + border)
								* partitionIndex), 0);
				position.setPosition(
						(int) (node.getDoublePosition(1) - rect.getMinY()), 1);
				positions.put(node, position);
			}
			partitionIndex++;
		}

		// create borders
		partitionIndex = 0;
		for (int p = 0; p < partitions.size(); p++) {
			if (partitionIndex > 0 && partitionIndex < partitions.size()) {
				int xOffset = partitionIndex * imgWidth + border
						* (partitionIndex - 1);
				for (int x = 0; x < border; x++) {
					ra.setPosition(xOffset + x, 0);
					for (int y = 0; y < imgHeight; y++) {
						ra.setPosition(y, 1);
						ra.get().set(color + 0x0f00);
					}
				}
			}
			partitionIndex++;
		}
		// ---borders

		for (Edge edge : edges) {
			TrackedNode start = edge.getStartNode();
			TrackedNode end = edge.getEndNode();

			Point p1 = positions.get(start);
			Point p2 = positions.get(end);

			BresenhamLine<UnsignedIntType> bl = new BresenhamLine<UnsignedIntType>(
					img, p1, p2);
			while (bl.hasNext()) {
				UnsignedIntType pixel = bl.next();
				pixel.set(color + 0x00f0);
			}

			// BresenhamLine<UnsignedIntType> bl = new
			// BresenhamLine<UnsignedIntType>(img, p1,p2);
		}

		return new ImgPlus<UnsignedIntType>(img);
	}

	public String toNodeString() {
		StringBuilder sb = new StringBuilder();
		for (String partition : partitions) {
			sb.append("[");
			for (TrackedNode node : nodes.get(partition)) {
				sb.append(node.getID()).append(" ");
			}
			sb.append("] ");
		}
		return sb.toString();
	}

	public Collection<TrackedNode> getNodes(String partition) {
		if (!partitions.contains(partition))
			return new LinkedList<TrackedNode>();
		return nodes.get(partition);
	}

	// might be moved
	// public static Collection<TransitionGraph> createAllPossibleGraphs(
	// TransitionGraph tg) {
	// List<TransitionGraph> graphs = new LinkedList<TransitionGraph>();
	// // assume 2 partitions only
	// if (tg.partitions.size() != 2) {
	// throw new IllegalStateException(
	// "Transition graph should have 2 partitions.");
	// }
	// // #possibilities
	// List<Node> nodes = new ArrayList<Node>();
	// for (String partition : tg.partitions) {
	// nodes.addAll(tg.nodes.get(partition));
	// }
	// long numPoss = (long) Math.pow(2, nodes.size());
	// // each bit represents usage of one node
	// long code = 0;
	// System.out.println("---" + tg + "---");
	// for (int i = 0; i < numPoss; i++) {
	// // System.out.println(i + ": " + Long.toBinaryString(code));
	// // ignore variants without connection
	// if (Long.bitCount(code) > 1) {
	//
	// // generate transition graph with connections
	// try {
	// TransitionGraph copy = new TransitionGraph(tg);
	// List<Node> firstPartNodes = new LinkedList<Node>();
	// List<Node> secondPartNodes = new LinkedList<Node>();
	// for (int bitIndex = 0; bitIndex < nodes.size(); bitIndex++) {
	// if ((code & (1L << bitIndex)) != 0L) {
	// Node node = nodes.get(bitIndex);
	// // search for correct object in copy
	// node = copy.getCopiedNode(node);
	// if (bitIndex < tg.nodes.get(tg.getFirstPartition())
	// .size())
	// firstPartNodes.add(node);
	// else
	// secondPartNodes.add(node);
	// }
	// }
	// for (Node fpn : firstPartNodes) {
	// for (Node spn : secondPartNodes) {
	// copy.createEdge(fpn, spn);
	// }
	// }
	//
	// graphs.add(copy);
	// } catch (PersistenceException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// code++;
	// }
	// System.out.println("---------------");
	// return graphs;
	// }

	/**
	 * Creates a collection of subgraphs of a {@link TransitionGraph}. If there
	 * are no edges, {@link TransitionGraph#createAllSubGraphs(TransitionGraph)}
	 * is called.
	 * 
	 * @param tg
	 *            a {@link TransitionGraph}
	 * @return a {@link Collection} of {@link TransitionGraph}s
	 */
	public static <T extends RealType<T>> Collection<TransitionGraph> createAllPossibleGraphs(
			TransitionGraph tg) {
		if (tg.edges.size() == 0)
			return createAllSubGraphs(tg);
		else
			return createLearningExamples(tg);
	}

	public static <T extends RealType<T>> Collection<TransitionGraph> createLearningExamples(
			TransitionGraph tg) {
		List<TransitionGraph> graphs = new LinkedList<TransitionGraph>();

		System.out.println("we r doing da learning example stuff");
		try {
			WeakConnectedComponent wcc = new WeakConnectedComponent(
					new ExecutionMonitor(), tg.getNet());
			wcc.start(new ExecutionMonitor());
			// neg node ids
			List<String> negNodeIds = new LinkedList<String>();
			for (KPartiteGraph<PersistentObject, Partition> graph : wcc
					.createComponentViews(new ExecutionMonitor(), tg.getNet())) {
				if (graph.getNoOfEdges() > 0) {
					// positive example
					graphs.add(new TransitionGraph(graph));
				} else {
					// neg examples
					assert graph.getNoOfNodes() == 1;
					negNodeIds.add(graph.getNodes().next().getId());
				}
			}

			if (negNodeIds.size() > 0) {
				System.out.println("NEG NODES!!! " + negNodeIds.size());
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (CanceledExecutionException e) {
			e.printStackTrace();
		}

		return graphs;
	}

	/**
	 * Creates all possible combinations of nodes in a {@link TransitionGraph}
	 * without edges.
	 * 
	 * @param tg
	 *            the source
	 * @return a {@link Collection} of {@link TransitionGraph}s
	 */
	public static <T extends RealType<T>> Collection<TransitionGraph> createAllSubGraphs(
			TransitionGraph tg) {
		if (tg.getPartitions().size() != 2)
			throw new IllegalArgumentException(
					"Transition graph must have 2 partitions.");

		List<TransitionGraph> graphs = new LinkedList<TransitionGraph>();

		if (tg.getNodes(tg.getLastPartition()).size() > 1) {
			// all 1:n combinations
			for (TrackedNode firstnode : tg.getNodes(tg.getFirstPartition())) {
				ArrayList<TrackedNode> nodes = new ArrayList<TrackedNode>(
						tg.getNodes(tg.getLastPartition()));
				int n = nodes.size();
				for (long i = 1; i < (1L << n); i++) {
					try {
						TransitionGraph graph = new TransitionGraph(
								tg.getPartitions());
						TrackedNode firstnodeCopy = firstnode.createCopyIn(graph);
						for (int j = 0; j < n; j++) {
							if ((i >> j) % 2 == 1) {
								TrackedNode node = nodes.get(j);
								TrackedNode nodeCopy = node.createCopyIn(graph);
								graph.createEdge(firstnodeCopy, nodeCopy);
							}
						}
						graphs.add(graph);
					} catch (PersistenceException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (tg.getNodes(tg.getFirstPartition()).size() > 1) {
			// all m:1 combinations
			for (TrackedNode secondnode : tg.getNodes(tg.getLastPartition())) {
				ArrayList<TrackedNode> nodes = new ArrayList<TrackedNode>(
						tg.getNodes(tg.getFirstPartition()));
				int n = nodes.size();
				for (long i = 1; i < (1L << n); i++) {
					if (Long.bitCount(i) <= 1) {
						// ignore 1:1 connections as we already got them in the
						// 1:n case
						continue;
					}
					try {
						TransitionGraph graph = new TransitionGraph(
								tg.getPartitions());
						TrackedNode secondnodeCopy = secondnode.createCopyIn(graph);
						for (int j = 0; j < n; j++) {
							if ((i >> j) % 2 == 1) {
								TrackedNode node = nodes.get(j);
								TrackedNode nodeCopy = node.createCopyIn(graph);
								graph.createEdge(nodeCopy, secondnodeCopy);
							}
						}
						graphs.add(graph);
					} catch (PersistenceException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (graphs.isEmpty()) {
			try {
				TransitionGraph copy = new TransitionGraph(tg);
				if (copy.getNodes(copy.getFirstPartition()).size() > 0
						&& copy.getNodes(copy.getLastPartition()).size() > 0) {
					TrackedNode start = copy.getNodes(copy.getFirstPartition())
							.iterator().next();
					TrackedNode end = copy.getNodes(copy.getLastPartition())
							.iterator().next();
					copy.createEdge(start, end);
				}
				graphs.add(copy);
			} catch (PersistenceException e) {
				e.printStackTrace();
			}
			// System.out.println("#tg edges: " + tg.edges.size());
		}

		return graphs;
	}

	public List<String> getPartitions() {
		return partitions;
	}

	/**
	 * Get the copied instance of the given node.
	 * 
	 * @param node
	 *            the original
	 * @return the copy
	 */
	public TrackedNode getCopiedNode(TrackedNode node) {
		// must be in same partition as copy
		for (TrackedNode n : getNodes(node.getPartition())) {
			if (n.getID().equals(node.getID()))
				return n;
		}
		throw new IllegalArgumentException(node + " not found in this copy.");
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public boolean hasFeature(String neededFeature) {
		try {
			return net.isFeatureDefined(neededFeature);
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setImageOffsets(long[] offsets) {
		try {
			if (!net.isFeatureDefined(TrackingConstants.NETWORK_FEATURE_DIMENSION)) {
				net.defineFeature(FeatureTypeFactory.getStringType(),
						TrackingConstants.NETWORK_FEATURE_DIMENSION);
			}
			net.addFeature(net, TrackingConstants.NETWORK_FEATURE_DIMENSION,
					OffsetHandling.encode(offsets));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long[] getImageOffsets() {
		String encoded;
		try {
			encoded = net.getFeatureString(net,
					TrackingConstants.NETWORK_FEATURE_DIMENSION);
			return OffsetHandling.decode(encoded);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new long[0];
	}

	/**
	 * Gets the time point of the first partition of this transition graph.
	 * 
	 * @return the first time point
	 */
	public double getStartTime() {
		int partIdx = 0;
		for (String partition : partitions) {
			if (!nodes.get(partition).isEmpty()) {
				return nodes.get(getFirstPartition()).iterator().next().frame()
						- partIdx;
			}
			partIdx++;
		}
		return 0;
	}
}
