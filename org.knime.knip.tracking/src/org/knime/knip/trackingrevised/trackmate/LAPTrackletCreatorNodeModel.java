package org.knime.knip.trackingrevised.trackmate;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.knip.trackingrevised.util.PartitionComparator;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Node;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.KPartiteGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

import Jama.Matrix;
import fiji.plugin.trackmate.tracking.SimpleLAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentProblem;
import fiji.plugin.trackmate.tracking.hungarian.HungarianAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.MunkresKuhnAlgorithm;

public class LAPTrackletCreatorNodeModel extends KPartiteGraphNodeModel {

	enum LAPTrackerVariant {
		Hungarian, MunkresKuhnAlgorithm;
	}

	NodeLogger logger = NodeLogger.getLogger(LAPTrackletCreatorNodeModel.class);

	private final static String CFGKEY_MAXRADIUS = "maxradius";

	static SettingsModelDoubleBounded createMaxRadiusSetting() {
		return new SettingsModelDoubleBounded(CFGKEY_MAXRADIUS, 15.0, 1, 100);
	}

	private final static String CFGKEY_THRESHOLD = "threshold";

	static SettingsModelDouble createThresholdSetting() {
		return new SettingsModelDouble(CFGKEY_THRESHOLD, 50);
	}

	private final static String CFGKEY_GAPS = "gaps";

	static SettingsModelBoolean createGapsSetting() {
		return new SettingsModelBoolean(CFGKEY_GAPS, true);
	}

	private SettingsModelDoubleBounded m_maxRadius = createMaxRadiusSetting();

	private SettingsModelDouble m_threshold = createThresholdSetting();

