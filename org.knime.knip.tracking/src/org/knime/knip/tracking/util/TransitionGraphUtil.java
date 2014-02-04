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
			TransitionGraph tg, ImgPlus<T> baseImg,
			ExecutionContext exec) {
		DataCell[] cells = new DataCell[createOutSpec().getNumColumns()];
		try {
			cells[0] = new ImgPlusCellFactory(exec)
					.createCell(TransitionGraphRenderer.renderTransitionGraph(tg,
							baseImg, tg));
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
}
