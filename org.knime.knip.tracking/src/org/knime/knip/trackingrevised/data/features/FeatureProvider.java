package org.knime.knip.trackingrevised.data.features;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

public class FeatureProvider {

	// do not instantiate
	private FeatureProvider() {

	}

	private static List<Class<? extends FeatureClass>> featureClasses = new LinkedList<Class<? extends FeatureClass>>();
	private static int featureCount = 0;

	static {
		addFeatureClass(TransitionFeatures.class);
		addFeatureClass(IntensityFeatures.class);
	}

	public static double[] getFeatureVector(TransitionGraph tg) {
		double[] vec = new double[featureCount];

		int index = 0;
		for (Class<?> featureClass : featureClasses) {
			for (Method method : featureClass.getMethods()) {
				if (method.isAnnotationPresent(Feature.class)) {
					Double value = 0.0;
					if (isFeatureApplyable(tg, method))
						try {
							value = (Double) method.invoke(featureClass, tg);
						} catch (Exception e) {
							e.printStackTrace();
							System.err
									.println("Exception on invoking feature method: "
											+ method
											+ " of FeatureClass:"
											+ featureClass);
						}
					if (Double.isNaN(value))
						value = 0.0;
					vec[index++] = value;

				}
			}
		}

		return vec;
	}

	public static String[] getFeatureNames() {
		String[] names = new String[featureCount];
		int index = 0;
		for (Class<?> featureClass : featureClasses) {
			for (Method method : featureClass.getMethods()) {
				Feature annotation = method.getAnnotation(Feature.class);
				if (annotation != null) {
					names[index++] = annotation.name();
				}
			}
		}
		return names;
	}

	private static void addFeatureClass(Class<? extends FeatureClass> clazz) {
		featureClasses.add(clazz);
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(Feature.class)) {
				featureCount++;
			}
		}
	}

	private static boolean isFeatureApplyable(TransitionGraph tg,
			Method featureMethod) {
		Feature feat = featureMethod.getAnnotation(Feature.class);
		for (String neededFeature : feat.neededFeatures()) {
			if (!tg.hasFeature(neededFeature))
				return false;
		}
		return true;
	}
}
