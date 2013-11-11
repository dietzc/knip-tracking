package org.knime.knip.tracking.nodes.transition.transitionEnumerator;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "TransitionEnumerator" Node. Enumerates all
 * possible transitions of a given transition graph.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TransitionEnumeratorNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the TransitionEnumerator node.
	 */
	protected TransitionEnumeratorNodeDialog() {

	}
}
