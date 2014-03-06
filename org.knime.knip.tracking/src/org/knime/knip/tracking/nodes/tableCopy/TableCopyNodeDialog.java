package org.knime.knip.tracking.nodes.tableCopy;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "TableCopy" Node.
 * Just creates a copy of the input.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TableCopyNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the TableCopy node.
     */
    protected TableCopyNodeDialog() {

    }
}

