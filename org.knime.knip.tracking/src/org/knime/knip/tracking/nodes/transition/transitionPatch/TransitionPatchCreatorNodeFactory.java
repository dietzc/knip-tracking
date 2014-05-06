package org.knime.knip.tracking.nodes.transition.transitionPatch;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionPatchCreator" Node.
 * Creates a transition graph containing the surrounding patch
 *
 * @author Stephan Sellien
 */
public class TransitionPatchCreatorNodeFactory 
        extends NodeFactory<TransitionPatchCreatorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TransitionPatchCreatorNodeModel createNodeModel() {
        return new TransitionPatchCreatorNodeModel();
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
    public NodeView<TransitionPatchCreatorNodeModel> createNodeView(final int viewIndex,
            final TransitionPatchCreatorNodeModel nodeModel) {
        return new TransitionPatchCreatorNodeView(nodeModel);
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
        return new TransitionPatchCreatorNodeDialog();
    }

}

