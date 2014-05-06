package org.knime.knip.tracking.nodes.transition.transitionPatch;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 * <code>NodeDialog</code> for the "TransitionPatchCreator" Node.
 * Creates a transition graph containing the surrounding patch
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TransitionPatchCreatorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the TransitionPatchCreator node.
     */
    protected TransitionPatchCreatorNodeDialog() {
    	addDialogComponent(new DialogComponentNumber(TransitionPatchCreatorNodeModel.createMarginModel(), "Margin", 50));
    	addDialogComponent(new DialogComponentBoolean(TransitionPatchCreatorNodeModel.createRenderModel(), "Render patch?"));
    }
}

