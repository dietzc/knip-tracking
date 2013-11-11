package org.knime.knip.tracking.nodes.trackletcombiner.hypothesis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.tracking.data.Interval;
import org.knime.knip.tracking.nodes.trackletcombiner.solver.ApacheCommonsMathSolver;
import org.knime.knip.tracking.nodes.trackletcombiner.solver.LPSolveSolver;
import org.knime.knip.tracking.nodes.trackletcombiner.solver.Solver;
import org.knime.knip.tracking.util.OffsetHandling;
import org.knime.knip.tracking.util.PartitionComparator;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.feature.FeatureTypeFactory;

public class HypothesesHandler {
	private List<Hypothesis> hypotheses = new LinkedList<Hypothesis>();

	private int trackletCounter = 0;

	private IntervalValue intervalValue;

	private KPartiteGraph<PersistentObject, Partition> net;

	// private List<KDTree<PersistentObject>> kdtrees = new
	// LinkedList<KDTree<PersistentObject>>();

	private static List<Class<? extends Hypothesis>> HYPOTHESESCLASSES = new LinkedList<Class<? extends Hypothesis>>();

	private double lamda1, lamda2, lamda3, deltaS, deltaT, alpha;

	private ExecutionContext exec;

	private long timeout;

	static {
		//
	}

	public HypothesesHandler(KPartiteGraph<PersistentObject, Partition> net,
			ExecutionContext exec, double lamda1, double lamda2, double lamda3,
			double deltaS, double deltaT, double alpha, long timeout)
			throws Exception {

		this.net = net;
		this.exec = exec;

		if (!net.isFeatureDefined(TrackingConstants.NETWORK_FEATURE_DIMENSION)) {
			throw new InvalidSettingsException(
					"Network contains no dimension feature. Please recreate network.");
		}
		long[] dims = OffsetHandling.decode(net.getFeatureString(net,
				TrackingConstants.NETWORK_FEATURE_DIMENSION));

		intervalValue = Interval.createFromDimension(dims);

		this.lamda1 = lamda1;
		this.lamda2 = lamda2;
		this.lamda3 = lamda3;
		this.deltaS = deltaS;
		this.deltaT = deltaT;
		this.alpha = alpha;

		this.timeout = timeout;

		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.FEATURE_TRACKLET_NUMBER);
		// count tracklets (needed for matrix)
		List<Partition> partitions = new LinkedList<Partition>(
				net.getPartitions(PartitionType.NODE));
		Collections.sort(partitions, new PartitionComparator());
		for (Partition p : partitions) {
			// List<PersistentObject> nodes = new
			// LinkedList<PersistentObject>();
			// List<RealLocalizable> positions = new
			// LinkedList<RealLocalizable>();
			for (PersistentObject node : net.getNodes(p)) {
				if (net.getStringFeature(node,
						TrackingConstants.FEATURE_TRACKLETSTARTNODE) == null) {
					net.addFeature(node,
							TrackingConstants.FEATURE_TRACKLET_NUMBER,
							trackletCounter);
					trackletCounter++;
				}

				// if((net.getStringFeature(node,
				// TrackingConstants.FEATURE_TRACKLETSTARTNODE) == null) ||
				// net.getBooleanFeature(node,
				// TrackingConstants.FEATURE_ISTRACKLETEND)) {
				// nodes.add(node);
				// ImgPlusCell ipc = ((ImgPlusCell)net.getFeatureCell(node,
				// TrackingConstants.FEATURE_BITMASK));
				//
				// long[] offset = ipc.getMinimum();
				// long[] imgMax = ipc.getMinimum();
				//
				// double[] min = new double[offset.length];
				// double[] max = new double[offset.length];
				// for(int d = 0; d < min.length; d++) {
				// min[d] = offset[d];
				// max[d] = offset[d]+imgMax[d];
				// }
				// }
				// }
				// kdtrees.add(new KDTree<PersistentObject>(nodes, positions));
			}
		}

