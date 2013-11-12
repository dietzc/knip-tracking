package org.knime.knip.tracking.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.knime.network.core.api.Partition;

/**
 * Sorts partitions of a network.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class PartitionSorter {
	/**
	 * Sorts a {@link List} of {@link Partition} according to its name. The
	 * original list isn't changed by this method! * Fix 29.11.2012: ignore
	 * strange 'nodes' partition which appears somehow after saving
	 * 
	 * @param partitions
	 *            the partitions to sort
	 */
	public static List<Partition> sortTimePartitions(List<Partition> partitions) {
		List<Partition> temp = new LinkedList<Partition>(partitions);
		ListIterator<Partition> it = temp.listIterator();
		while (it.hasNext()) {
			if (it.next().getId().equals("nodes")) {
				it.remove();
			}
		}
		Collections.sort(temp, new PartitionComparator());
		return temp;
	}
}
