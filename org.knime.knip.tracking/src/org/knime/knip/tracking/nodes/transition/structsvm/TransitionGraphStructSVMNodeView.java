package org.knime.knip.tracking.nodes.transition.structsvm;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "TransitionGraphStructSVM" Node.
 * A struct SVM based classifier for transition graphs.
 *
 * @author Stephan Sellien
 */
public class TransitionGraphStructSVMNodeView extends NodeView<TransitionGraphStructSVMNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link TransitionGraphStructSVMNodeModel})
     */
    protected TransitionGraphStructSVMNodeView(final TransitionGraphStructSVMNodeModel nodeModel) {
        super(nodeModel);
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO: generated method stub
    }

}

