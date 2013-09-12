package org.knime.knip.trackingrevised.util;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.ops.img.ImgNormalize;
import org.knime.knip.trackingrevised.data.graph.Edge;
import org.knime.knip.trackingrevised.data.graph.Node;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;

public class TransitionGraphRenderer {

	private static final int OVERVIEW_BORDER_SIZE = 50;

	public static <T extends NativeType<T> & IntegerType<T>> ImgPlus<T> renderTransitionGraph(
			TransitionGraph base, ImgPlus<T> baseImg, TransitionGraph links) {
		final int border = 10;

		Rectangle2D rect = null;
		for (String partition : base.getPartitions()) {
			for (Node node : base.getNodes(partition)) {
				if (rect == null) {
					rect = node.getImageRectangle();
				} else {
					rect.add(node.getImageRectangle());
				}
			}
		}

		if (rect == null)
			return new ImgPlus<T>(new ArrayImgFactory<T>().create(new long[] {
					0, 0 }, baseImg.firstElement().createVariable()));

		{ // increase rect to increase overview
			double minX = rect.getMinX();
			double minY = rect.getMinY();
			double maxX = rect.getMaxX();
			double maxY = rect.getMaxY();

			minX = Math.max(0, minX - OVERVIEW_BORDER_SIZE);
			minY = Math.max(0, minY - OVERVIEW_BORDER_SIZE);
			maxX = Math.min(baseImg.max(0), maxX + OVERVIEW_BORDER_SIZE);
			maxY = Math.min(baseImg.max(1), maxY + OVERVIEW_BORDER_SIZE);

			System.out.print("Old rect: " + rect + " ");
			rect = new Rectangle2D.Double(minX, minY, maxX - minX + 1, maxY
					- minY + 1);
			System.out.println(" new one: " + rect);
		}

		int imgWidth = (int) rect.getWidth();
		int imgHeight = (int) rect.getHeight();
		long[] dim = new long[2];
		dim[0] = imgWidth * base.getPartitions().size() + border
				* (base.getPartitions().size() - 1);
		dim[1] = imgHeight;
		Img<T> img = new ArrayImgFactory<T>().create(dim, baseImg
				.firstElement().createVariable());
		RandomAccess<T> ra = img.randomAccess();

		// copy original image for better overview to each partitions background
		if (baseImg != null) {
			int timeindex = -1;
			if (!base.getNodes(base.getFirstPartition()).isEmpty()) {
				timeindex = (int) base.getNodes(base.getFirstPartition())
						.iterator().next().getTime();
			}
			if (!base.getNodes(base.getLastPartition()).isEmpty()) {
				timeindex = (int) base.getNodes(base.getLastPartition())
						.iterator().next().getTime() - 1;
			}
			if (timeindex == -1) {
				throw new IllegalArgumentException(
						"Base transition graph contains no single node.");
			}
			long[] min = new long[3];
			min[0] = (long) rect.getMinX();
			min[1] = (long) rect.getMinY();
			min[2] = timeindex;
			long[] max = new long[3];
			max[0] = min[0] + imgWidth - 1;
			max[1] = min[1] + imgHeight - 1;
			max[2] = min[2];
			long[] position = new long[3];
			IntervalView<T> p1 = Views.interval(baseImg, min, max);
			min[2] = ++max[2];
			IntervalView<T> p2 = Views.interval(baseImg, min, max);
			for (int p = 0; p < base.getPartitions().size(); p++) {
				int baseX = imgWidth * p + border * (p);
				IntervalView<T> view = (p == 0) ? p1 : p2;
				Cursor<T> cursor = view.localizingCursor();
				while (cursor.hasNext()) {
					cursor.next();
					cursor.localize(position);
					position[0] += baseX - min[0];
					position[1] -= min[1];
					// System.out.println(Arrays.toString(position) + " img:  "
					// + rect + " " + Arrays.toString(min) +
					// Arrays.toString(max) + baseX);
					ra.setPosition(position);
					T pixel = ra.get();
					T value = cursor.get();
					pixel.set(value);
				}
				// System.out.println("-----------------");
			}
		}

		Map<Node, Point> positions = new HashMap<Node, Point>();

		long color = 10;
		long[] rectDiff = new long[2];
		int partitionIndex = 0;
		for (String partition : links.getPartitions()) {

			for (Node node : links.getNodes(partition)) {
				ImgPlusValue<?> bitmask = node.getBitmask();
				Rectangle2D r = node.getImageRectangle();
				rectDiff[0] = (long) (r.getMinX() - rect.getMinX())
						+ (imgWidth + border) * partitionIndex;
				rectDiff[1] = (long) (r.getMinY() - rect.getMinY());
				@SuppressWarnings("unchecked")
				ImgPlus<BitType> ip = (ImgPlus<BitType>) bitmask.getImgPlus();

				Cursor<BitType> cursor = ip.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					for (int d = 0; d < cursor.numDimensions(); d++) {
						if (d == 2)
							continue;
						ra.setPosition(cursor.getLongPosition(d) + rectDiff[d],
								d);
						// System.out.println((cursor.getLongPosition(d) +
						// rectDiff[d] + " vs " + dim[d] + " [" + d + "]"));
					}
					// ra.get().setInteger(cursor.get().getInteger() * color);
					ra.get().setInteger(color);
				}
				color++;

				// remember position for drawing edges later on
				Point position = new Point(dim.length);
				RealPoint p = node.getPosition();
				position.setPosition(
						(int) (p.getDoublePosition(0) - rect.getMinX() + (imgWidth + border)
								* partitionIndex), 0);
				position.setPosition(
						(int) (p.getDoublePosition(1) - rect.getMinY()), 1);
				positions.put(node, position);
			}
			partitionIndex++;
		}

		// create borders
		partitionIndex = 0;
		for (int p = 0; p < base.getPartitions().size(); p++) {
			if (partitionIndex > 0
					&& partitionIndex < base.getPartitions().size()) {
				int xOffset = partitionIndex * imgWidth + border
						* (partitionIndex - 1);
				for (int x = 0; x < border; x++) {
					ra.setPosition(xOffset + x, 0);
					for (int y = 0; y < imgHeight; y++) {
						ra.setPosition(y, 1);
						ra.get().setInteger(color + 10);
					}
				}
			}
			partitionIndex++;
		}
		// ---borders

		for (Edge edge : links.getEdges()) {
			Node start = edge.getStartNode();
			Node end = edge.getEndNode();

			Point p1 = positions.get(start);
			Point p2 = positions.get(end);

			BresenhamLine<T> bl = new BresenhamLine<T>(img, p1, p2);
			while (bl.hasNext()) {
				T pixel = bl.next();
				// pixel.setInteger(color + 25);
				// edges always white for now
				pixel.setOne();
			}

			// BresenhamLine<UnsignedIntType> bl = new
			// BresenhamLine<UnsignedIntType>(img, p1,p2);
		}

		T zero = img.firstElement().createVariable();
		zero.setZero();
		T max = img.firstElement().createVariable();
		max.setReal(max.getMaxValue());
		ImgNormalize<T> imgNormalize = new ImgNormalize<T>(0, img
				.firstElement().createVariable(),
				new ValuePair<T, T>(zero, max), true);

		imgNormalize.compute(img, img);

		return new ImgPlus<T>(img);
	}

}