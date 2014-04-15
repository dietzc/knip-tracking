package org.knime.knip.tracking.util;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.tracking.data.featuresnew.FeatureHandler;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.data.graph.renderer.TransitionGraphRenderer;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.cell.GraphCellFactory;

public class TransitionGraphUtil {
	public static DataTableSpec createOutSpec() {
		List<DataColumnSpec> cols = new LinkedList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator("Image representation",
				ImgPlusCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator("String representation",
				StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator("Nodes/Partition", StringCell.TYPE)
				.createSpec());
		cols.add(new DataColumnSpecCreator("Transition graphs",
				GraphCellFactory.getType()).createSpec());
		for (String feature : FeatureHandler.getFeatureNames()) {
			cols.add(new DataColumnSpecCreator(feature, DoubleCell.TYPE)
					.createSpec());
		}
		return new DataTableSpec(cols.toArray(new DataColumnSpec[cols.size()]));
	}

	public static <T extends NativeType<T> & IntegerType<T>> DataCell[] transitionGraph2DataCells(
			TransitionGraph tg, ImgPlus<T> baseImg, ImgPlusCellFactory ipcf) {
		DataCell[] cells = new DataCell[createOutSpec().getNumColumns()];
		try {
			cells[0] = ipcf
					.createCell(TransitionGraphRenderer.renderTransitionGraph(
							tg, baseImg, tg));
		} catch (IOException e) {
			e.printStackTrace();
		}
		cells[1] = new StringCell(tg.toString());
		cells[2] = new StringCell(tg.toNodeString());
		cells[3] = GraphCellFactory.createCell(tg.getNet());
		double[] distVec = FeatureHandler.getFeatureVector(tg);
		for (int i = 0; i < distVec.length; i++) {
			cells[i + 4] = new DoubleCell(distVec[i]);
		}
		return cells;
	}
	
	public static TransitionGraph createTransitionGraphForNetwork(
			KPartiteGraphView<PersistentObject, Partition> net, Partition t0,
			Partition t1) {
		try {
			TransitionGraph tg = new TransitionGraph();
			tg.addPartition(t0.getId());
			tg.addPartition(t1.getId());
			// set img dimensions for feature calc
			String dimString = net.getStringFeature(net,
					TrackingConstants.NETWORK_FEATURE_DIMENSION);
			tg.getNet().defineFeature(FeatureTypeFactory.getStringType(),
					TrackingConstants.NETWORK_FEATURE_DIMENSION);
			tg.getNet().addFeature(tg.getNet(),
					TrackingConstants.NETWORK_FEATURE_DIMENSION, dimString);
			return tg;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
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
	public static Img<BitType> createPartitionImg(TransitionGraph tg,
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
