package org.knime.knip.trackingrevised.data;

import net.imglib2.img.Img;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.Metadata;
import net.imglib2.type.numeric.RealType;

@Deprecated
public class OffsetImgPlus<T extends RealType<T>> extends ImgPlus<T> {

	private long[] offset;

	public OffsetImgPlus(Img<T> img, Metadata metadata) {
		super(img, metadata);
	}

	public OffsetImgPlus(Img<T> img, String name, AxisType[] axes, double[] cal) {
		super(img, name, axes, cal);
	}

	public OffsetImgPlus(Img<T> img, String name, AxisType[] axes) {
		super(img, name, axes);
	}

	public OffsetImgPlus(Img<T> img, String name) {
		super(img, name);
	}

	public OffsetImgPlus(Img<T> img) {
		super(img);
	}

	public OffsetImgPlus(ImgPlus<T> imgPlus, long[] offset) {
		this(imgPlus, imgPlus);
		setOffset(offset);
	}

	public void setOffset(final long[] offset) {
		if (offset.length != getImg().numDimensions())
			throw new IllegalArgumentException(
					"Number of offset dimensions does not match.");
		this.offset = offset.clone();
	}

	public long[] getOffset() {
		return this.offset;
	}
}
