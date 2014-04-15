package org.knime.knip.tracking.nodes.addfeature;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "AddFeaturesToTrackingNetwork" Node.
 * Preprocessing node adding feature values to each node. Use as preprocessing step for transition graphs.
 *
 * @author Stephan Sellien
 */
public class AddFeaturesToTrackingNetworkNodeView extends NodeView<AddFeaturesToTrackingNetworkNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link AddFeaturesToTrackingNetworkNodeModel})
     */
    protected AddFeaturesToTrackingNetworkNodeView(final AddFeaturesToTrackingNetworkNodeModel nodeModel) {
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

