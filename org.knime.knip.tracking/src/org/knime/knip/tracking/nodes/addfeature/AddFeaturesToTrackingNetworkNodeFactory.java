package org.knime.knip.tracking.nodes.addfeature;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AddFeaturesToTrackingNetwork" Node.
 * Preprocessing node adding feature values to each node. Use as preprocessing step for transition graphs.
 *
 * @author Stephan Sellien
 */
public class AddFeaturesToTrackingNetworkNodeFactory 
        extends NodeFactory<AddFeaturesToTrackingNetworkNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AddFeaturesToTrackingNetworkNodeModel createNodeModel() {
        return new AddFeaturesToTrackingNetworkNodeModel();
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
    public NodeView<AddFeaturesToTrackingNetworkNodeModel> createNodeView(final int viewIndex,
            final AddFeaturesToTrackingNetworkNodeModel nodeModel) {
        return new AddFeaturesToTrackingNetworkNodeView(nodeModel);
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
        return new AddFeaturesToTrackingNetworkNodeDialog();
    }

}

