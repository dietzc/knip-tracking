package org.knime.knip.trackingrevised.nodes.transition.hungarian;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "HungarianOptimizer" Node. Optimizes given
 * transitions in pairwise frames with the hungarian method to clear equal
 * situations.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class HungarianOptimizerNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the HungarianOptimizer node.
	 */
	protected HungarianOptimizerNodeDialog() {

	}
}
