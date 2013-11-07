package org.knime.knip.tracking.data;

import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;

import org.knime.core.data.RowKey;

public class Segment {

	private RowKey key;
	private Img<BitType> bitmask;
	private Interval interval;
	private double[] features;

	public Segment(RowKey key, Img<BitType> bitmask, Interval interval,
			double... features) {
		this.key = key;
		this.bitmask = bitmask;
		this.features = features;
	}

	public RowKey getRowKey() {
		return key;
	}

	public Img<BitType> getBitmask() {
		return bitmask;
	}

	public Interval getInterval() {
		return interval;
	}

	public double[] getFeatures() {
		return features;
	}
}
