package org.knime.knip.tracking;

public interface TrackableObject<L extends Comparable<L>> extends
		Comparable<TrackableObject<L>>,
		fiji.plugin.trackmate.tracking.TrackableObject {

	int id();

}
