package org.knime.knip.tracking.nodes.transition.structsvm;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "TransitionGraphStructSVM" Node.
 * A struct SVM based classifier for transition graphs.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphStructSVMNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the TransitionGraphStructSVM node.
     */
    protected TransitionGraphStructSVMNodeDialog() {

    }
}

