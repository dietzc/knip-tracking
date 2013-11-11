package org.knime.knip.tracking.nodes.trackingimagegenerator;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;

/**
 * <code>NodeDialog</code> for the "TrackletImageGenerator" Node. Creates a
 * tracking image out of a defined 'scripting language'
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class TrackingImageGeneratorNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the TrackletImageGenerator node.
	 */
	protected TrackingImageGeneratorNodeDialog() {
		addDialogComponent(new DialogComponentMultiLineString(
				TrackingImageGeneratorNodeModel.createCodeSetting(), "Code"));
	}
}
