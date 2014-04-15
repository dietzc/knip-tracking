package org.knime.knip.tracking.nodes.input.botReader;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TiffFolder2TransitionGraph" Node.
 * Reads a solved tracking problem as TransitionGraphs. * nThe problem must have the following structure: * n- raw/     image data * n- seg/     segmentation * n- training/     tracking info
 *
 * @author Stephan Sellien
 */
public class BOTReaderNodeFactory<T extends NativeType<T> & IntegerType<T>>
        extends NodeFactory<BOTReaderNodeModel<T>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BOTReaderNodeModel<T> createNodeModel() {
        return new BOTReaderNodeModel<T>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<BOTReaderNodeModel<T>> createNodeView(final int viewIndex,
            final BOTReaderNodeModel<T> nodeModel) {
        return null;
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

