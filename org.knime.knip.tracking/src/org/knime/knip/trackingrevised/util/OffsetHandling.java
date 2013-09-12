package org.knime.knip.trackingrevised.util;

public class OffsetHandling {

	private static final String SEPERATOR = "|";

	public static String encode(long[] offset) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < offset.length; i++) {
			result.append(offset[i]);
			if (i < offset.length - 1)
				result.append(SEPERATOR);
		}
		return result.toString();
	}

	public static long[] decode(String offString) {
		if (offString.trim().isEmpty())
			throw new IllegalArgumentException(
					"Offset string must not be empty.");
		String parts[] = offString.split("\\|");
		long[] result = new long[parts.length];
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].isEmpty())
				throw new IllegalArgumentException("Offset string broken: "
						+ offString + " @index: " + i);
			result[i] = Long.parseLong(parts[i]);
		}
		return result;
	}
}
