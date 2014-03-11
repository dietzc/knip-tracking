package org.knime.knip.tracking.data.featuresnew;

import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.DoubleHandler;
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
		double[] fpavg = null;
		double[] lpavg = null;
		if (!tg.hasFeature(getNetworkFeatureName())) {
			try {
				tg.getNet().defineFeature(FeatureTypeFactory.getStringType(), getNetworkFeatureName());
			} catch (PersistenceException e) {
				e.printStackTrace();
			} catch (InvalidFeatureException e) {
				e.printStackTrace();
			}
			for (TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				double[] vals = calc(node);
				if (fpavg == null)
					fpavg = new double[vals.length];
				DoubleHandler.increaseBy(fpavg, vals);
				node.setNetworkFeature(getNetworkFeatureName(), vals);
			}
			for (TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				double[] vals = calc(node);
				if (lpavg == null)
					lpavg = new double[vals.length];
				DoubleHandler.increaseBy(lpavg, vals);
				node.setNetworkFeature(getNetworkFeatureName(), vals);
			}
		} else {
			//already calculated, just get the results
			for(TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				double[] vals = node.getNetworkFeature(getNetworkFeatureName());
				if(fpavg == null)
					fpavg = new double[vals.length];
				DoubleHandler.increaseBy(fpavg, vals);
			}
			for(TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				double[] vals = node.getNetworkFeature(getNetworkFeatureName());
				if(lpavg == null)
					lpavg = new double[vals.length];
				DoubleHandler.increaseBy(lpavg, vals);
			}
		}
		return diff(fpavg, lpavg);
	}

	public double diff(double[] fpVals, double[] lpVals) {
		double res = 0.0;
		for (int d = 0; d < fpVals.length; d++) {
			res += (fpVals[d] * fpVals[d]) - (lpVals[d] * lpVals[d]);
		}
		return Math.sqrt(res);
	}

	@Override
	public String toString() {
		return name;
	}
}
