package org.knime.knip.trackingrevised.nodes.createtrackingnetwork;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * <code>NodeDialog</code> for the "CreateTrackingNetwork" Node. Creates a
 * tracking network out of a labeling cell and a corresponding segmentation
 * table.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class CreateTrackingNetworkNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring the CreateTrackingNetwork node.
	 */
	@SuppressWarnings("unchecked")
	protected CreateTrackingNetworkNodeDialog() {
		addDialogComponent(new DialogComponentColumnNameSelection(
				CreateTrackingNetworkNodeModel.labelColumnSettings(),
				"Label column", 0, true, StringValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				CreateTrackingNetworkNodeModel.bitmaskColumnSettings(),
				"Bitmask column", 0, ImgPlusValue.class));
		addDialogComponent(new DialogComponentColumnFilter(
				CreateTrackingNetworkNodeModel.featureColumnSettings(), 0,
				true, DoubleValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				CreateTrackingNetworkNodeModel.timeColumnSettings(),
				"Time column", 0, DoubleValue.class));
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings,
			DataTableSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);
	}
}
