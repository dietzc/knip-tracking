package org.knime.knip.trackingrevised.nodes.heldreader;

import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * 
 * @author dietzc, wildnerm University Konstanz
 */
public class HeldReaderNodeDialog<L extends Comparable<L>> extends
		DefaultNodeSettingsPane {

	/**
     * 
     */
	public HeldReaderNodeDialog() {

		addDialogComponent(new DialogComponentColumnNameSelection(
				HeldReaderNodeModel.createSMSourcesColImg(), "Img column: ", 0,
				true, ImgPlusValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				HeldReaderNodeModel.createSMSourcesColX(), "x-values: ", 1,
				true, IntValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				HeldReaderNodeModel.createSMSourcesColY(), "y-values: ", 1,
				true, IntValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				HeldReaderNodeModel.createSMSourcesColT(), "t-values: ", 1,
				true, IntValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(
				HeldReaderNodeModel.createSMSourcesColClass(),
				"class column: ", 1, true, StringValue.class));

	}
}