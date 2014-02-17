package org.knime.knip.tracking.nodes.input.ctcReader;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "TiffFolder2TransitionGraph" Node.
 * Reads a solved tracking problem as TransitionGraphs. * nThe problem must have the following structure: * n- raw/     image data * n- seg/     segmentation * n- training/     tracking info
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Sellien
 */
public class CTCReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring TiffFolder2TransitionGraph node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected CTCReaderNodeDialog() {
        super();
        SettingsModelString folderStringModel = CTCReaderNodeModel.createFolderSetting();
        addDialogComponent(new DialogComponentFileChooser(folderStringModel, "tiff2tgFolderSelector", JFileChooser.OPEN_DIALOG, true));
    }
}

