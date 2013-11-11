package org.knime.knip.tracking.util;

import java.util.Comparator;

/**
 * Comparator for comparing partition names. Only valid for names like t###
 * where ### is an integer number.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class PartitionComparatorString implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		if (o1 == null || o2 == null)
			return 0;
		if (!o1.startsWith("t") || !o2.startsWith("t")) {
			throw new IllegalArgumentException(o1 + " or " + o2
					+ " is no valid time partition name.\n" + o1 + " vs " + o2);
		}
		Integer i1 = Integer.parseInt(o1.substring(1));
		Integer i2 = Integer.parseInt(o2.substring(1));
		return i1.compareTo(i2);
	}

}
