package com.telmomenezes.jfastemd;

import org.knime.knip.tracking.util.MathUtils;

public class Feature1D implements Feature {
	
	private int x;
	
	public Feature1D(int x) {
		this.x = x;
	}
	
	@Override
	public double groundDist(Feature f) {
		Feature1D f1d = (Feature1D)f;
		return Math.abs(x-f1d.x);
	}
	
	public static Signature createSignature(double[] vals) {
		Feature1D[] features = new Feature1D[vals.length];
		double[] weights = new double[vals.length];
		for(int x = 0; x < vals.length; x++) {
			features[x] = new Feature1D(x);
			weights[x] = vals[x];
		}
		
		Signature signature = new Signature();
		signature.setNumberOfFeatures(features.length);
		signature.setFeatures(features);
		signature.setWeights(weights);
		
		return signature;
	}
	
	public static void main(String[] args) {
		double[] hist1 = new double[] {3,2,1};
		double[] hist2 = new double[] {1,2,3};
		
		hist1 = MathUtils.normalizeHistogram(hist1);
		hist2 = MathUtils.normalizeHistogram(hist2);
		
		Signature sig1 = createSignature(hist1);
		Signature sig2 = createSignature(hist2);
		
		System.out.println(JFastEMD.distance(sig1, sig2, 0));
	}
}
