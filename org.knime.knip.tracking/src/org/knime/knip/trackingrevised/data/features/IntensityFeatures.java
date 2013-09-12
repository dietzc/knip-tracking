package org.knime.knip.trackingrevised.data.features;

import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

public class IntensityFeatures extends FeatureClass {
	@Override
	public String getName() {
		return "Intensity features";
	}

	@Feature(name = "Intensity difference")
	public static double histoDifference(TransitionGraph tg) {
		return Double.NaN;
	}
}
