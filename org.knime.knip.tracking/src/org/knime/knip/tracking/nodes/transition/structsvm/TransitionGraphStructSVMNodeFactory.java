package org.knime.knip.tracking.nodes.transition.structsvm;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionGraphStructSVM" Node.
 * A struct SVM based classifier for transition graphs.
 *
 * @author Stephan Sellien
 */
public class TransitionGraphStructSVMNodeFactory 
        extends NodeFactory<TransitionGraphStructSVMNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TransitionGraphStructSVMNodeModel createNodeModel() {
        return new TransitionGraphStructSVMNodeModel();
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
    public NodeView<TransitionGraphStructSVMNodeModel> createNodeView(final int viewIndex,
            final TransitionGraphStructSVMNodeModel nodeModel) {
        return new TransitionGraphStructSVMNodeView(nodeModel);
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
        return new TransitionGraphStructSVMNodeDialog();
    }

}

