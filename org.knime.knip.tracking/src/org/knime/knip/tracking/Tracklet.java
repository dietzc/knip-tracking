package org.knime.knip.tracking;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class Tracklet<L extends Comparable<L>> {

	private final int id;

	private final SortedSet<TrackableObject<L>> list;

	public Tracklet(final int id) {
		this.id = id;
		this.list = new TreeSet<TrackableObject<L>>(new TrackletComparator());
	}

	public void addTrackableObject(TrackableObject<L> o) {
		list.add(o);
	}

	public Iterator<TrackableObject<L>> iterator() {
		return list.iterator();
	}

	public int numElements() {
		return list.size();
	}

	public int id() {
		return id;
	}

	class TrackletComparator implements Comparator<TrackableObject<L>> {

		@Override
		public int compare(TrackableObject<L> o1, TrackableObject<L> o2) {
			return o2.frame() - o1.frame();
		}
	}

}
