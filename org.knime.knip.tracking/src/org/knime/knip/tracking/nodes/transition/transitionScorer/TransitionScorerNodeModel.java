package org.knime.knip.tracking.nodes.transition.transitionScorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
		if (getAvailableFlowVariables().get("AL_ITERATION").getIntValue() == 0) {
			// first iteration
			List<Integer> indices = new LinkedList<Integer>();
			Random rand = new Random();
			int firstCount = Math.min(m_firstCount.getIntValue(),
					inData[0].getRowCount());
			for (int i = 0; i < firstCount; i++) {
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
			return new BufferedDataTable[] { exec.createBufferedDataTable(
					cont.getTable(), exec) };
		}

		int kernelDensityIdx = inData[0].getDataTableSpec().findColumnIndex(
				"kernel density");
		if (kernelDensityIdx == -1)
			kernelDensityIdx = inData[0].getDataTableSpec().findColumnIndex(
					"Density");
		if (kernelDensityIdx != -1) {
			final int index = kernelDensityIdx;
			BufferedDataTableSorter sorter = new BufferedDataTableSorter(
					inData[0], new Comparator<DataRow>() {
						@Override
						public int compare(DataRow row1, DataRow row2) {
							int cmp = Double.compare(((DoubleValue) row1
									.getCell(index)).getDoubleValue(),
									((DoubleValue) row2.getCell(index))
											.getDoubleValue());
							return cmp;
						}
					});
			BufferedDataTable sortedTable = sorter.sort(exec);

			int ctr = 0;
			for (DataRow row : sortedTable) {
				System.out.println("should be: " + row.getKey() + " with "
						+ row.getCell(kernelDensityIdx));
				ctr++;
				if (ctr == 5)
					break;
			}

			RowIterator it = sortedTable.iterator();
			return new BufferedDataTable[] { output(it,
					sortedTable.getDataTableSpec(), exec) };
		}
		
		final int plusIdx = inData[0].getDataTableSpec().findColumnIndex("+");
		final int minusIdx = inData[0].getDataTableSpec().findColumnIndex("-");
		
		if(plusIdx != -1 && minusIdx != -1) {
			ArrayList<Double> qs = new ArrayList<Double>(inData[0].getRowCount());
			for(DataRow row : inData[0]) {
				double plus = ((DoubleValue)row.getCell(plusIdx)).getDoubleValue();
				double minus = ((DoubleValue)row.getCell(minusIdx)).getDoubleValue();
				double q = Math.exp(-Math.abs(plus-minus));
				qs.add(q);
			}
			Collections.sort(qs);
			int startIndex = (int) Math.round(qs.size() * 0.1);
			int endIndex = qs.size() - startIndex;
			
			double mean = 0.0;
			for(int index = startIndex; index < endIndex; index++) {
				mean += qs.get(index);
			}
			mean /= (endIndex-startIndex);
			
			qs = null;
			
			System.out.println("meanQ=" + mean);
			
			pushFlowVariableDouble("meanQ", mean);
			
			BufferedDataTableSorter sorter = new BufferedDataTableSorter(
					inData[0], new Comparator<DataRow>() {
						@Override
						public int compare(DataRow row1, DataRow row2) {
							int cmp = Double.compare(Math.abs(((DoubleValue) row1
									.getCell(plusIdx)).getDoubleValue() -
									((DoubleValue) row1.getCell(minusIdx))
											.getDoubleValue()), Math.abs(((DoubleValue) row2
													.getCell(plusIdx)).getDoubleValue() -
													((DoubleValue) row2.getCell(minusIdx))
															.getDoubleValue()));
							return cmp;
						}
					});
			BufferedDataTable sortedTable = sorter.sort(exec);

			int ctr = 0;
			for (DataRow row : sortedTable) {
				System.out.println("should be: " + row.getKey() + " with "
						+ Math.abs(((DoubleValue) row
								.getCell(plusIdx)).getDoubleValue() -
								((DoubleValue) row.getCell(minusIdx))
										.getDoubleValue()));
				ctr++;
				if (ctr == m_rowCount.getIntValue())
					break;
			}

			RowIterator it = sortedTable.iterator();
			return new BufferedDataTable[] { output(it,
					sortedTable.getDataTableSpec(), exec) };
		}

		//normal case, x random rows
		RowIterator it = inData[0].iterator();
		return new BufferedDataTable[] { randomOutput(it,
				inData[0], exec) };

	}

	private BufferedDataTable output(RowIterator it, DataTableSpec inSpec,
			ExecutionContext exec) throws CanceledExecutionException {
		DataTableSpec outSpec = createOutspec(inSpec);
		DataContainer cont = new DataContainer(outSpec);
		// get first x values
		for (int count = 0; count < m_rowCount.getIntValue() && it.hasNext(); count++) {
			cont.addRowToTable(filterColumns(it.next(), inSpec));
		}
		cont.close();
		return exec.createBufferedDataTable(cont.getTable(), exec);
	}
	
	private BufferedDataTable randomOutput(RowIterator it, BufferedDataTable table,
			ExecutionContext exec) throws CanceledExecutionException {
		DataTableSpec outSpec = createOutspec(table.getDataTableSpec());
		DataContainer cont = new DataContainer(outSpec);
		//create indices
		List<Integer> indices = new LinkedList<Integer>();
		Random rand = new Random();
		int firstCount = Math.min(m_firstCount.getIntValue(),
				table.getRowCount());
		for (int i = 0; i < firstCount; i++) {
			boolean found = false;
			while (!found) {
				int nr = rand.nextInt(table.getRowCount());
				if (!indices.contains(nr)) {
					indices.add(nr);
					found = true;
				}
			}
		}
		// get first x values
		for (int count = 0; count < m_rowCount.getIntValue() && it.hasNext(); count++) {
			cont.addRowToTable(filterColumns(it.next(), table.getDataTableSpec()));
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
