package org.knime.knip.tracking;

public class TrackableObjectHolder<L extends Comparable<L>> implements
		Comparable<TrackableObjectHolder<L>> {

	public final static String TRACK_ID_PREFIX = "Track";

	private final TrackableObject<L> obj;

	private int trackId;

	public TrackableObjectHolder(final int trackId, final TrackableObject<L> obj) {
		this.obj = obj;
		this.trackId = trackId;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int compareTo(TrackableObjectHolder<L> o) {
		return obj.id() - o.obj.id();
	}

	@Override
	public String toString() {
		return "TrackableObject[" + trackId + "] with ObjectID " + "[" + trackId
				+ "]";
	}
}
