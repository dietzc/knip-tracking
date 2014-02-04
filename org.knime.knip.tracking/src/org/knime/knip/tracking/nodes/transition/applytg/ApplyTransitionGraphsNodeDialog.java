package org.knime.knip.tracking.nodes.transition.applytg;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "ApplyTransitionGraphs" Node.
 * Applies transition graphs to a tracking network after a frame based optimisation
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class ApplyTransitionGraphsNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring ApplyTransitionGraphs node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ApplyTransitionGraphsNodeDialog() {
        super();
        
                    
    }
}

