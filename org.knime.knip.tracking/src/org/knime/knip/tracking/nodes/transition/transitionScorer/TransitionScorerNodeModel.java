package org.knime.knip.tracking.nodes.transition.transitionScorer;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of TransitionScorer. Judges the classified
 * transition results and get the optimal version of each transition graph
 * 
 * @author Stephan Sellien
 */
public class TransitionScorerNodeModel extends NodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected TransitionScorerNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		return inData;

		// ColumnRearranger rearranger = createColumnRearranger(inData[0]
		// .getDataTableSpec());
		//
		// return new BufferedDataTable[] { exec.createColumnRearrangeTable(
		// inData[0], rearranger, exec) };
	}

//	private TransitionGraph merge(Set<TransitionGraph> graphs) {
//		if (graphs.size() == 0)
//			return null;
//		else
//			return graphs.iterator().next();
//	}

//	private ColumnRearranger createColumnRearranger(DataTableSpec dataTableSpec) {
//		ColumnRearranger rearranger = new ColumnRearranger(dataTableSpec);
//		rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(
//				"Transition score", DoubleCell.TYPE).createSpec()) {
//
//			@Override
//			public DataCell getCell(DataRow row) {
//				return new DoubleCell(new Random().nextDouble());
//			}
//		});
//		return rearranger;
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
//		return new DataTableSpec[] { createColumnRearranger(inSpecs[0])
//				.createSpec() };
		return inSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

}
