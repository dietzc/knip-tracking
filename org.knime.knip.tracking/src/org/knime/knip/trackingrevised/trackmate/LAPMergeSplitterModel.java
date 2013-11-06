//package org.knime.knip.trackingrevised.trackmate;
//
//import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
//import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
//import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
//import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
//import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.SortedSet;
//
//import net.imglib2.type.logic.BitType;
//
//import org.jgrapht.graph.DefaultWeightedEdge;
//import org.knime.core.node.ExecutionContext;
//import org.knime.core.node.InvalidSettingsException;
//import org.knime.core.node.NodeLogger;
//import org.knime.core.node.NodeSettingsRO;
//import org.knime.core.node.NodeSettingsWO;
//import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
//import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
//import org.knime.knip.trackingrevised.data.graph.TrackedNode;
//import org.knime.knip.trackingrevised.util.PartitionComparator;
//import org.knime.knip.trackingrevised.util.TrackingConstants;
//import org.knime.network.core.api.Edge;
//import org.knime.network.core.api.GraphObjectIterator;
//import org.knime.network.core.api.KPartiteGraph;
//import org.knime.network.core.api.KPartiteGraphView;
//import org.knime.network.core.api.Partition;
//import org.knime.network.core.api.PersistentObject;
//import org.knime.network.core.core.PartitionType;
//import org.knime.network.core.core.exception.PersistenceException;
//import org.knime.network.core.core.feature.FeatureTypeFactory;
//import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
//import org.knime.network.core.knime.port.GraphPortObjectSpec;
//
//import fiji.plugin.trackmate.Spot;
//import fiji.plugin.trackmate.tracking.LAPUtils;
//import fiji.plugin.trackmate.tracking.TrackerKeys;
//import fiji.plugin.trackmate.tracking.costfunction.GapClosingCostFunction;
//import fiji.plugin.trackmate.tracking.costfunction.MergingCostFunction;
//import fiji.plugin.trackmate.tracking.costfunction.SplittingCostFunction;
//import fiji.plugin.trackmate.tracking.costmatrix.TrackSegmentCostMatrixCreator;
//import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
//import fiji.plugin.trackmate.tracking.hungarian.AssignmentProblem;
//import fiji.plugin.trackmate.tracking.hungarian.HungarianAlgorithm;
//import fiji.plugin.trackmate.util.TMUtils;
//
//import Jama.Matrix;
//
///**
// * Splits & Merges Tracks
// * 
// * @author Raffael Wagner, Jonas Zinn
// */
//public class LAPMergeSplitterModel extends KPartiteGraphNodeModel {
//	NodeLogger logger = NodeLogger.getLogger(LAPMergeSplitterModel.class);
//
//	private final static String CFGKEY_MTHRESHOLD = "mthreshold";
//
//	static SettingsModelDoubleBounded createMThresholdSetting() {
//		return new SettingsModelDoubleBounded(CFGKEY_MTHRESHOLD, 15.0, 1, 100);
//	}
//
//	private final static String CFGKEY_THRESHOLD = "threshold";
//
//	static SettingsModelDoubleBounded createThresholdSetting() {
//		return new SettingsModelDoubleBounded(CFGKEY_THRESHOLD, 50.0, 1, 100);
//	}
//
//	private final static String CFGKEY_STHRESHOLD = "sthreshold";
//
//	static SettingsModelDoubleBounded createSThresholdSetting() {
//		return new SettingsModelDoubleBounded(CFGKEY_STHRESHOLD, 15.0, 1, 100);
//	}
//
//	private final static String CFGKEY_DOMERGE = "merge";
//
//	static SettingsModelBoolean createMergeSetting() {
//		return new SettingsModelBoolean(CFGKEY_DOMERGE, true);
//	}
//
//	private final static String CFGKEY_DOSPLIT = "split";
//
//	static SettingsModelBoolean createSplitSetting() {
//		return new SettingsModelBoolean(CFGKEY_DOSPLIT, true);
//	}
//
//	private SettingsModelDoubleBounded m_mThresholdSetting = createMThresholdSetting();
//	private SettingsModelDoubleBounded m_sThresholdSetting = createSThresholdSetting();
//	private SettingsModelDoubleBounded m_thresholdSetting = createThresholdSetting();
//	private SettingsModelBoolean m_merge = createMergeSetting();
//	private SettingsModelBoolean m_split = createSplitSetting();
//
//	@Override
//	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
//			throws InvalidSettingsException {
//		return spec;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	protected KPartiteGraphView<PersistentObject, Partition> execute(
//			ExecutionContext exec,
//			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
//
//		double mthreshold = m_mThresholdSetting.getDoubleValue();
//		double sthreshold = m_sThresholdSetting.getDoubleValue();
//		double threshold = m_thresholdSetting.getDoubleValue();
//
//		boolean doMerge = m_merge.getBooleanValue();
//		boolean doSplit = m_split.getBooleanValue();
//
//		// final tracklet edges
//		Partition trackletEdgePartition = net
//				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);
//
//		List<Partition> partitions = new LinkedList<Partition>(
//				net.getPartitions(PartitionType.NODE));
//
//		List<PersistentObject> trackletStartNodes = new ArrayList<PersistentObject>();
//		List<PersistentObject> trackletEndNodes = new ArrayList<PersistentObject>();
//
//		final ArrayList<PersistentObject> trackletMiddleNodes = new ArrayList<PersistentObject>(
//				trackletStartNodes.size());
//
//		ArrayList<PersistentObject> splittingTrackletsPoints = null;
//		ArrayList<PersistentObject> mergingTrackletsPoints = null;
//
//		if (doSplit || doMerge) {
//			Iterator<PersistentObject> nodes = net.getNodes();
//			while (nodes.hasNext()) {
//				PersistentObject next = nodes.next();
//				GraphObjectIterator<PersistentObject> adjacentObjects = net
//						.getAdjacentObjects(next, trackletEdgePartition);
//
//				// only consider objects which live in the tracklet partition
//				int ctr = 0;
//				while (adjacentObjects.hasNext()) {
//					adjacentObjects.next();
//					if (ctr == 1) {
//						trackletMiddleNodes.add(next);
//						break;
//					}
//					ctr++;
//				}
//
//				// we found end or start
//				if (ctr == 1) {
//					if (net.getBooleanFeature(next,
//							TrackingConstants.FEATURE_TRACKLETSTARTNODE)) {
//						trackletEndNodes.add(next);
//					} else {
//						trackletStartNodes.add(next);
//					}
//				}
//
//			}
//		} else {
//			splittingTrackletsPoints = trackletMiddleNodes;
//			mergingTrackletsPoints = trackletMiddleNodes;
//		}
//
//		int frameCutOff = 2;
//
//		// Top left quadrant
//		Matrix topLeft = createTopLeftQuadrant(frameCutOff, doSplit, doMerge,
//				net, trackletMiddleNodes, trackletStartNodes, trackletEndNodes);
//
//		double cutoff = getCutoff(topLeft);
//		Matrix topRight = getAlternativeScores(topLeft.getRowDimension(),
//				cutoff);
//		Matrix bottomLeft = getAlternativeScores(topLeft.getColumnDimension(),
//				cutoff);
//		Matrix bottomRight = getLowerRight(topLeft, cutoff);
//
//		// 4 - Fill in complete cost matrix by quadrant
//		final int numCols = 2 * trackletStartNodes.size()
//				+ splittingTrackletsPoints.size()
//				+ mergingTrackletsPoints.size();
//		final int numRows = 2 * trackletStartNodes.size()
//				+ splittingTrackletsPoints.size()
//				+ mergingTrackletsPoints.size();
//
//		Matrix costs = new Matrix(numRows, numCols, 0);
//
//		costs.setMatrix(0, topLeft.getRowDimension() - 1, 0,
//				topLeft.getColumnDimension() - 1, topLeft); // Gap closing
//		costs.setMatrix(topLeft.getRowDimension(), numRows - 1, 0,
//				topLeft.getColumnDimension() - 1, bottomLeft); // Initiating and
//																// merging
//																// alternative
//		costs.setMatrix(0, topLeft.getRowDimension() - 1,
//				topLeft.getColumnDimension(), numCols - 1, topRight); // Terminating
//																		// and
//																		// splitting
//																		// alternative
//		costs.setMatrix(topLeft.getRowDimension(), numRows - 1,
//				topLeft.getColumnDimension(), numCols - 1, bottomRight); // Lower
//																			// right
//																			// (transpose
//																			// of
//																			// gap
//																			// closing,
//																			// mathematically
//																			// required
//																			// for
//																			// LAP)
//
//		//
//
//		double sigma = net.getDoubleFeature(net,
//				TrackingConstants.NETWORK_FEATURE_STDEV);
//
//		// sort to ensure correct order
//		Collections.sort(partitions, new PartitionComparator());
//
//		HashSet<PersistentObject> delEnd = new HashSet<PersistentObject>();
//		HashSet<PersistentObject> delStart = new HashSet<PersistentObject>();
//
//		int numberPart = partitions.size() - 1;
//		// merging
//		if (doMerge) {
//			// Loop over the end nodes of each tracklet
//			for (String o : ends) {
//				PersistentObject end = net.getNode(o);
//				// check if the found end node is in the last frame
//				int timeFrame = net.getIntegerFeature(end,
//						TrackMateConstants.TIME_STAMPS);
//				timeFrame += 1;
//				if (!(timeFrame <= numberPart))
//					continue;
//				// find all nodes close to the chosen end node
//				double min = Double.POSITIVE_INFINITY;
//				PersistentObject mergeObj = null;
//				for (PersistentObject edge : net.getOutgoingEdges(end)) {
//					PersistentObject merge = null;
//					// filter out nodes that are in former frames
//					for (PersistentObject x : net.getIncidentNodes(edge))
//						if (!end.equals(x))
//							merge = x;
//					if (timeFrame > net.getIntegerFeature(merge,
//							TrackMateConstants.TIME_STAMPS))
//						continue;
//					// find the closest node that is within the max Radius
//					double weight = net.getEdgeWeight(edge);
//					if (weight < min && weight < mthreshold) {
//						min = weight;
//						mergeObj = merge;
//					}
//				}
//				// is the found edge valid
//				double prob = (Math.exp(-min / sigma) * 100);
//				if (null == mergeObj || prob < threshold)
//					continue;
//
//				// add the found edge and merge the two tracklets and change the
//				// entries in the nodes
//				PersistentObject edge = net.createEdge(end + "-" + mergeObj,
//						trackletEdgePartition, end, mergeObj);
//				net.setEdgeWeight(edge, (int) prob);
//				net.addFeature(end, TrackingConstants.FEATURE_ISTRACKLETEND,
//						false);
//				delEnd.add(end);
//				PersistentObject start = net.getNode(net.getFeatureString(end,
//						TrackingConstants.FEATURE_TRACKLETSTARTNODE));
//				delStart.add(start);
//				PersistentObject newStart = net.getNode(net.getFeatureString(
//						mergeObj, TrackingConstants.FEATURE_TRACKLETSTARTNODE));
//				Integer trackletSizeNew = net.getIntegerFeature(start,
//						TrackingConstants.FEATURE_TRACKLET_SIZE);
//				Integer trackletSizeOld = net.getIntegerFeature(newStart,
//						TrackingConstants.FEATURE_TRACKLET_SIZE);
//				net.addFeature(newStart,
//						TrackingConstants.FEATURE_TRACKLET_SIZE,
//						trackletSizeNew + trackletSizeOld);
//				// switch the startNodes of the one tracklet to the startNode of
//				// the other tracklet to have the same
//				// startNode in the new bigger tracklet
//				boolean found = true;
//				for (int i = timeFrame; i >= 0 && found; i--) {
//					found = false;
//					for (PersistentObject n : net.getNodes(partitions.get(i))) {
//
//						String startNode = net.getFeatureString(n,
//								TrackingConstants.FEATURE_TRACKLETSTARTNODE);
//						if (start.getId().equals(startNode)) {
//							found = true;
//							net.addFeature(
//									n,
//									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
//									newStart.getId());
//						}
//					}
//				}
//			}
//			ends.removeAll(delEnd);
//			trackletStartNodes.removeAll(delStart);
//			delEnd.clear();
//			delStart.clear();
//		}
//
//		// spliting
//		if (doSplit) {
//			// Loop over the start nodes of each tracklet
//			for (String o : trackletStartNodes) {
//				PersistentObject start = net.getNode(o);
//				// check if the found start node is in the first frame
//				int timeFrame = net.getIntegerFeature(start,
//						TrackMateConstants.TIME_STAMPS);
//				timeFrame -= 1;
//				if (!(timeFrame >= 0))
//					continue;
//				// find all nodes close to the chosen start node
//				double min = Double.POSITIVE_INFINITY;
//				PersistentObject splitObj = null;
//
//				for (PersistentObject obj : net.getOutgoingEdges(start)) {
//					PersistentObject split = null;
//					// filter out nodes that are in later frames
//					for (PersistentObject x : net.getIncidentNodes(obj))
//						if (!start.equals(x))
//							split = x;
//					if (timeFrame < net.getIntegerFeature(split,
//							TrackMateConstants.TIME_STAMPS))
//						continue;
//					// find the closest node that is within the max Radius
//					double weight = net.getEdgeWeight(obj);
//					if (weight < min && weight < sthreshold) {
//						min = weight;
//						splitObj = split;
//					}
//				}
//
//				if (splitObj == null)
//					continue;
//
//				// is the found edge valid
//				double prob = (Math.exp(-min / sigma) * 100);
//				if (prob < threshold)
//					continue;
//
//				// add the found edge and merge the two tracklets and change the
//				// entries in the nodes
//				PersistentObject edge = net.createEdge(splitObj + "-" + start,
//						trackletEdgePartition, splitObj, start);
//				net.setEdgeWeight(edge, (int) prob);
//
//				delStart.add(start);
//				PersistentObject newStart = net.getNode(net.getFeatureString(
//						splitObj, TrackingConstants.FEATURE_TRACKLETSTARTNODE));
//				Integer trackletSizeNew = net.getIntegerFeature(start,
//						TrackingConstants.FEATURE_TRACKLET_SIZE);
//				Integer trackletSizeOld = net.getIntegerFeature(newStart,
//						TrackingConstants.FEATURE_TRACKLET_SIZE);
//				net.addFeature(newStart,
//						TrackingConstants.FEATURE_TRACKLET_SIZE,
//						trackletSizeNew + trackletSizeOld);
//				// switch the startNodes of the one tracklet to the startNode of
//				// the other tracklet to have the same
//				// startNode in the new bigger tracklet
//				boolean found = true;
//				for (int i = timeFrame; i < numberPart && found; i++) {
//					found = false;
//					for (PersistentObject n : net.getNodes(partitions.get(i))) {
//
//						String startNode = net.getFeatureString(n,
//								TrackingConstants.FEATURE_TRACKLETSTARTNODE);
//						if (start.getId().equals(startNode)) {
//							found = true;
//							net.addFeature(
//									n,
//									TrackingConstants.FEATURE_TRACKLETSTARTNODE,
//									newStart.getId());
//						}
//					}
//				}
//			}
//			ends.removeAll(delEnd);
//			trackletStartNodes.removeAll(delStart);
//		}
//
//		net.addFeature(net, TrackMateConstants.START_NODES, trackletStartNodes);
//		net.addFeature(net, TrackMateConstants.END_NODES, ends);
//
//		net.commit();
//		return net;
//	}
//
//	/**
//	 * Creates the final tracks computed from step 2.
//	 * 
//	 * @see TrackSegmentCostMatrixCreator#getMiddlePoints()
//	 * @param middlePoints
//	 *            A list of the middle points of the track segments.
//	 * @return True if execution completes successfully, false otherwise.
//	 */
//	public boolean linkTrackSegmentsToFinalTracks(double[][] costs) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//
//		// Solve LAP
//		final int[][] finalTrackSolutions = solveLAPForFinalTracks(costs);
//
//		// Compile LAP solutions into final tracks
//		compileFinalTracks(finalTrackSolutions);
//
//		return true;
//	}
//
//	public int[][] solveLAPForFinalTracks(double[][] costs) {
//		// Solve the LAP using the Hungarian Algorithm
//		final AssignmentProblem problem = new AssignmentProblem(costs);
//		final AssignmentAlgorithm solver = createAssignmentProblemSolver();
//		final int[][] solutions = problem.solve(solver);
//		return solutions;
//	}
//
//	/**
//	 * Hook for subclassers. Generate the assignment algorithm that will be used
//	 * to solve the {@link AssignmentProblem} held by this tracker.
//	 * <p>
//	 * Here, by default, it returns the Hungarian algorithm implementation by
//	 * Gary Baker and Nick Perry that solves an assignment problem in O(n^4).
//	 */
//	protected AssignmentAlgorithm createAssignmentProblemSolver() {
//		return new HungarianAlgorithm();
//	}
//
//	/**
//	 * Takes the submatrix of costs defined by rows 0 to numRows - 1 and columns
//	 * 0 to numCols - 1, transpose it, and sets any non-BLOCKED value to be
//	 * cutoff.
//	 * <p>
//	 * The reasoning for this is explained in the supplementary notes of the
//	 * paper, but basically it has to be made this way so that the LAP is
//	 * solvable.
//	 */
//	protected Matrix getLowerRight(Matrix topLeft, double cutoff) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//		Matrix lowerRight = topLeft.transpose();
//		for (int i = 0; i < lowerRight.getRowDimension(); i++) {
//			for (int j = 0; j < lowerRight.getColumnDimension(); j++) {
//				if (lowerRight.get(i, j) < blockingValue) {
//					lowerRight.set(i, j, cutoff);
//				}
//			}
//		}
//		return lowerRight;
//	}
//
//	/**
//	 * Sets alternative scores in a new matrix along a diagonal. The new matrix
//	 * is n x n, and is set to BLOCKED everywhere except along the diagonal that
//	 * runs from top left to bottom right.
//	 */
//	protected Matrix getAlternativeScores(int n, double cutoff) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//		final Matrix alternativeScores = new Matrix(n, n, blockingValue);
//
//		// Set the cutoff along the diagonal (top left to bottom right)
//		for (int i = 0; i < alternativeScores.getRowDimension(); i++) {
//			alternativeScores.set(i, i, cutoff);
//		}
//
//		return alternativeScores;
//	}
//
//	/**
//	 * Takes the solutions from the Hungarian algorithm, which are an int[][],
//	 * and appropriately links the track segments. Before this method is called,
//	 * the Spots in the track segments are connected within themselves, but not
//	 * between track segments.
//	 * 
//	 * Thus, we only care here if the result was a 'gap closing,' 'merging,' or
//	 * 'splitting' event, since the others require no change to the existing
//	 * structure of the track segments.
//	 * 
//	 * Method: for each solution of the LAP, determine if it's a gap closing,
//	 * merging, or splitting event. If so, appropriately link the track segment
//	 * Spots.
//	 */
//	private void compileFinalTracks(
//			KPartiteGraph<PersistentObject, Partition> net,
//			final double[][] costs, final int[][] finalTrackSolutions,
//			List<PersistentObject> startNodes, List<PersistentObject> endNodes,
//			List<PersistentObject> mergingNodes,
//			List<PersistentObject> splittingNodes) {
//		final int numTrackSegments = startNodes.size();
//		final int numMergingMiddlePoints = mergingNodes.size();
//		final int numSplittingMiddlePoints = splittingNodes.size();
//		double weight;
//
//		Partition trackletEdgePartition = net
//				.getPartition(TrackingConstants.TRACKLET_EDGE_PARTITION);
//
//		for (final int[] solution : finalTrackSolutions) {
//			final int i = solution[0];
//			final int j = solution[1];
//
//			if (i < numTrackSegments) {
//
//				// Case 1: Gap closing
//				if (j < numTrackSegments) {
//					final PersistentObject segmentEnd = endNodes.get(i);
//					final PersistentObject segmentStart = startNodes.get(j);
//					weight = costs[i][j];
//
//					net.addFeature(segmentEnd,
//							TrackingConstants.FEATURE_ISTRACKLETEND, false);
//
//					net.createEdge(
//							segmentEnd.getId() + "_" + segmentStart.getId(),
//							trackletEdgePartition, segmentEnd, segmentStart);
//
//					net.addFeature(segmentEnd,
//							TrackingConstants.FEATURE_TRACKLETSTARTNODE,
//							segmentStart);
//
//				} else if (j < (numTrackSegments + numMergingMiddlePoints)) {
//
//					// Case 2: Merging
//					final SortedSet<Spot> segmentEnd = endNodes.get(i);
//					final Spot end = segmentEnd.last();
//					final Spot middle = mergingMiddlePoints.get(j
//							- numTrackSegments);
//					weight = segmentCosts[i][j];
//					final DefaultWeightedEdge edge = graph.addEdge(end, middle);
//					graph.setEdgeWeight(edge, weight);
//
//					if (DEBUG) {
//						SortedSet<Spot> track = null;
//						int indexTrack = 0;
//						int indexSpot = 0;
//						for (final SortedSet<Spot> t : trackSegments)
//							if (t.contains(middle)) {
//								track = t;
//								for (final Spot spot : track) {
//									if (spot == middle)
//										break;
//									else
//										indexSpot++;
//								}
//								break;
//							} else
//								indexTrack++;
//						System.out.println("Merging from segment " + i
//								+ " end to spot " + indexSpot + " in segment "
//								+ indexTrack + ".");
//					}
//
//				}
//			} else if (i < (numTrackSegments + numSplittingMiddlePoints)) {
//
//				// Case 3: Splitting
//				if (j < numTrackSegments) {
//					final SortedSet<Spot> segmentStart = trackSegments.get(j);
//					final Spot start = segmentStart.first();
//					final Spot mother = splittingMiddlePoints.get(i
//							- numTrackSegments);
//					weight = segmentCosts[i][j];
//					final DefaultWeightedEdge edge = graph.addEdge(mother,
//							start);
//					graph.setEdgeWeight(edge, weight);
//
//					if (DEBUG) {
//						SortedSet<Spot> track = null;
//						int indexTrack = 0;
//						int indexSpot = 0;
//						for (final SortedSet<Spot> t : trackSegments)
//							if (t.contains(mother)) {
//								track = t;
//								for (final Spot spot : track) {
//									if (spot == mother)
//										break;
//									else
//										indexSpot++;
//								}
//								break;
//							} else
//								indexTrack++;
//						System.out.println("Splitting from spot " + indexSpot
//								+ " in segment " + indexTrack + " to segment"
//								+ j + ".");
//					}
//				}
//			}
//		}
//
//	}
//
//	/**
//	 * Calculates the CUTOFF_PERCENTILE cost of all costs in gap closing,
//	 * merging, and splitting matrices to assign the top right and bottom left
//	 * score matrices.
//	 */
//	private double getCutoff(Matrix m) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//		final double cutoffPercentile = TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
//		final double alternativeLinkingCostFactor = TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
//
//		// Get a list of all non-BLOCKED cost
//		ArrayList<Double> scores = new ArrayList<Double>();
//		for (int i = 0; i < m.getRowDimension(); i++) {
//			for (int j = 0; j < m.getColumnDimension(); j++) {
//				if (m.get(i, j) < blockingValue) {
//					scores.add(m.get(i, j));
//				}
//			}
//		}
//
//		// Get the correct percentile of all non-BLOCKED cost values
//		double[] scoreArr = new double[scores.size()];
//		for (int i = 0; i < scores.size(); i++) {
//			scoreArr[i] = scores.get(i);
//		}
//		double cutoff = TMUtils.getPercentile(scoreArr, cutoffPercentile);
//		if (!(cutoff < blockingValue)) {
//			cutoff = 10.0d; // TODO how to fix this? In this case, there are no
//							// costs in the matrix, so nothing to calculate the
//							// cutoff values from
//		}
//		return alternativeLinkingCostFactor * cutoff;
//	}
//
//	/**
//	 * Creates the top left quadrant of the overall cost matrix, which contains
//	 * the costs for gap closing, merging, and splitting (as well as the empty
//	 * middle section).
//	 * 
//	 * @param startNodes
//	 * @throws PersistenceException
//	 */
//	private Matrix createTopLeftQuadrant(int cutOff, boolean allowSplitting,
//			boolean allowMerging,
//			KPartiteGraph<PersistentObject, Partition> net,
//			List<PersistentObject> middleNodes,
//			List<PersistentObject> startNodes, List<PersistentObject> endNodes)
//			throws PersistenceException {
//
//		if (endNodes.size() != startNodes.size()) {
//			throw new IllegalArgumentException("this shouldn't happen");
//		}
//
//		Matrix topLeft, mergingScores, splittingScores, middle;
//
//		// Create sub-matrices of top left quadrant (gap closing, merging,
//		// splitting, and empty middle
//
//		Matrix gapClosingScores = getGapClosingCostSubMatrix(cutOff, net,
//				startNodes, endNodes);
//
//		if (!allowMerging && !allowSplitting) {
//
//			// We skip the rest and only keep this modest matrix.
//			topLeft = gapClosingScores;
//			mergingScores = new Matrix(0, 0);
//			splittingScores = new Matrix(0, 0);
//			middle = new Matrix(0, 0);
//
//		} else {
//			ArrayList<PersistentObject> mergingObjects = new ArrayList<PersistentObject>();
//			mergingScores = getMergingCostSubMatrix(mergingObjects,
//					allowMerging, net, middleNodes, endNodes);
//			if (null == mergingScores) {
//				return null;
//			}
//
//			ArrayList<PersistentObject> splittingObjects = new ArrayList<PersistentObject>();
//			splittingScores = getSplittingCostSubMatrix(splittingObjects,
//					allowMerging, net, middleNodes, startNodes);
//			if (null == splittingScores) {
//				return null;
//			}
//
//			middle = new Matrix(splittingObjects.size(), mergingObjects.size(),
//					TrackerKeys.DEFAULT_BLOCKING_VALUE);
//
//			// Initialize the top left quadrant
//			final int numRows = startNodes.size() + splittingObjects.size();
//			final int numCols = startNodes.size() + mergingObjects.size();
//			topLeft = new Matrix(numRows, numCols);
//
//			// Fill in top left quadrant
//			topLeft.setMatrix(0, startNodes.size() - 1, 0,
//					startNodes.size() - 1, gapClosingScores);
//			topLeft.setMatrix(startNodes.size(), numRows - 1, 0,
//					startNodes.size() - 1, splittingScores);
//			topLeft.setMatrix(0, startNodes.size() - 1, startNodes.size(),
//					numCols - 1, mergingScores);
//			topLeft.setMatrix(startNodes.size(), numRows - 1,
//					startNodes.size(), numCols - 1, middle);
//		}
//
//		return topLeft;
//	}
//
//	/**
//	 * Uses a merging cost function to fill in the merging costs sub-matrix.
//	 * 
//	 * @throws PersistenceException
//	 */
//	private Matrix getMergingCostSubMatrix(List<PersistentObject> merging,
//			boolean allowed, KPartiteGraph<PersistentObject, Partition> net,
//			List<PersistentObject> middlePoints, List<PersistentObject> endNodes)
//			throws PersistenceException {
//		Matrix m = null;
//
//		if (!allowed) {
//			m = new Matrix(endNodes.size(), 0,
//					TrackerKeys.DEFAULT_BLOCKING_VALUE);
//		} else {
//			m = new Matrix(endNodes.size(), middlePoints.size());
//		}
//
//		for (int i = 0; i < endNodes.size(); i = 0) {
//			TrackedNode<?> endNode = new TrackedNode(net, endNodes.get(i));
//
//			for (int j = 0; j < middlePoints.size(); j++) {
//				TrackedNode<?> middle = new TrackedNode(net, middlePoints.get(j));
//
//				// Frame threshold - middle Spot must be one frame ahead of the
//				// end Spot
//				double endFrame = endNode.getTime();
//				double middleFrame = middle.getTime();
//				// We only merge from one frame to the next one, no more
//				if (middleFrame - endFrame != 1) {
//					m.set(i, j, TrackerKeys.DEFAULT_BLOCKING_VALUE);
//					continue;
//				}
//
//				double cost = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//				for (PersistentObject e : net.getOutgoingEdges(endNode
//						.getPersistentObject())) {
//					if (net.isIncident(e, middle.getPersistentObject())) {
//						cost = net.getEdgeWeight(e);
//						break;
//					}
//				}
//
//				m.set(i, j, cost);
//			}
//		}
//
//		return pruneColumns(m, middlePoints, merging);
//	}
//
//	/**
//	 * Uses a splitting cost function to fill in the splitting costs submatrix.
//	 * 
//	 * @throws PersistenceException
//	 */
//	private Matrix getSplittingCostSubMatrix(List<PersistentObject> splitting,
//			boolean allowed, KPartiteGraph<PersistentObject, Partition> net,
//			List<PersistentObject> middlePoints,
//			List<PersistentObject> startNodes) throws PersistenceException {
//		Matrix m = null;
//
//		if (!allowed) {
//			m = new Matrix(middlePoints.size(), splitting.size(),
//					TrackerKeys.DEFAULT_BLOCKING_VALUE);
//		} else {
//			m = new Matrix(middlePoints.size(), startNodes.size());
//		}
//
//		for (int i = 0; i < middlePoints.size(); i++) {
//
//			TrackedNode<?> middle = new TrackedNode(net, middlePoints.get(i));
//
//			for (int j = 0; j < startNodes.size(); j++) {
//
//				TrackedNode<?> start = new TrackedNode(net, startNodes.get(j));
//
//				if (middle.getStringFeature(
//						TrackingConstants.FEATURE_TRACKLETSTARTNODE)
//						.equalsIgnoreCase(start.toString())) {
//					m.set(i, j, TrackerKeys.DEFAULT_BLOCKING_VALUE);
//					continue;
//				}
//
//				// Frame threshold - middle Spot must be one frame behind of the
//				// start Spot
//				double startFrame = start.getTime();
//				double middleFrame = middle.getTime();
//				if (startFrame - middleFrame != 1) {
//					m.set(i, j, TrackerKeys.DEFAULT_BLOCKING_VALUE);
//					continue;
//				}
//
//				double cost = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//				for (PersistentObject e : net.getIncomingEdges(start
//						.getPersistentObject())) {
//					if (net.isIncident(e, middle.getPersistentObject())) {
//						cost = net.getEdgeWeight(e);
//						break;
//					}
//				}
//
//				m.set(i, j, cost);
//			}
//		}
//
//		return pruneRows(m, middlePoints, splitting);
//	}
//
//	/**
//	 * Iterates through the complete cost matrix, and removes any rows that only
//	 * contain BLOCKED (there are no useful rows with non-BLOCKED values).
//	 * Returns the pruned matrix with the empty rows.
//	 */
//	private Matrix pruneRows(Matrix m, List<PersistentObject> middlePoints,
//			List<PersistentObject> keptMiddleSpots) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//		// Find all rows that contain a cost (a value != BLOCKED)
//		double[][] full = m.copy().getArray();
//		double[] curRow;
//		ArrayList<double[]> usefulRows = new ArrayList<double[]>();
//		for (int i = 0; i < m.getRowDimension(); i++) {
//			boolean containsCost = false;
//			curRow = full[i];
//			for (int j = 0; j < m.getColumnDimension(); j++) {
//				if (curRow[j] < blockingValue) {
//					containsCost = true;
//				}
//			}
//			if (containsCost) {
//				usefulRows.add(curRow);
//				keptMiddleSpots.add(middlePoints.get(i));
//			}
//		}
//
//		// Convert ArrayList<double[]> -> double[][] -> Matrix
//		double[][] pruned = new double[usefulRows.size()][m
//				.getColumnDimension()];
//		for (int i = 0; i < usefulRows.size(); i++) {
//			pruned[i] = usefulRows.get(i);
//		}
//
//		if (pruned.length == 0) {
//			return new Matrix(0, 0);
//		}
//		return new Matrix(pruned);
//	}
//
//	/**
//	 * Iterates through the complete cost matrix, and removes any columns that
//	 * only contain BLOCKED (there are no useful columns with non-BLOCKED
//	 * values). Returns the pruned matrix with the empty columns.
//	 */
//	private Matrix pruneColumns(Matrix m,
//			List<PersistentObject> allMiddlePoints,
//			List<PersistentObject> keptMiddleSpots) {
//		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//		// Find all columns that contain a cost (a value != BLOCKED)
//		double[][] full = m.copy().getArray();
//		ArrayList<double[]> usefulColumns = new ArrayList<double[]>();
//		for (int j = 0; j < m.getColumnDimension(); j++) {
//			boolean containsCost = false;
//			double[] curCol = new double[m.getRowDimension()];
//			for (int i = 0; i < m.getRowDimension(); i++) {
//				curCol[i] = full[i][j];
//				if (full[i][j] < blockingValue) {
//					containsCost = true;
//				}
//			}
//			if (containsCost) {
//				usefulColumns.add(curCol);
//				keptMiddleSpots.add(allMiddlePoints.get(j));
//			}
//		}
//
//		// Convert ArrayList<double[]> -> double[][] -> Matrix
//		double[][] pruned = new double[m.getRowDimension()][usefulColumns
//				.size()];
//		double[] col;
//		for (int i = 0; i < usefulColumns.size(); i++) {
//			col = usefulColumns.get(i);
//			for (int j = 0; j < col.length; j++) {
//				pruned[j][i] = col[j];
//			}
//
//		}
//
//		return new Matrix(pruned);
//	}
//
//	/**
//	 * Uses a gap closing cost function to fill in the gap closing costs
//	 * sub-matrix.
//	 * 
//	 * @throws PersistenceException
//	 */
//
//	// can be multithreaded in the future
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private Matrix getGapClosingCostSubMatrix(int frameCutOff,
//			KPartiteGraph<PersistentObject, Partition> net,
//			List<PersistentObject> startNodes, List<PersistentObject> endNodes)
//			throws PersistenceException {
//
//		final Partition gapPartition = net
//				.getPartition(TrackingConstants.GAP_PARTITION_NAME);
//
//		final int numTracklets = endNodes.size();
//		final Matrix m = new Matrix(startNodes.size(), endNodes.size());
//
//		for (int i = 0; i < numTracklets; i++) {
//			TrackedNode<?> endNode = new TrackedNode(net, startNodes.get(i));
//			double endFrame = endNode.getTime();
//			for (int j = 0; j < numTracklets; j++) {
//				// If i and j are the same track segment, block it
//				if (i == j) {
//					m.set(i, j, TrackerKeys.DEFAULT_BLOCKING_VALUE);
//					continue;
//				}
//				TrackedNode<?> startNode = new TrackedNode(net, startNodes.get(j));
//
//				double startFrame = startNode.getTime();
//
//				// Frame cutoff. A value of 1 means a gap of 1 frame. If the
//				// end spot
//				// is in frame 10, the start spot in frame 12, and if the
//				// max gap is 1
//				// then we should sought to bridge this gap (12 to 10 is a
//				// gap of 1 frame).
//				if (startFrame - endFrame > (frameCutOff + 1)
//						|| endFrame >= startFrame) {
//					m.set(i, j, TrackerKeys.DEFAULT_BLOCKING_VALUE);
//					continue;
//				}
//
//				double cost = TrackerKeys.DEFAULT_BLOCKING_VALUE;
//				for (PersistentObject edge : net.getIncidentEdges(
//						startNode.getPersistentObject(), gapPartition)) {
//					if (net.isIncident(edge, endNode.getPersistentObject())) {
//						cost = net.getEdgeWeight(edge);
//						break;
//					}
//				}
//
//				m.set(i, j, cost);
//			}
//
//		}
//
//		return m;
//	}
//
//	@Override
//	protected void resetInternal() {
//	}
//
//	@Override
//	protected void saveSettingsTo(NodeSettingsWO settings) {
//		m_thresholdSetting.saveSettingsTo(settings);
//		m_mThresholdSetting.saveSettingsTo(settings);
//		m_sThresholdSetting.saveSettingsTo(settings);
//		m_merge.saveSettingsTo(settings);
//		m_split.saveSettingsTo(settings);
//	}
//
//	@Override
//	protected void validateSettings(NodeSettingsRO settings)
//			throws InvalidSettingsException {
//		m_thresholdSetting.validateSettings(settings);
//		m_mThresholdSetting.validateSettings(settings);
//		m_sThresholdSetting.validateSettings(settings);
//		m_merge.validateSettings(settings);
//		m_split.validateSettings(settings);
//	}
//
//	@Override
//	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
//			throws InvalidSettingsException {
//		m_thresholdSetting.loadSettingsFrom(settings);
//		m_mThresholdSetting.loadSettingsFrom(settings);
//		m_sThresholdSetting.loadSettingsFrom(settings);
//		m_merge.loadSettingsFrom(settings);
//		m_split.loadSettingsFrom(settings);
//	}
//}