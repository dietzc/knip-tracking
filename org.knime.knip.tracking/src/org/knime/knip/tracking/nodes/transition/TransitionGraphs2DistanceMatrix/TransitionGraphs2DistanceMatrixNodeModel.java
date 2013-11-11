package org.knime.knip.tracking.nodes.transition.TransitionGraphs2DistanceMatrix;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.distmatrix.type.DistanceVectorDataCellFactory;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.network.core.knime.cell.GraphCell;
import org.knime.network.core.knime.cell.GraphCellFactory;

/**
 * This is the model implementation of TransitionGraphs2DistanceMatrix. Create a
 * distance matrix out of a bunch of transition graphs.
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphs2DistanceMatrixNodeModel extends NodeModel {

	protected TransitionGraphs2DistanceMatrixNodeModel() {
		super(1, 1);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		System.out.println(inSpecs[0]);

		/*
		 * if(inSpecs[0].containsCompatibleType(GraphValue.class)) throw new
		 * InvalidSettingsException("Input must contain a graph cell column.");
		 */
		// TODO: correct it!
		return inSpecs;
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {
		int graphColumnIndex = NodeTools.firstCompatibleColumn(inData[0]
				.getDataTableSpec(), GraphCellFactory.getType()
				.getPreferredValueClass());

		DataContainer cont = exec.createDataContainer(createOutputSpec());

		int i = 0, j = 0;
		for (DataRow row : inData[0]) {
			GraphCell cell = (GraphCell) row.getCell(graphColumnIndex);
			TransitionGraph graph = new TransitionGraph(cell.getView());

			double distances[] = new double[i + 1];

			for (DataRow innerRow : inData[0]) {
				if (i < j)
					break;
				GraphCell innerCell = (GraphCell) innerRow
						.getCell(graphColumnIndex);
				TransitionGraph innerGraph = new TransitionGraph(
						innerCell.getView());
				distances[j] = graph.distanceTo(innerGraph);
				j++;
			}

			cont.addRowToTable(new DefaultRow(row.getKey(),
					DistanceVectorDataCellFactory.createCell(distances,
							DistanceVectorDataCellFactory.RANDOM.nextInt())));
			i++;
			j = 0;
		}
		cont.close();

		return new BufferedDataTable[] { exec.createBufferedDataTable(
				cont.getTable(), exec) };
	}

	private DataTableSpec createOutputSpec() {
		return new DataTableSpec(new DataColumnSpecCreator("distance",
				DistanceVectorDataCellFactory.TYPE).createSpec());
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	@Override
	protected void reset() {
	}

}
