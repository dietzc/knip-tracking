package org.knime.knip.tracking.nodes.trackletcreator;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

public class TrackletCreatorNodeDialog extends DefaultNodeSettingsPane {

	public TrackletCreatorNodeDialog() {
		addDialogComponent(new DialogComponentNumber(
				TrackletCreatorNodeModel.createTresholdSetting(), "Threshold",
				1));
		addDialogComponent(new DialogComponentNumber(
				TrackletCreatorNodeModel.createSafetyRadiusSetting(),
				"Safety radius", 5));
	}
}
