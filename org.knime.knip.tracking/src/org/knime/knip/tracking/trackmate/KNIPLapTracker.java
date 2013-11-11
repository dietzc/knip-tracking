package org.knime.knip.tracking.trackmate;

import java.util.Map;

import org.knime.knip.tracking.data.graph.TrackedNode;

import fiji.plugin.trackmate.FeatureHolderUtils;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.TrackingUtils;
import fiji.plugin.trackmate.tracking.costfunction.CostCalculator;

public class KNIPLapTracker extends FastLAPTracker<TrackedNode> {

	@Override
	protected CostCalculator defaultCostCalculator() {
		return new KNIPCostCalculator();
	};

	// Currently the same implementation
	class KNIPCostCalculator implements CostCalculator {

		@Override
		public double computeLinkingCostFor(TrackableObject t0,
				TrackableObject t1, double distanceCutOff,
				double blockingValue, Map<String, Double> featurePenalties) {

			double d2 = TrackingUtils.squareDistanceTo(t0, t1);

			// Distance threshold
			if (d2 > distanceCutOff * distanceCutOff) {
				return blockingValue;
			}

			double penalty = 1;
			for (String feature : featurePenalties.keySet()) {
				double ndiff = FeatureHolderUtils.normalizeDiffToSp(t0, t1,
						feature);
				if (Double.isNaN(ndiff))
					continue;
				double factor = featurePenalties.get(feature);
				penalty += factor * 1.5 * ndiff;
			}

			// Set score
			return d2 * penalty * penalty;

		}

	}
}
