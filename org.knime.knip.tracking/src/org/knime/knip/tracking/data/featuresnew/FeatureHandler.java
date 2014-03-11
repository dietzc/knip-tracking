package org.knime.knip.tracking.data.featuresnew;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.knime.knip.tracking.data.graph.TransitionGraph;

public class FeatureHandler {
	
	// linked to maintain order
	private static List<TrackedNodeFeature> trackedNodeFeatures = new LinkedList<TrackedNodeFeature>();
	
	public static void addFeature(TrackedNodeFeature tnf) {
		trackedNodeFeatures.add(tnf);
	}
	
	public static void main(String[] args) {
		System.out.println("TEST");
	}
	
	static {
		for(Method method : ObjectFeatures.class.getDeclaredMethods()) {
			if(method.getReturnType().isAssignableFrom(TrackedNodeFeature.class)) {
				try {
					trackedNodeFeatures.add((TrackedNodeFeature) method.invoke(ObjectFeatures.class, new Object[0]));
				} catch (Exception e) {
					System.err.println("Failed to add TrackedNodeFeature: " + method);
					e.printStackTrace();
				}
				System.out.println("+ " + method);
			} else
				System.out.println("- " + method);
		}
		System.out.println("Class has " + ObjectFeatures.class.getMethods().length + " methods");
		System.out.println("Added " + trackedNodeFeatures.size() + " features");
	}
	
	public static Collection<String> getFeatureNames() {
		Collection<String> featureNames = new LinkedList<String>();
		for(TrackedNodeFeature tnf : trackedNodeFeatures) {
			featureNames.add(tnf.toString());
		}
		return featureNames;
	}
	
	public static double[] getFeatureVector(TransitionGraph tg) {
		double[] vec = new double[trackedNodeFeatures.size()];
		int featNo = 0;
		for(TrackedNodeFeature tnf : trackedNodeFeatures) {
			vec[featNo] = tnf.calc(tg);
			featNo++;
		}
		return vec;
	}

}
