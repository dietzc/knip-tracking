package org.knime.knip.tracking.nodes.input.botReader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TiffFolder2TransitionGraph" Node.
 * Reads a solved tracking problem as TransitionGraphs. * nThe problem must have the following structure: * n- raw/     image data * n- seg/     segmentation * n- training/     tracking info
 *
 * @author Stephan Sellien
 */
public class BOTReaderNodeFactory 
        extends NodeFactory<BOTReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BOTReaderNodeModel createNodeModel() {
        return new BOTReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<BOTReaderNodeModel> createNodeView(final int viewIndex,
            final BOTReaderNodeModel nodeModel) {
        return new BOTReaderNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new BOTReaderNodeDialog();
    }

}

