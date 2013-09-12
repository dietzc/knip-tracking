package org.knime.knip.trackingrevised.util;

import java.util.Comparator;

import org.knime.network.core.api.Partition;

/**
 * Comparator for comparing partition names. Only valid for names like t###
 * where ### is an integer number.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class PartitionComparator implements Comparator<Partition> {

	@Override
	public int compare(Partition o1, Partition o2) {
		if (o1 == null || o2 == null)
			return 0;
		if (!o1.getId().startsWith("t") || !o2.getId().startsWith("t")) {
			throw new IllegalArgumentException(o1 + " or " + o2
					+ " is no valid time partition name.\n" + o1.getId()
					+ " vs " + o2.getId());
		}
		Integer i1 = Integer.parseInt(o1.getId().substring(1));
		Integer i2 = Integer.parseInt(o2.getId().substring(1));
		return i1.compareTo(i2);
	}

}
