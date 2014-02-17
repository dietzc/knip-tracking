package org.knime.knip.tracking.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.tracking.data.features.FeatureProvider;
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
		for (String feature : FeatureProvider.getFeatureNames()) {
			cols.add(new DataColumnSpecCreator(feature, DoubleCell.TYPE)
					.createSpec());
		}
		return new DataTableSpec(cols.toArray(new DataColumnSpec[cols.size()]));
	}

	public static <T extends NativeType<T> & IntegerType<T>> DataCell[] transitionGraph2DataCells(
			TransitionGraph tg, ImgPlus<T> baseImg, ExecutionContext exec) {
		DataCell[] cells = new DataCell[createOutSpec().getNumColumns()];
		try {
			cells[0] = new ImgPlusCellFactory(exec)
					.createCell(TransitionGraphRenderer.renderTransitionGraph(
							tg, baseImg, tg));
		} catch (IOException e) {
			e.printStackTrace();
		}
		cells[1] = new StringCell(tg.toString());
		cells[2] = new StringCell(tg.toNodeString());
		cells[3] = GraphCellFactory.createCell(tg.getNet());
		double[] distVec = FeatureProvider.getFeatureVector(tg);
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
}
