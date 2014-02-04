package org.knime.knip.tracking.nodes.labmerger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.Axes;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;

/**
 * This is the model implementation of LabelingMerger.
 * Merges a table with labelings. * nSame labels in different labelings are seperated!
 *
 * @author Stephan Sellien
 * @param <T>
 */
public class LabelingMergerNodeModel<L extends Comparable<L>, T extends IntegerType<T> &NativeType<T>> extends NodeModel implements BufferedDataTableHolder {
    
    /**
     * Constructor for the node model.
     */
    protected LabelingMergerNodeModel() {
    
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	int labIdx = NodeUtils.firstCompatibleColumn(inData[0].getDataTableSpec(), LabelingValue.class, new Integer[0]);
    	
    	DataContainer cont = exec.createDataContainer(inData[0].getDataTableSpec());
    	
    	long[] resDim = null;
    	
    	
    	//get dimensions
    	int counter = 0;
    	for(DataRow row : inData[0]) {
    		@SuppressWarnings("unchecked")
			Labeling<L> lab = ((LabelingValue<L>) row.getCell(labIdx)).getLabeling();
    		if(resDim == null) {
    			resDim = new long[lab.numDimensions()+1];
    		}
    		long[] dim = new long[lab.numDimensions()];
    		lab.dimensions(dim);
    		
    		for(int d = 0; d < dim.length; d++) {
    			resDim[d] = Math.max(dim[d], resDim[d]);
    		}
    		counter++;
    	}
    	resDim[resDim.length - 1] = counter;
    	
    	ImgFactory<T> factory = new PlanarImgFactory<T>();
    	@SuppressWarnings("unchecked")
		T resType = (T)new UnsignedIntType();
    	    	
    	Img<T> img = factory.create(resDim, resType);

    	Labeling<String> resLab = new NativeImgLabelingWithoutBackground<String>(img);
    	
    	Cursor<LabelingType<String>> resCursor = resLab.cursor();
    	for(DataRow row : inData[0]) {
    		@SuppressWarnings("unchecked")
			Labeling<L> lab = ((LabelingValue<L>) row.getCell(labIdx)).getLabeling();
    		Map<L, String> labelMap = new HashMap<L, String>();
    		for(L label : lab.getLabels()) {
    			String frame = row.getKey().getString().replace(".tiff", "");
    			labelMap.put(label, frame + "_" + label.toString());
    		}
    		    		
    		Cursor<LabelingType<L>> labCursor = lab.cursor();
    		
    		while(labCursor.hasNext()) {
    			exec.checkCanceled();
    			labCursor.next();
    			resCursor.next();
    			
    			if(!labCursor.get().getLabeling().isEmpty()) {
    				String newLabel = labelMap.get(labCursor.get().getLabeling().get(0));
    				resCursor.get().setLabel(newLabel);
    			} else {
    				resCursor.get().intern(Collections.<String> emptyList());
    			}
    		}
    	}
    	LabelingMetadata metadata = new DefaultLabelingMetadata(resLab.numDimensions(), new DefaultLabelingColorTable());
    	metadata.setAxis(new DefaultLinearAxis(Axes.X), 0);
    	metadata.setAxis(new DefaultLinearAxis(Axes.Y), 1);
    	metadata.setAxis(new DefaultLinearAxis(Axes.TIME), 2);
    	
    	cont.addRowToTable(new DefaultRow("12345", new LabelingCellFactory(exec).createCell(resLab, metadata)));
    	
    	System.out.println(new LabelingCellFactory(exec).createCell(resLab, metadata));
    	
    	cont.close();
    	
    	internalBDtables = new BufferedDataTable[]{exec.createBufferedDataTable(cont.getTable(), exec)};
 
        return internalBDtables;
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
    	if(NodeUtils.firstCompatibleColumn(inSpecs[0], LabelingValue.class, new Integer[0]) == -1) {
    		throw new InvalidSettingsException("No labeling column found.");
    	}
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

    private class NativeImgLabelingWithoutBackground<LL extends Comparable<LL>> extends NativeImgLabeling<LL, T> {

        /**
         * @param img
         */
        public NativeImgLabelingWithoutBackground(final Img<T> img) {
            super(img);
            super.mapping = new LabelingMapping<LL>(img.firstElement().createVariable()) {
                {
                    //remove background from lists
                    listsByIndex.clear();
                    internedLists.clear();
                }
            };
        }

    }
    
    private BufferedDataTable[] internalBDtables = new BufferedDataTable[0];

	@Override
	public BufferedDataTable[] getInternalTables() {
		return internalBDtables;
	}

	@Override
	public void setInternalTables(BufferedDataTable[] tables) {
		internalBDtables = tables;
	}
}