		generateProblem(FileStoreFactory.createWorkflowFileStoreFactory(exec));
		solveProblem();
	}

	private void solveProblem() {

		exec.setMessage("Solving...");
		List<LinkHypothesis> links = new LinkedList<LinkHypothesis>();
		List<DividingHypothesis> divs = new LinkedList<DividingHypothesis>();
		LinkedList<InitalizationHypothesis> inits = new LinkedList<InitalizationHypothesis>();
		LinkedList<TerminationHypothesis> terms = new LinkedList<TerminationHypothesis>();
		LinkedList<FalsePositiveHypothesis> fps = new LinkedList<FalsePositiveHypothesis>();
		LinkedList<MergeHypothesis> merges = new LinkedList<MergeHypothesis>();
		for (Hypothesis hypo : hypotheses) {
			if (hypo instanceof LinkHypothesis)
				links.add((LinkHypothesis) hypo);
			if (hypo instanceof DividingHypothesis)
				divs.add((DividingHypothesis) hypo);
			if (hypo instanceof InitalizationHypothesis) {
				inits.add((InitalizationHypothesis) hypo);
			}
			if (hypo instanceof TerminationHypothesis) {
				terms.add((TerminationHypothesis) hypo);
			}
			if (hypo instanceof FalsePositiveHypothesis) {
				fps.add((FalsePositiveHypothesis) hypo);
			}
			if (hypo instanceof MergeHypothesis) {
				merges.add((MergeHypothesis) hypo);
			}
			if (Double.isInfinite(hypo.propability)) {
				throw new RuntimeException("Probability is infinte: " + hypo);
			}
		}

		// System.out.println("--- inits ---");
		// for (Hypothesis hypo : inits) {
		// System.out.println(hypo);
		// }
		// System.out.println("--- links ---");
		// for (Hypothesis hypo : links) {
		// System.out.println(hypo);
		// }
		// System.out.println("--- divs ---");
		// for (Hypothesis hypo : divs) {
		// System.out.println(hypo);
		// }
		// System.out.println("--- merges ---");
		// for (Hypothesis hypo : merges) {
		// System.out.println(hypo);
		// }
		// System.out.println("--- terms ---");
		// for (Hypothesis hypo : terms) {
		// System.out.println(hypo);
		// }
		System.out.println("#inits: " + inits.size());
		System.out.println("#terms: " + terms.size());
		System.out.println("#fps: " + fps.size());
		System.out.println("#links: " + links.size());
		System.out.println("#divs: " + divs.size());
		System.out.println("#merges: " + merges.size());

		// System.out.println(fps.size());

		System.out.println("We got: " + trackletCounter + " tracklets and "
				+ hypotheses.size() + " hypotheses");

		System.out
				.println("needed size in bytes: "
						+ (2L * trackletCounter * hypotheses.size() * 32)
						+ " in mb: "
						+ (2L * trackletCounter * hypotheses.size() * 32)
						/ 1024 / 1024);

		@SuppressWarnings("unchecked")
		List<Integer>[] transposedIndices = new List[2 * trackletCounter];

		for (int i = 0; i < transposedIndices.length; i++)
			transposedIndices[i] = new LinkedList<Integer>();

		double[] propabilities = new double[hypotheses.size()];
		Iterator<Hypothesis> it = hypotheses.iterator();
		for (int row = 0; row < hypotheses.size(); row++) {
			Hypothesis hypo = it.next();
			for (int index : hypo.indices) {
				transposedIndices[index].add(row);
			}
			propabilities[row] = hypo.propability;

			// System.out.println(hypo.indices + "\t\t" + hypo);
		}

		// System.out.println();

		// for(List<Integer> list : transposedIndices) {
		// System.out.println(list);
		// }
		//
		// System.out.println();

		Solver solver = new ApacheCommonsMathSolver();
		// try all solvers for debug reasons
		// try {
		// solver.solve(transposedIndices, hypotheses.size(), propabilities);
		// } catch (Throwable e2) {
		// e2.printStackTrace();
		// }
		solver = new LPSolveSolver();
		double[] result = new double[0];
		try {
			result = solver.solve(transposedIndices, hypotheses.size(),
					propabilities, timeout, exec);
		} catch (Throwable e1) {
			throw new RuntimeException(e1);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException e1) {
			return;
		}

		if (result.length < 100)
			System.out.println(Arrays.toString(result));

		exec.setMessage("Applying result to network");

		for (int d = 0; d < result.length; d++) {
			if (result[d] > 0.5) {
				// if (result.length < 100)
				// System.out.println(result[d] + " -> " + hypotheses.get(d));
				try {
					hypotheses.get(d).apply();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			if (result[d] != 0 && result[d] != 1) {
				System.err.println(result[d] + " @ " + hypotheses.get(d));
				System.err.flush();
			}
		}
	}

	public void generateProblem(final FileStoreFactory factory)
			throws Exception {
		exec.setMessage("Generating problem");

		HYPOTHESESCLASSES.clear();
		HYPOTHESESCLASSES.add(InitalizationHypothesis.class);
		HYPOTHESESCLASSES.add(TerminationHypothesis.class);
		HYPOTHESESCLASSES.add(FalsePositiveHypothesis.class);
		HYPOTHESESCLASSES.add(LinkHypothesis.class);
		HYPOTHESESCLASSES.add(DividingHypothesis.class);
		HYPOTHESESCLASSES.add(MergeHypothesis.class);

		ThreadPool myPool = KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool();

		final List<Partition> partitions = new ArrayList<Partition>(
				net.getPartitions(PartitionType.NODE));
		Collections.sort(partitions, new PartitionComparator());

		List<Future<?>> futures = new LinkedList<Future<?>>();

		hypotheses = Collections.synchronizedList(hypotheses);

		final AtomicInteger counter = new AtomicInteger();
		for (int currentPartitionIdx = 0; currentPartitionIdx < partitions
				.size(); currentPartitionIdx++) {
			final int tmpIdx = currentPartitionIdx;
			exec.checkCanceled();
			Runnable task = new Runnable() {
				public void run() {
					Partition partition = partitions.get(tmpIdx);
					try {
						for (PersistentObject node : net.getNodes(partition)) {
							exec.checkCanceled();
							for (Class<? extends Hypothesis> hypothesis : HYPOTHESESCLASSES) {
								Hypothesis h = hypothesis.newInstance();
								h.setExecutionContext(exec);
								h.setNumberOfTracklets(trackletCounter);
								h.setParameters(lamda1, lamda2, lamda3, deltaS,
										deltaT, alpha);
								final List<Hypothesis> hypos = h.create(
										factory, net, node, partitions, tmpIdx,
										intervalValue);
								hypotheses.addAll(hypos);
								// if(node.getId().equals("225") ||
								// node.getId().equals("670")) {
								// System.out.println("\t" + hypos);
								// }
							}
						}
						counter.incrementAndGet();
						exec.setProgress(
								counter.doubleValue() / partitions.size(),
								"Generating problem: Finished Partition "
										+ counter + "/" + partitions.size());
					} catch (Exception e) {
						e.printStackTrace();
					}

					// System.out.println("Finished Partition #" + (tmpIdx));
				};
			};
			futures.add(myPool.enqueue(task));
		}

		myPool.waitForTermination();
	}
}
