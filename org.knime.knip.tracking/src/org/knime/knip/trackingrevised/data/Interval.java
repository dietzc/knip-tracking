package org.knime.knip.trackingrevised.data;

import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.Named;
import net.imglib2.meta.Sourced;

import org.knime.knip.base.data.IntervalValue;

/**
 * Direct implementation of the {@link IntervalValue} interface.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class Interval implements IntervalValue {

	private long[] minimum;
	private long[] maximum;

	private Named name;
	private Sourced source;
	private CalibratedSpace<CalibratedAxis> calibratedSpace;

	public static Interval createFromDimension(long[] dimension) {
		long[] max = new long[dimension.length];
		for (int d = 0; d < dimension.length; d++) {
			max[d] = dimension[d] - 1;
		}
		return new Interval(new long[dimension.length], max);
	}

	public Interval(long[] minimum, long[] maximum) {
		this(minimum, maximum, null, null, null);
	}

	public Interval(long[] minimum, long[] maximum, Named name, Sourced source,
			CalibratedSpace<CalibratedAxis> calibratedSpace) {
		this.minimum = minimum;
		this.maximum = maximum;
		this.name = name;
		this.source = source;
		this.calibratedSpace = calibratedSpace;
	}

	@Override
	public long[] getMinimum() {
		return minimum;
	}

	@Override
	public long[] getMaximum() {
		return maximum;
	}

	@Override
	public Named getName() {
		return name;
	}

	@Override
	public Sourced getSource() {
		return source;
	}

	@Override
	public CalibratedSpace<CalibratedAxis> getCalibratedSpace() {
		return calibratedSpace;
	}

}
