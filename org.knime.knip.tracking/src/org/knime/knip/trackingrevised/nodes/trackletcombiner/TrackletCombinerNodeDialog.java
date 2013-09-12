package org.knime.knip.trackingrevised.nodes.trackletcombiner;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;

/**
 * <code>NodeDialog</code> for the "TrackletCombiner" Node. Combines tracklets
 * to complete tracks.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TrackletCombinerNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring TrackletCombiner node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
	protected TrackletCombinerNodeDialog() {
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createLamda1Settings(), "Lamda 1"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createLamda2Settings(), "Lamda 2"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createLamda3Settings(), "Lamda 3"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createDeltaSSettings(), "Delta S"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createDeltaTSettings(), "Delta T"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createAlphaSettings(), "Alpha"));
		addDialogComponent(new DialogComponentNumberEdit(
				TrackletCombinerNodeModel.createTimeoutSettings(),
				"Solver Timeout (seconds)"));
	}
}
