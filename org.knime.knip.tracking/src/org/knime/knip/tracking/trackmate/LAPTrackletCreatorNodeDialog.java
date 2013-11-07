package org.knime.knip.tracking.trackmate;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

public class LAPTrackletCreatorNodeDialog extends DefaultNodeSettingsPane {

	public LAPTrackletCreatorNodeDialog() {
		addDialogComponent(new DialogComponentNumber(
				LAPTrackletCreatorNodeModel.createMaxRadiusSetting(),
				"Maximum Radius", 1));
		addDialogComponent(new DialogComponentNumber(
				LAPTrackletCreatorNodeModel.createThresholdSetting(),
				"Threshold", 1));
		addDialogComponent(new DialogComponentBoolean(
				LAPTrackletCreatorNodeModel.createGapsSetting(), "Gap Closing"));
	}
}
