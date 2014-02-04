package org.knime.knip.tracking.nodes.transition.applytg;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ApplyTransitionGraphs" Node.
 * Applies transition graphs to a tracking network after a frame based optimisation
 *
 * @author Stephan Sellien
 */
public class ApplyTransitionGraphsNodeFactory 
        extends NodeFactory<ApplyTransitionGraphsNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplyTransitionGraphsNodeModel createNodeModel() {
        return new ApplyTransitionGraphsNodeModel();
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
    public NodeView<ApplyTransitionGraphsNodeModel> createNodeView(final int viewIndex,
            final ApplyTransitionGraphsNodeModel nodeModel) {
        return new ApplyTransitionGraphsNodeView(nodeModel);
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
        return new ApplyTransitionGraphsNodeDialog();
    }

}

