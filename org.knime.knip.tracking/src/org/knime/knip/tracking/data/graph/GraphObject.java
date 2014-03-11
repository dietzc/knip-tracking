package org.knime.knip.tracking.data.graph;

import java.util.LinkedList;
import java.util.List;

import org.knime.network.core.api.Feature;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;

public abstract class GraphObject {
	private PersistentObject pObj;
	private KPartiteGraph<PersistentObject, Partition> net;

	public GraphObject(KPartiteGraph<PersistentObject, Partition> net,
			PersistentObject pObj) {
		this.pObj = pObj;
		this.net = net;
	}

	public PersistentObject getPersistentObject() {
		return this.pObj;
	}

	public KPartiteGraph<PersistentObject, Partition> getNetwork() {
		return this.net;
	}

	public boolean hasFeature(String featureName) {
		try {
			return net.isFeatureDefined(featureName);
		} catch (PersistenceException e) {
		}
		return false;
	}

	public double getDoubleFeature(String featureName) {
		try {
			Double result = net.getDoubleFeature(pObj, featureName);
			// avoid NPE
			if (result == null)
				return 0.0f;
			return result;
		} catch (InvalidFeatureException e) {
			// assume the guy knows what he does..
			//e.printStackTrace();
		} catch (PersistenceException e) {
			//e.printStackTrace();
		}
		return Double.NaN;
	}

	public int getIntegerFeature(String featureName) {
		try {
			return net.getIntegerFeature(pObj, featureName);
		} catch (InvalidFeatureException e) {
			// assume the guy knows what he does..
		} catch (PersistenceException e) {
		}
		return Integer.MIN_VALUE;
	}

	public String getID() {
		return pObj.getId();
	}

	public String getStringFeature(String featureName) {
		try {
			return net.getStringFeature(pObj, featureName);
		} catch (InvalidFeatureException e) {
			// assume the guy knows what he does..
		} catch (PersistenceException e) {
		}
		return pObj.getId();
	}

	public String toString() {
		return getID();
	}

	public List<String> getDoubleFeatures() {
		List<String> result = new LinkedList<String>();
		try {
			for (Feature feature : net.getFeatures()) {
				if (feature.getType().isNumeric())
					result.add(feature.getName());
			}
		} catch (PersistenceException e) {

		}
		return result;
	}

	public double featureDistance(GraphObject otherGraphObject) {
		double featureDist = 0.0;
		for (String featureName : getDoubleFeatures()) {
			featureDist += (getDoubleFeature(featureName) - otherGraphObject
					.getDoubleFeature(featureName))
					* (getDoubleFeature(featureName) - otherGraphObject
							.getDoubleFeature(featureName));
		}
		return Math.sqrt(featureDist);
	}
}
