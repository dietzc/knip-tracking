package org.knime.knip.tracking.nodes.addfeature;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "AddFeaturesToTrackingNetwork" Node.
 * Preprocessing node adding feature values to each node. Use as preprocessing step for transition graphs.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class AddFeaturesToTrackingNetworkNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the AddFeaturesToTrackingNetwork node.
     */
    protected AddFeaturesToTrackingNetworkNodeDialog() {

    }
}

