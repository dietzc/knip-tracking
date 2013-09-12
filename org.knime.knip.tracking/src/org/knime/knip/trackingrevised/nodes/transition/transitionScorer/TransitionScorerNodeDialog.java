package org.knime.knip.trackingrevised.nodes.transition.transitionScorer;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "TransitionScorer" Node. Judges the
 * classified transition results and get the optimal version of each transition
 * graph
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TransitionScorerNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the TransitionScorer node.
	 */
	protected TransitionScorerNodeDialog() {

	}
}
