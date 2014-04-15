package org.knime.knip.tracking.data.features;

import net.imglib2.img.Img;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.ConvexHull2D;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.TransitionGraphUtil;

@Deprecated
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
			fpImg = TransitionGraphUtil.createPartitionImg(tg, tg.getFirstPartition());
			ConvexHull2D convexHull = new ConvexHull2D(
					0, 1, true);
			convexHull.compute(fpImg, fpImg);
		}
		if (tg.getNodes(tg.getLastPartition()).size() > 1) {
			lpImg = TransitionGraphUtil.createPartitionImg(tg, tg.getLastPartition());
			ConvexHull2D convexHull = new ConvexHull2D(
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

	
}
