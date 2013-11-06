package org.knime.knip.trackingrevised.data.features;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.ConvexHull2D;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.trackingrevised.data.graph.TrackedNode;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

public class ShapeFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Shape Features";
	}

	@Feature(name = "diff shape")
	public static <T extends RealType<T>> double diffShape(TransitionGraph tg) {
		return 0.0;
	}

	@Feature(name = "shape evenness")
	public static <T extends RealType<T>> double shapeEvenness(
			TransitionGraph tg) {
		return 0.0;
	}

	/**
	 * Returns the shape compactness. It compares the sizes of a single segment
	 * with the size of the convex hull of the other segments.
	 * 
	 * @param tg
	 *            the graph
	 * @return the shape compactness value
	 */
	@Feature(name = "Shape Compactness")
	public static <T extends RealType<T>> double shapeCompactness(
			TransitionGraph tg) {
		if (tg.getNodes(tg.getFirstPartition()).size() == 0
				|| tg.getNodes(tg.getLastPartition()).size() == 0)
			return Double.NaN;

		// remember.. there are only 1:n and m:1 transitions generated!

		Img<BitType> fpImg = tg.getNodes(tg.getFirstPartition()).iterator()
				.next().getBitmask().getImgPlus().getImg();
		Img<BitType> lpImg = tg.getNodes(tg.getLastPartition()).iterator()
				.next().getBitmask().getImgPlus().getImg();
		if (tg.getNodes(tg.getFirstPartition()).size() > 1) {
			fpImg = createPartitionImg(tg, tg.getFirstPartition());
			ConvexHull2D<Img<BitType>> convexHull = new ConvexHull2D<Img<BitType>>(
					0, 1, true);
			convexHull.compute(fpImg, fpImg);
		}
		if (tg.getNodes(tg.getLastPartition()).size() > 1) {
			lpImg = createPartitionImg(tg, tg.getLastPartition());
			ConvexHull2D<Img<BitType>> convexHull = new ConvexHull2D<Img<BitType>>(
					0, 1, true);
			convexHull.compute(lpImg, lpImg);
		}

		double fpNumPix = 0;
		for (BitType b : fpImg)
			if (b.get())
				fpNumPix++;

		double lpNumPix = 0;
		for (BitType b : lpImg)
			if (b.get())
				lpNumPix++;

		return fpNumPix / lpNumPix;
	}

	/**
	 * Creates a {@link Img} of the given partition.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @param partition
	 *            the partition
	 * @return an {@link Img} with all nodes of the partition
	 */
	private static Img<BitType> createPartitionImg(TransitionGraph tg,
			String partition) {
		Collection<TrackedNode> nodes = tg.getNodes(partition);
		Rectangle2D rect = null;
		for (TrackedNode node : nodes) {
			if (rect == null) {
				rect = node.getImageRectangle();
			} else {
				rect.add(node.getImageRectangle());
			}
		}
		long[] dims = new long[2];
		dims[0] = (long) rect.getWidth();
		dims[1] = (long) rect.getHeight();
		Img<BitType> img = new ArrayImgFactory<BitType>().create(dims,
				new BitType());
		RandomAccess<BitType> ra = img.randomAccess();
		long[] pos = new long[2];
		for (TrackedNode node : nodes) {
			Rectangle2D imgRect = node.getImageRectangle();
			Cursor<BitType> cursor = node.getBitmask().getImgPlus()
					.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				pos[0] = cursor.getLongPosition(0)
						+ (long) (imgRect.getMinX() - rect.getMinX());
				pos[1] = cursor.getLongPosition(1)
						+ (long) (imgRect.getMinY() - rect.getMinY());
				ra.setPosition(pos);
				ra.get().set(cursor.get());
			}
		}
		return img;
	}
}
