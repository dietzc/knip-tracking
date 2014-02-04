package org.knime.knip.tracking.nodes.labmerger;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "LabelingMerger" Node.
 * Merges a table with labelings. * nSame labels in different labelings are seperated!
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class LabelingMergerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LabelingMerger node.
     */
    protected LabelingMergerNodeDialog() {

    }
}

