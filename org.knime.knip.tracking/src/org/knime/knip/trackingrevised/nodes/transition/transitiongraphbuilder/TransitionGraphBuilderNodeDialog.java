package org.knime.knip.trackingrevised.nodes.transition.transitiongraphbuilder;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 * <code>NodeDialog</code> for the "TransitionGraphBuilder" Node.
 * 
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphBuilderNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring TransitionGraphBuilder node dialog. This is just
	 * a suggestion to demonstrate possible default dialog components.
	 */
	protected TransitionGraphBuilderNodeDialog() {
		super();

		addDialogComponent(new DialogComponentNumber(
				TransitionGraphBuilderNodeModel.createDistanceSettings(),
				"Distance:", /* step */1, /* componentwidth */5));

	}
}
