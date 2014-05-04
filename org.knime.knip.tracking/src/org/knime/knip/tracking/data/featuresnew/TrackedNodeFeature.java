package org.knime.knip.tracking.data.featuresnew;

import java.util.LinkedList;
import java.util.List;

import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.MathUtils;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.core.feature.FeatureTypeFactory;

public abstract class TrackedNodeFeature {

	protected String name = "";

	private static final String NETWORK_FEATURE_PREFIX = "tnfNetPrefix_";

	public TrackedNodeFeature(String name) {
		this.name = name;
	}

	protected String getNetworkFeatureName() {
		return NETWORK_FEATURE_PREFIX + name;
	}

	/**
	 * Dimension of the feature value, usually 1
	 * 
	 * @return the number of dimensions of the feature value
	 */
	protected int getFeatureDimension() {
		return 1;
	}

	/**
	 * Calculates feature.
	 * 
	 * @param node
	 *            the node
	 */
	public abstract double[] calcInt(TrackedNode node);

	public double[] calc(TrackedNode node) {
		double[] vals = calcInt(node);
		node.setNetworkFeature(getNetworkFeatureName(), vals);
		return vals;
	}

	/**
	 * Complete calculation with storage in network.
	 * 
	 * @param tg
	 *            the transition graph
	 * @return the feature value
	 */
	public double calc(TransitionGraph tg) {
		List<double[]> fp = new LinkedList<double[]>();
		List<double[]> lp = new LinkedList<double[]>();
		if (!tg.hasFeature(getNetworkFeatureName())) {
			try {
				tg.getNet().defineFeature(FeatureTypeFactory.getStringType(),
						getNetworkFeatureName());
			} catch (PersistenceException e) {
				e.printStackTrace();
			} catch (InvalidFeatureException e) {
				e.printStackTrace();
			}
			for (TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				double[] vals = calc(node);
				fp.add(vals);
				node.setNetworkFeature(getNetworkFeatureName(), vals);
			}
			for (TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				double[] vals = calc(node);
				lp.add(vals);
				node.setNetworkFeature(getNetworkFeatureName(), vals);
			}
		} else {
			// already calculated, just get the results
			for (TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				double[] vals = node.getNetworkFeature(getNetworkFeatureName());
				fp.add(vals);
			}
			for (TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				double[] vals = node.getNetworkFeature(getNetworkFeatureName());
				lp.add(vals);
			}
		}
		if (fp.isEmpty() && lp.isEmpty()) {
			System.out.println("Both arrays null? Bug?");
			return Double.NaN;
		}
		if (fp.isEmpty()) {
			// TODO: frame avg?
		}
		if (lp.isEmpty()) {
			// TODO: frame avg?
		}
		return diff(fp, lp, tg);
	}

	/**
	 * Difference between two list of features.
	 * Default is euclidean distance between sums.
	 * @param fpVals feature(s) of first partition
	 * @param lpVals feature(s) of last partition
	 * @return the distance
	 */
	public double diff(List<double[]> fpVals, List<double[]> lpVals, TransitionGraph tg) {
		double[] fp = MathUtils.avg(fpVals);
		double[] lp = MathUtils.avg(lpVals);
//		System.out.println(this.getNetworkFeatureName() + ": ");
//		System.out.println(str(fpVals) + " -> " + Arrays.toString(fp));
//		System.out.println(str(lpVals) + " -> " + Arrays.toString(lp));
		return euclideanDistance(fp, lp);
	}
	
//	private String str(List<double[]> vals) {
//		StringBuilder sb = new StringBuilder();
//		for(double[] arr : vals) {
//			sb.append(Arrays.toString(arr));
//			sb.append(" ");
//		}
//		return sb.toString();
//	}

	protected double euclideanDistanceSqr(double[] fpVals, double[] lpVals) {
		double res = 0.0;
		for (int d = 0; d < fpVals.length; d++) {
			res += (fpVals[d] - lpVals[d]) * (fpVals[d] - lpVals[d]);
		}
		return res;
	}

	protected double euclideanDistance(double[] fpVals, double[] lpVals) {
		return Math.sqrt(euclideanDistanceSqr(fpVals, lpVals));
	}

	protected double earthMoverDistance(double[] fpVals, double[] lpVals) {
		System.out.println("dunno what to do");
		return Double.NaN;
	}

	protected double anglePatternDistance(double[] center, List<double[]> centers) {
		double[] a = centers.get(0);
		double[] b = centers.get(1);
		
		double[] u = MathUtils.subtract(center, a);
		double[] v = MathUtils.subtract(center, b);
		u = MathUtils.normalize(u);
		v = MathUtils.normalize(v);
		return MathUtils.dot(u, v);
	}

	@Override
	public String toString() {
		return name;
	}
}
