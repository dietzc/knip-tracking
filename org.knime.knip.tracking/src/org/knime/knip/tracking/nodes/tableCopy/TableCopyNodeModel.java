package org.knime.knip.tracking.nodes.tableCopy;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
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
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.network.core.knime.cell.GraphCellFactory;
import org.knime.network.core.knime.cell.GraphValue;

/**
 * This is the model implementation of TableCopy.
 * Just creates a copy of the input.
 *
 * @author Stephan Sellien
 */
public class TableCopyNodeModel extends NodeModel {
    
    /**
     * Constructor for the node model.
     */
    protected TableCopyNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataContainer cont = exec.createDataContainer(inData[0].getDataTableSpec());
    	
    	for(DataRow row : inData[0]) {
    		DataCell[] cells = new DataCell[row.getNumCells()];
    		for(int c = 0; c < cells.length; c++) {
    			cells[c] = row.getCell(c);
    			if(cells[c] instanceof GraphValue) {
    				TransitionGraph tg = new TransitionGraph(((GraphValue)cells[c]).getView());
    				cells[c] = GraphCellFactory.createCell(tg.getNet());
    			}
    		}
    		cont.addRowToTable(new DefaultRow(row.getKey(), cells));
    	}
    	
    	cont.close();
    	
        return new BufferedDataTable[]{exec.createBufferedDataTable(cont.getTable(), exec)};
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

        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

}

