package org.knime.knip.trackingrevised.nodes.trackingimagegenerator.generator;

import java.util.Arrays;

public class Segment {

	private static final int DEFAULT_RADIUS = 5;

	public Segment(double[] position) {
		this(position, DEFAULT_RADIUS, (byte) 255);
	}

	public Segment(double[] position, byte color) {
		this(position, DEFAULT_RADIUS, color);
	}

	public Segment(double[] position, int radius, byte color) {
		this.position = position.clone();
		this.radius = radius;
		this.color = color;
		this.time = (int) Math.round(position[position.length - 1]);
	}

	public double[] position;
	public int radius = DEFAULT_RADIUS;
	public byte color = (byte) 255;
	public int time = 0;

	public String toString() {
		return Arrays.toString(position);
	}

	public long[] longPosition() {
		long[] pos = new long[position.length];
		for (int d = 0; d < pos.length; d++) {
			pos[d] = Math.round(position[d]);
		}
		return pos;
	}
}
