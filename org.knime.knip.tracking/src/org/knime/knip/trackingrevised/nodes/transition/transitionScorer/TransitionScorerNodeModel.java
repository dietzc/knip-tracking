package org.knime.knip.trackingrevised.nodes.transition.transitionScorer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.trackingrevised.data.graph.TransitionGraph;
import org.knime.network.core.knime.cell.GraphValue;

/**
 * This is the model implementation of TransitionScorer. Judges the classified
 * transition results and get the optimal version of each transition graph
 * 
 * @author Stephan Sellien
 */
public class TransitionScorerNodeModel<T extends RealType<T>> extends NodeModel {

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

		String currentTgId = "";
		Set<TransitionGraph> graphs = new HashSet<TransitionGraph>();
		int tgIndex = NodeTools.firstCompatibleColumn(
				inData[0].getDataTableSpec(), GraphValue.class);

		DataContainer cont = new DataContainer(createOutspec());

		for (DataRow row : inData[0]) {
			if (row.getKey().getString().contains(";")) {
				System.out.println("tg: " + row.getKey());
				String tgId = row.getKey().getString().split(";")[0];
				if (tgId.equals(currentTgId)) {
					graphs.add(new TransitionGraph(((GraphValue) row
							.getCell(tgIndex)).getView()));
				} else {
					if (graphs.size() > 0) {
						// TODO unused
						TransitionGraph tg = merge(graphs);
						cont.addRowToTable(new DefaultRow(tgId, new DataCell[0]));
					}
					currentTgId = tgId;
					// start a new set
					graphs.clear();
				}
				if (graphs.size() > 0) {
					// TODO unused
					TransitionGraph tg = merge(graphs);
					cont.addRowToTable(new DefaultRow(tgId, new DataCell[0]));
				}

			} else {
				cont.addRowToTable(new DefaultRow(row.getKey(), new DataCell[0]));
			}
		}

		cont.close();

		return new BufferedDataTable[] { exec.createBufferedDataTable(
				cont.getTable(), exec) };

		// ColumnRearranger rearranger = createColumnRearranger(inData[0]
		// .getDataTableSpec());
		//
		// return new BufferedDataTable[] { exec.createColumnRearrangeTable(
		// inData[0], rearranger, exec) };
	}

	private DataTableSpec createOutspec() {
		return new DataTableSpec();
	}

	private TransitionGraph merge(Set<TransitionGraph> graphs) {
		if (graphs.size() == 0)
			return null;
		else
			return graphs.iterator().next();
	}

	private ColumnRearranger createColumnRearranger(DataTableSpec dataTableSpec) {
		ColumnRearranger rearranger = new ColumnRearranger(dataTableSpec);
		rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(
				"Transition score", DoubleCell.TYPE).createSpec()) {

			@Override
			public DataCell getCell(DataRow row) {
				return new DoubleCell(new Random().nextDouble());
			}
		});
		return rearranger;
	}

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
		return new DataTableSpec[] { createColumnRearranger(inSpecs[0])
				.createSpec() };
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
