package org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator;

public class Setting {
	public long[] dimension;
	public int time;

	public Setting(String code) {
		String[] parts = code.split("x");
		dimension = new long[parts.length];
		for (int d = 0; d < parts.length; d++) {
			dimension[d] = Long.parseLong(parts[d]);
		}
		time = (int) dimension[dimension.length - 1];
	}
}
