package org.knime.knip.tracking.data.graph.renderer;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
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
import org.knime.knip.core.features.seg.ExtractOutlineImg;
import org.knime.knip.core.ops.img.ImgPlusNormalize;
import org.knime.knip.core.util.ShowInSameFrame;
import org.knime.knip.tracking.data.graph.Edge;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.OffsetHandling;
import org.knime.knip.tracking.util.TrackingConstants;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.core.feature.FeatureTypeFactory;

public class TransitionGraphRenderer {

	public static final int OVERVIEW_BORDER_SIZE = 50;
	public static final int BORDER = 10;
	
	/**
	 * Renders a {@link TransitionGraph} to an {@link ImgPlus}.
	 * 
	 * @param base
	 *            base graph (with surroundings)
	 * @param baseImg
	 *            original image as background
	 * @param links
	 *            {@link TransitionGraph} with edges to be drawn
	 * @return a {@link ImgPlus}
	 */
	public static <T extends NativeType<T> & IntegerType<T>> ImgPlus<T> renderTransitionGraph(
			TransitionGraph base, ImgPlus<T> baseImg, TransitionGraph links) {

		Rectangle2D rect = null;
		for (String partition : base.getPartitions()) {
			for (TrackedNode node : base.getNodes(partition)) {
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

			// System.out.print("Old rect: " + rect + " ");
			rect = new Rectangle2D.Double(minX, minY, maxX - minX + 1, maxY
					- minY + 1);
			// System.out.println(" new one: " + rect);
		}

		int imgWidth = (int) rect.getWidth();
		int imgHeight = (int) rect.getHeight();
		long[] dim = new long[2];
		dim[0] = imgWidth * base.getPartitions().size() + BORDER
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
						.iterator().next().frame();
			}
			if (!base.getNodes(base.getLastPartition()).isEmpty()) {
				timeindex = (int) base.getNodes(base.getLastPartition())
						.iterator().next().frame() - 1;
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
				int baseX = imgWidth * p + BORDER * (p);
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

		Map<TrackedNode, Point> positions = new HashMap<TrackedNode, Point>();

		long color = 10;
		long[] rectDiff = new long[2];
		int partitionIndex = 0;
		for (String partition : links.getPartitions()) {

			for (TrackedNode node : links.getNodes(partition)) {
				ImgPlusValue<?> bitmask = node.getBitmask();
				Rectangle2D r = node.getImageRectangle();
				rectDiff[0] = (long) (r.getMinX() - rect.getMinX())
						+ (imgWidth + BORDER) * partitionIndex;
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
					// ra.get().setInteger(color);
				}
				color++;
				
				//draw outline
				ExtractOutlineImg outlineOp = new ExtractOutlineImg(true);
				long[] outlineDim = new long[2];
				outlineDim[0] = ip.dimension(0);
				outlineDim[1] = ip.dimension(1);
				Img<BitType> outline = new ArrayImgFactory<BitType>().create(outlineDim, new BitType());
				cursor = outline.cursor();
				for(BitType bt : ip) {
					cursor.next();
					cursor.get().set(bt);
				}
				outlineOp.compute(outline.copy(), outline);
				cursor = outline.localizingCursor();
				while(cursor.hasNext()) {
					cursor.fwd();
					if(!cursor.get().get()) continue; 
					for(int d = 0; d < cursor.numDimensions(); d++) {
						if(d == 2) continue;
						ra.setPosition(cursor.getLongPosition(d) + rectDiff[d], d);
					}
					ra.get().setInteger(255);
				}

				// remember position for drawing edges later on
				Point position = new Point(dim.length);
				position.setPosition(
						(int) (node.getDoublePosition(0) - rect.getMinX() + (imgWidth + BORDER)
								* partitionIndex), 0);
				position.setPosition(
						(int) (node.getDoublePosition(1) - rect.getMinY()), 1);
				positions.put(node, position);
				// add node position in graph to draw nodes easily on top.
				try {
					if (!links
							.getNet()
							.isFeatureDefined(
									TrackingConstants.RENDERER_NODE_POSITION)) {
						links.getNet()
								.defineFeature(
										FeatureTypeFactory.getStringType(),
										TrackingConstants.RENDERER_NODE_POSITION);
					}
					long[] pos = new long[position.numDimensions()];
					position.localize(pos);
					links.getNet()
							.addFeature(
									node.getPersistentObject(),
									TrackingConstants.RENDERER_NODE_POSITION,
									OffsetHandling.encode(pos));
				} catch (PersistenceException e) {
					e.printStackTrace();
				} catch (InvalidFeatureException e) {
					e.printStackTrace();
				}
			}
			partitionIndex++;
		}

		// create borders
		partitionIndex = 0;
		for (int p = 0; p < base.getPartitions().size(); p++) {
			if (partitionIndex > 0
					&& partitionIndex < base.getPartitions().size()) {
				int xOffset = partitionIndex * imgWidth + BORDER
						* (partitionIndex - 1);
				for (int x = 0; x < BORDER; x++) {
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
			TrackedNode start = edge.getStartNode();
			TrackedNode end = edge.getEndNode();

			Point p1 = positions.get(start);
			Point p2 = positions.get(end);

			BresenhamLine<T> bl = new BresenhamLine<T>(img, p1, p2);
			while (bl.hasNext()) {
				T pixel = bl.next();
				pixel.setInteger(color + 20);
			}
		}

		T zero = img.firstElement().createVariable();
		zero.setZero();
		T max = img.firstElement().createVariable();
		max.setReal(max.getMaxValue());
		ImgPlusNormalize<T> imgNormalize = new ImgPlusNormalize<T>(0, img
				.firstElement().createVariable(),
				new ValuePair<T, T>(zero, max), true);

		ImgPlus<T> imgPlus = new ImgPlus<T>(img);

		imgNormalize.compute(imgPlus, imgPlus);

		// safe offsets in TransitionGraph to ease orientation
		links.setImageOffsets(new long[] { (long) rect.getMinX(),
				(long) rect.getMinY() });

		return imgPlus;
	}

	/**
	 * (Re-)Draws edges over an existing img.
	 * 
	 * @param img
	 *            img containing the {@link TransitionGraph} rendered by
	 *            {@link TransitionGraphRenderer#renderTransitionGraph(TransitionGraph, ImgPlus, TransitionGraph)}
	 *            .
	 * @param tg
	 *            the according transition graph
	 */
	public static <T extends NativeType<T> & IntegerType<T>> ImgPlus<T> drawEdges(
			ImgPlus<T> img, TransitionGraph tg) {
		try {
			if (!tg.getNet().isFeatureDefined(
					TrackingConstants.RENDERER_NODE_POSITION)) {
				System.out.println("Feature "
						+ TrackingConstants.RENDERER_NODE_POSITION
						+ " does not exist!");
				return img;
			}
			for (Edge edge : tg.getEdges()) {
				long[] startPos = OffsetHandling
						.decode(edge
								.getStartNode()
								.getStringFeature(
										TrackingConstants.RENDERER_NODE_POSITION));
				long[] endPos = OffsetHandling
						.decode(edge
								.getEndNode()
								.getStringFeature(
										TrackingConstants.RENDERER_NODE_POSITION));
				Point start = new Point(startPos);
				Point end = new Point(endPos);

				BresenhamLine<T> bl = new BresenhamLine<T>(img, start, end);
				while (bl.hasNext()) {
					T pixel = bl.next();
					pixel.setInteger(150);
				}
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return img;
	}
	
	public static void main(String[] args) {
		Img<BitType> img = new ArrayImgFactory<BitType>().create(new long[]{100,100}, new BitType());
		RandomAccess<BitType> ra = img.randomAccess();
		
		Random rnd = new Random();
		
		//draw a filled rect
		for(int y = 20; y < img.dimension(1) - 20; y++) {
			for(int x = 20; x < img.dimension(0) - 20; x++) {
				ra.setPosition(x, 0);
				ra.setPosition(y, 1);
				ra.get().set(rnd.nextDouble() < 0.1);
			}
		}
		new ShowInSameFrame().show(img, 5);
		Img<BitType> outline = img.copy();
		ExtractOutlineImg outlineOp = new ExtractOutlineImg(true);
		outlineOp.compute(outline.copy(), outline);
		new ShowInSameFrame().show(outline, 5);
	}

}
