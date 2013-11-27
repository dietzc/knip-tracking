package org.knime.knip.tracking.data.features;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.ConvexHull2D;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.data.algebra.RealVector;
import org.knime.knip.tracking.data.graph.Edge;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.network.core.core.exception.PersistenceException;

@Deprecated
public class TransitionFeatures extends FeatureClass {

	@Override
	public String getName() {
		return "Transition features";
	}

	@Feature(name = "Number difference")
	public static <T extends RealType<T>> double numDiff(TransitionGraph tg) {
		return tg.getNodes(tg.getLastPartition()).size()
				- tg.getNodes(tg.getFirstPartition()).size();
	}

	@Feature(name = "First partition count")
	public static <T extends RealType<T>> double fpc(TransitionGraph tg) {
		return tg.getNodes(tg.getFirstPartition()).size();
	}

	@Feature(name = "Last partition count")
	public static <T extends RealType<T>> double lpc(TransitionGraph tg) {
		return tg.getNodes(tg.getLastPartition()).size();
	}

	@Feature(name = "Division Angle Pattern")
	public static <T extends RealType<T>> double divisionAnglePattern(
			TransitionGraph tg) {
		double angle = Double.NaN;
		for (TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
			// System.out.println(node + " " + node.getOutgoingEdges().size() +
			// " " + node.getOutgoingEdges());
			if (node.getOutgoingEdges().size() == 2) {
				double[] tmp = new double[node.numDimensions()];
				node.localize(tmp);
				RealVector p1 = new RealVector(tmp);
				node.getOutgoingEdges().get(0).getEndNode().localize(tmp);
				RealVector p2 = new RealVector(tmp);
				node.getOutgoingEdges().get(1).getEndNode().localize(tmp);
				RealVector p3 = new RealVector(tmp);

				RealVector u = p2.subtract(p1);
				RealVector v = p3.subtract(p1);

				u = u.norm2();
				v = v.norm2();

				angle = 0.0;
				for (int d = 0; d < u.numDimensions(); d++) {
					angle += u.getDoublePosition(d) * v.getDoublePosition(d);
				}

				angle = Math.acos(angle);

			}
		}
		return angle;
	}

	@Feature(name = "Merge Angle Pattern")
	public static <T extends RealType<T>> double mergeAnglePattern(
			TransitionGraph tg) {
		double angle = Double.NaN;
		for (TrackedNode node : tg.getNodes(tg.getLastPartition())) {
			if (node.getIncomingEdges().size() == 2) {
				double[] tmp = new double[node.numDimensions()];
				node.localize(tmp);
				RealVector p1 = new RealVector(tmp);
				node.getIncomingEdges().get(0).getStartNode().localize(tmp);
				RealVector p2 = new RealVector(tmp);
				node.getIncomingEdges().get(1).getStartNode().localize(tmp);
				RealVector p3 = new RealVector(tmp);

				RealVector u = p2.subtract(p1);
				RealVector v = p3.subtract(p1);

				u = u.norm2();
				v = v.norm2();

				angle = 0.0;
				for (int d = 0; d < u.numDimensions(); d++) {
					angle += u.getDoublePosition(d) * v.getDoublePosition(d);
				}

				angle = Math.acos(angle);
			}
		}
		return angle;
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
	public static double shapeCompactness(TransitionGraph tg) {
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
			ConvexHull2D convexHull = new ConvexHull2D(
					0, 1, true);
			convexHull.compute(fpImg, fpImg);
		}
		if (tg.getNodes(tg.getLastPartition()).size() > 1) {
			lpImg = createPartitionImg(tg, tg.getLastPartition());
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

	/**
	 * Creates a {@link Img} of the given partition.
	 * 
	 * @param tg
	 *            the {@link TransitionGraph}
	 * @param partition
	 *            the partition
	 * @return an {@link Img} with all nodes of the partition
	 */
	private static <T extends RealType<T>> Img<BitType> createPartitionImg(
			TransitionGraph tg, String partition) {
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

	@Feature(name = "Euclidean Distance")
	public static <T extends RealType<T>> double euclDist(TransitionGraph tg) {
		double dist = 0.0;
		if (tg.getNodes(tg.getFirstPartition()).size() > 0
				&& tg.getNodes(tg.getLastPartition()).size() > 0)
			try {
				System.out.println("blubb!" + tg.getNet().getNoOfEdges());
			} catch (PersistenceException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		for (Edge e : tg.getEdges()) {
			System.out.println("EDGE!" + e);
			dist += e.getStartNode().euclideanDistanceTo(e.getEndNode());
		}
		return dist;
	}
}
