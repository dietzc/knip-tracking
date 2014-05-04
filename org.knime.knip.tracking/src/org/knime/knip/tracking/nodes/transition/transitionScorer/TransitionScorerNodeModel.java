package org.knime.knip.tracking.nodes.transition.transitionScorer;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.knip.tracking.util.TransitionGraphUtil;

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

	private SettingsModelInteger m_firstCount = createFirstModel();
	private SettingsModelInteger m_rowCount = createNoRowModel();

	static SettingsModelInteger createFirstModel() {
		return new SettingsModelInteger("smi_FirstModel", 5);
	}

	static SettingsModelInteger createNoRowModel() {
		return new SettingsModelInteger("smi_NoRowModel", 5);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		System.out.println(getAvailableFlowVariables().get("AL_ITERATION"));
		if (getAvailableFlowVariables().get("AL_ITERATION").getIntValue() == 0) {
			// first iteration
			List<Integer> indices = new LinkedList<Integer>();
			Random rand = new Random(1337);
			for (int i = 0; i < m_firstCount.getIntValue(); i++) {
				boolean found = false;
				while (!found) {
					int nr = rand.nextInt(inData[0].getRowCount());
					if (!indices.contains(nr)) {
						indices.add(nr);
						found = true;
					}
				}
			}
			DataContainer cont = new DataContainer(
					createOutspec(inData[0].getDataTableSpec()));
			int index = 0;
			for (DataRow row : inData[0]) {
				if (indices.contains(index)) {
					cont.addRowToTable(filterColumns(row,
							inData[0].getDataTableSpec()));
				}
				index++;
			}

			cont.close();
			return new BufferedDataTable[] {exec.createBufferedDataTable(cont.getTable(), exec)};
		}

		final int kernelDensityIdx = inData[0].getDataTableSpec()
				.findColumnIndex("kernel density");
		if (kernelDensityIdx != -1) {
			BufferedDataTableSorter sorter = new BufferedDataTableSorter(
					inData[0], new Comparator<DataRow>() {
						@Override
						public int compare(DataRow row1, DataRow row2) {
							int cmp = Double.compare(((DoubleValue) row1
									.getCell(kernelDensityIdx))
									.getDoubleValue(), ((DoubleValue) row2
									.getCell(kernelDensityIdx))
									.getDoubleValue());
							return -cmp;
						}
					});
			sorter.sort(exec);

			RowIterator it = inData[0].iterator();
			return new BufferedDataTable[] { output(it,
					inData[0].getDataTableSpec(), exec) };
		} else {
			RowIterator it = inData[0].iterator();
			return new BufferedDataTable[] { output(it,
					inData[0].getDataTableSpec(), exec) };
		}
	}

	private BufferedDataTable output(RowIterator it, DataTableSpec inSpec,
			ExecutionContext exec) throws CanceledExecutionException {
		DataTableSpec outSpec = createOutspec(inSpec);
		DataContainer cont = new DataContainer(outSpec);
		// get first x values
		for (int count = 0; count < m_rowCount.getIntValue(); count++) {
			cont.addRowToTable(filterColumns(it.next(), inSpec));
		}
		cont.close();
		return exec.createBufferedDataTable(cont.getTable(), exec);
	}

	// private TransitionGraph merge(Set<TransitionGraph> graphs) {
	// if (graphs.size() == 0)
	// return null;
	// else
	// return graphs.iterator().next();
	// }

	// private ColumnRearranger createColumnRearranger(DataTableSpec
	// dataTableSpec) {
	// ColumnRearranger rearranger = new ColumnRearranger(dataTableSpec);
	// rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(
	// "Transition score", DoubleCell.TYPE).createSpec()) {
	//
	// @Override
	// public DataCell getCell(DataRow row) {
	// return new DoubleCell(new Random().nextDouble());
	// }
	// });
	// return rearranger;
	// }

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
		// return new DataTableSpec[] { createColumnRearranger(inSpecs[0])
		// .createSpec() };
		// return inSpecs;
		return new DataTableSpec[] { createOutspec(inSpecs[0]) };
	}

	private DataTableSpec createOutspec(DataTableSpec inSpec) {
		DataTableSpec outSpec = TransitionGraphUtil.createOutSpec();
		ColumnRearranger ra = new ColumnRearranger(inSpec);
		for (int col = outSpec.getNumColumns(); col < inSpec.getNumColumns(); col++)
			ra.remove(outSpec.getNumColumns()); // not col because each call
												// reduces size by one
		return ra.createSpec();
	}

	private DataRow filterColumns(DataRow row, DataTableSpec inSpec) {
		// remove appended columns
		DataTableSpec spec = createOutspec(inSpec);
		DataCell[] cells = new DataCell[spec.getNumColumns()];
		for (int c = 0; c < cells.length; c++) {
			cells[c] = row.getCell(c);
		}
		return new DefaultRow(row.getKey(), cells);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_firstCount.saveSettingsTo(settings);
		m_rowCount.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_firstCount.loadSettingsFrom(settings);
		m_rowCount.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_firstCount.validateSettings(settings);
		m_rowCount.validateSettings(settings);
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