	private SettingsModelBoolean m_gaps = createGapsSetting();

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		return spec;
	}

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net) throws Exception {
		List<Partition> partitions = new LinkedList<Partition>(
				net.getPartitions(PartitionType.NODE));

		// sort to ensure correct order
		Collections.sort(partitions, new PartitionComparator());

		exec.setMessage("Search for linear edges ...");

		// some network stuff
		Partition trackletEdgePartition = net.createPartition(
				TrackingConstants.TRACKLET_EDGE_PARTITION, PartitionType.EDGE);

		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.FEATURE_TRACKLETSTARTNODE);

		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.FEATURE_TRACKLET_SIZE);

		// go over all partitions
		fillNetworkWithTracklets(exec, net, partitions, trackletEdgePartition);

		net.commit();
		return net;
	}

	/**
	 * @param exec
	 * @param net
	 * @param partitions
	 * @param trackletEdgePartition
	 * @throws PersistenceException
	 * @throws CanceledExecutionException
	 * @throws InvalidFeatureException
	 */
	private void fillNetworkWithTracklets(ExecutionContext exec,
			KPartiteGraph<PersistentObject, Partition> net,
			List<Partition> partitions, Partition trackletEdgePartition)
			throws PersistenceException, CanceledExecutionException,
			InvalidFeatureException {
		for (int p = 0; p < partitions.size() - 1; p++) {
			System.out.println("next partition");
			Partition t0partition = partitions.get(p);
			Partition t1Partition = partitions.get(p + 1);

			final int t0Size = numNodes(net, t0partition);
			final int t1Size = numNodes(net, t1Partition);

			HashMap<Integer, PersistentObject> idxMapT0 = new HashMap<Integer, PersistentObject>();
			HashMap<Integer, PersistentObject> idxMapT1 = new HashMap<Integer, PersistentObject>();

			// create cost matrix
			double[][] costMatrix = createCostMatrix(t0Size, t1Size, net,
					t0partition, t1Partition, idxMapT0, idxMapT1);

			exec.checkCanceled();

			boolean allBlocked = true;
			for (int i = 0; i < costMatrix.length; i++) {
				for (int j = 0; j < costMatrix[i].length; j++) {
					if (costMatrix[i][j] != Double.POSITIVE_INFINITY) {
						allBlocked = false;
						break;
					}
				}
				if (!allBlocked)
					break;
			}

			if (!allBlocked) {
				// Find solution
				final AssignmentProblem problem = new AssignmentProblem(
						costMatrix);
				final AssignmentAlgorithm solver = createAssignmentProblemSolver(LAPTrackerVariant.MunkresKuhnAlgorithm);

				final int[][] solutions = problem.solve(solver);

				// Extend track segments using solutions: we update the graph
				// edges
				for (int j = 0; j < solutions.length; j++) {
					if (solutions[j].length == 0)
						continue;
					final int i0 = solutions[j][0];
					final int i1 = solutions[j][1];

					if (i0 < t0Size && i1 < t1Size) {
						// Solution belong to the upper-left quadrant: we can
						// connect the spots

						PersistentObject source = idxMapT0.get(i0);
						PersistentObject target = idxMapT1.get(i1);

						// We set the edge weight to be the linking cost, for
						// future reference. This is NOT used in further tracking steps
						final double weight = costMatrix[i0][i1];

						PersistentObject edge = net
								.createEdge(source + "-" + target,
										trackletEdgePartition, source, target);

						net.setEdgeWeight(edge, weight);

						net.addFeature(source,
								TrackingConstants.FEATURE_ISTRACKLETEND, false);

						String startNode = net.getFeatureString(source,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE);

						if (startNode == null) {
							startNode = source.getId();
						}

						net.addFeature(target,
								TrackingConstants.FEATURE_TRACKLETSTARTNODE,
								startNode);

						// count tracklet size
						PersistentObject trackletStartNode = net
								.getNode(startNode);
						Integer trackletSize = net.getIntegerFeature(
								trackletStartNode,
								TrackingConstants.FEATURE_TRACKLET_SIZE);
						if (trackletSize == null) {
							trackletSize = 2;
						} else {
							trackletSize++;
						}
						net.addFeature(trackletStartNode,
								TrackingConstants.FEATURE_TRACKLET_SIZE,
								trackletSize);

					} // otherwise we do not create any connection
				}
			}
		}
	}

	protected AssignmentAlgorithm createAssignmentProblemSolver(
			LAPTrackerVariant variant) {

		// TODO: make extensionpoint
		switch (variant) {
		case Hungarian:
			return new MunkresKuhnAlgorithm();
		case MunkresKuhnAlgorithm:
			return new MunkresKuhnAlgorithm();
		default:
			throw new IllegalArgumentException(
					"Unknown LAP-Tracker Implementation");
		}

	}

	// heavily inspired by LinkingCostMatrixCreator of TrackMate (actually
	// copied ;-))
	private double[][] createCostMatrix(int t0Size, int t1Size,
			KPartiteGraph<PersistentObject, Partition> net, Partition t0,
			Partition t1, HashMap<Integer, PersistentObject> idxMapT0,
			HashMap<Integer, PersistentObject> idxMapT1)
			throws PersistenceException {

		// the cost matrix
		Matrix costs = null;

		// special cases
		if (t1Size == 1) {
			// 0.1 - No spots in late frame -> termination only.
			costs = new Matrix(t0Size, t0Size,
					TrackerKeys.DEFAULT_BLOCKING_VALUE);
			for (int i = 0; i < t0Size; i++) {
				costs.set(i, i, 0);
			}
		}

		if (t0Size == 0) {
			// 0.1 - No spots in late frame -> termination only.
			costs = new Matrix(t1Size, t1Size,
					TrackerKeys.DEFAULT_BLOCKING_VALUE);
			for (int i = 0; i < t1Size; i++) {
				costs.set(i, i, 0);
			}
		}

		if (costs != null)
			return costs.getArray();
		else
			costs = new Matrix(t0Size + t1Size, t0Size + t1Size);

		// fill the quadrants! Important difference to TrackMate: We have done
		// all the feature calculation / edge pruning beforehands in our add
		// distance edges node
		Matrix topLeft = getLinkingCostMatrix(t0Size, t1Size, net, t0, t1,
				idxMapT0, idxMapT1);

		final double alternativeCostFactor = TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
		final double cutoff = alternativeCostFactor * getMaxScore(topLeft);

		//
		Matrix topRight = getAlternativeScores(t0Size, cutoff);
		Matrix bottomLeft = getAlternativeScores(t1Size, cutoff);
		Matrix bottomRight = getLowerRight(topLeft, cutoff);

		// 2 - Fill in complete cost matrix by quadrant
		costs.setMatrix(0, t0Size - 1, 0, t1Size - 1, topLeft);
		costs.setMatrix(t0Size, costs.getRowDimension() - 1, t1Size,
				costs.getColumnDimension() - 1, bottomRight);
		costs.setMatrix(0, t0Size - 1, t1Size, costs.getColumnDimension() - 1,
				topRight);
		costs.setMatrix(t0Size, costs.getRowDimension() - 1, 0, t1Size - 1,
				bottomLeft);

		return costs.getArray();
	}

	/**
	 * Takes the submatrix of costs defined by rows 0 to numRows - 1 and columns
	 * 0 to numCols - 1, transpose it, and sets any non-BLOCKED value to be
	 * cutoff.
	 * <p>
	 * The reasoning for this is explained in the supplementary notes of the
	 * paper, but basically it has to be made this way so that the LAP is
	 * solvable.
	 */
	protected Matrix getLowerRight(Matrix topLeft, double cutoff) {
		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
		Matrix lowerRight = topLeft.transpose();
		for (int i = 0; i < lowerRight.getRowDimension(); i++) {
			for (int j = 0; j < lowerRight.getColumnDimension(); j++) {
				if (lowerRight.get(i, j) < blockingValue) {
					lowerRight.set(i, j, cutoff);
				}
			}
		}
		return lowerRight;
	}

	/**
	 * Sets alternative scores in a new matrix along a diagonal. The new matrix
	 * is n x n, and is set to BLOCKED everywhere except along the diagonal that
	 * runs from top left to bottom right.
	 */
	protected Matrix getAlternativeScores(int n, double cutoff) {
		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
		final Matrix alternativeScores = new Matrix(n, n, blockingValue);

		// Set the cutoff along the diagonal (top left to bottom right)
		for (int i = 0; i < alternativeScores.getRowDimension(); i++) {
			alternativeScores.set(i, i, cutoff);
		}

		return alternativeScores;
	}

	/**
	 * Gets the max score in a matrix m.
	 */
	private double getMaxScore(Matrix m) {
		final double blockingValue = TrackerKeys.DEFAULT_BLOCKING_VALUE;
		double max = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (m.get(i, j) > max && m.get(i, j) < blockingValue) {
					max = m.get(i, j);
				}
			}
		}
		return max;
	}

	public Matrix getLinkingCostMatrix(int t0Size, int t1Size,
			KPartiteGraph<PersistentObject, Partition> net, Partition t0,
			Partition t1, Map<Integer, PersistentObject> idxMapT0,
			Map<Integer, PersistentObject> idxMapT1)
			throws PersistenceException {
		//
		final Matrix m = new Matrix(t0Size, t1Size,
				TrackerKeys.DEFAULT_BLOCKING_VALUE);

		//
		final GraphObjectIterator<PersistentObject> t0Nodes = net.getNodes(t0);

		// fill cost matrix with edges weight
		HashMap<Node, Integer> helperMap = new HashMap<Node, Integer>();

		int nodeIdxT0 = 0;
		while (t0Nodes.hasNext()) {
			// get outgoing edges for current node
			final PersistentObject nodeT0 = t0Nodes.next();
			idxMapT0.put(nodeIdxT0, nodeT0);
			for (PersistentObject outEdge : net
					.getIncidentEdges(nodeT0, t0, t1)) {
				PersistentObject nodeT1 = net.getIncidentNodes(outEdge, t1)
						.next();
				int nodeIdxT1 = getIdx(nodeT1, helperMap);
				
				// as each edge is 1to1
				m.set(nodeIdxT0, nodeIdxT1, net.getEdgeWeight(nodeT0, outEdge));

				idxMapT1.put(nodeIdxT1, nodeT1);
			}
			nodeIdxT0++;
		}

		return m;
	}

	private int getIdx(PersistentObject node,
			HashMap<Node, Integer> integerNodeMap) {

		Integer integer = integerNodeMap.get(node);
		if (integer == null) {
			integer = integerNodeMap.size() + 1;
			integerNodeMap.put(node, integer);
		}

		return integer;
	}

	private int numNodes(KPartiteGraph<PersistentObject, Partition> net,
			Partition partition) throws PersistenceException {
		GraphObjectIterator<PersistentObject> nodes = net.getNodes(partition);
		int ctr = 0;
		while (nodes.hasNext()) {
			nodes.next();
			ctr++;
		}

		return ctr;
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_maxRadius.saveSettingsTo(settings);
		m_threshold.saveSettingsTo(settings);
		m_gaps.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_maxRadius.validateSettings(settings);
		m_threshold.validateSettings(settings);
		m_gaps.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_maxRadius.loadSettingsFrom(settings);
		m_threshold.loadSettingsFrom(settings);
		m_gaps.loadSettingsFrom(settings);
	}
}