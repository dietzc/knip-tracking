package org.knime.knip.tracking.nodes.transition.transitionScorer;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

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
		SettingsModelInteger smiFR = TransitionScorerNodeModel.createFirstModel();
		SettingsModelInteger smiNR = TransitionScorerNodeModel.createNoRowModel();
		addDialogComponent(new DialogComponentNumber(smiFR, "Number of rows in first iteration:", 1, createFlowVariableModel(smiFR)));
		addDialogComponent(new DialogComponentNumber(smiNR, "Number of rows in each iteration:", 1, createFlowVariableModel(smiNR)));
	}
}
