package org.knime.knip.tracking.nodes.botReader;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "TiffFolder2TransitionGraph" Node.
 * Reads a solved tracking problem as TransitionGraphs. * nThe problem must have the following structure: * n- raw/     image data * n- seg/     segmentation * n- training/     tracking info
 *
 * @author Stephan Sellien
 */
public class BOTReaderNodeView extends NodeView<BOTReaderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link BOTReaderNodeModel})
     */
    protected BOTReaderNodeView(final BOTReaderNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        BOTReaderNodeModel nodeModel = 
            (BOTReaderNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

