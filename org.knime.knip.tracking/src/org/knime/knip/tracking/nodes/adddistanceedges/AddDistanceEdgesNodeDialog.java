package org.knime.knip.tracking.nodes.adddistanceedges;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.nfunk.jep.FunctionTable;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;

public class AddDistanceEdgesNodeDialog extends NodeDialogPane {

	private JList<String> columnList = new JList<String>(
			new DefaultListModel<String>());
	private JList<String> functionList = new JList<String>(
			new DefaultListModel<String>());
	private JList<String> constantList = new JList<String>(
			new DefaultListModel<String>());
	private JTextArea expressionTA = new JTextArea(5, 80);
	private PortObjectSpec spec;
	private JEP jep;

	private JTextField maxDistanceField = new JTextField(20);

	public AddDistanceEdgesNodeDialog() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel();
		northPanel.setLayout(new FlowLayout());

		columnList.setBorder(BorderFactory
				.createTitledBorder("Feature Differences"));
		Dimension dim = new Dimension(200, 200);
		columnList.setMinimumSize(dim);
		columnList.setPreferredSize(dim);
		columnList.setSize(dim);
		columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		columnList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()
						&& columnList.getSelectedValue() != null) {
					expressionTA.insert(columnList.getSelectedValue()
							.toString(), expressionTA.getCaretPosition());
					columnList.clearSelection();
				}
			}
		});
		northPanel.add(new JScrollPane(columnList));

		functionList.setBorder(BorderFactory.createTitledBorder("Functions"));
		functionList.setMinimumSize(dim);
		functionList.setPreferredSize(dim);
		functionList.setSize(dim);
		functionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		functionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()
						&& functionList.getSelectedValue() != null) {
					expressionTA.insert(functionList.getSelectedValue()
							.toString() + "(", expressionTA.getCaretPosition());
					functionList.clearSelection();
				}
			}
		});
		northPanel.add(new JScrollPane(functionList));

		constantList.setBorder(BorderFactory.createTitledBorder("Constants"));
		constantList.setMinimumSize(dim);
		constantList.setPreferredSize(dim);
		constantList.setSize(dim);
		constantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		constantList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()
						&& constantList.getSelectedValue() != null) {
					expressionTA.insert(constantList.getSelectedValue()
							.toString(), expressionTA.getCaretPosition());
					constantList.clearSelection();
				}
			}
		});
		northPanel.add(new JScrollPane(constantList));

		panel.add(northPanel, BorderLayout.NORTH);

		expressionTA.setBorder(BorderFactory.createTitledBorder("Expression"));
		panel.add(new JScrollPane(expressionTA), BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		southPanel.add(new JLabel("Max. Distance"));
		southPanel.add(maxDistanceField);
		panel.add(southPanel, BorderLayout.SOUTH);

		panel.revalidate();
		this.addTab("General", panel);
	}

	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings,
			PortObjectSpec[] specs) throws NotConfigurableException {
		((DefaultListModel<String>) columnList.getModel()).clear();
		List<String> encodedFeatures = JEPHelper.encodeNumericColumns(specs[0]);
		for (String feature : encodedFeatures) {
			((DefaultListModel<String>) columnList.getModel())
					.addElement(feature);
		}
		if (columnList.getModel().getSize() == 0) {
			throw new NotConfigurableException("No numeric features available");
		}
		spec = specs[0];
		jep = JEPHelper.generateJEPwithVariables(spec);
		FunctionTable ft = jep.getFunctionTable();
		for (Object key : ft.keySet()) {
			((DefaultListModel<String>) functionList.getModel()).addElement(key
					.toString());
		}
		SymbolTable st = jep.getSymbolTable();
		for (Object key : st.keySet()) {
			if (encodedFeatures.contains(key.toString()))
				continue;
			((DefaultListModel<String>) constantList.getModel()).addElement(key
					.toString());
		}
		SettingsModelString sms = AddDistanceEdgesNodeModel
				.createExpressionModel();
		try {
			sms.loadSettingsFrom(settings);
			expressionTA.setText(sms.getStringValue());
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
		}

		SettingsModelDouble smd = AddDistanceEdgesNodeModel
				.createMaxDistanceModel();
		try {
			smd.loadSettingsFrom(settings);
			maxDistanceField.setText("" + smd.getDoubleValue());
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
			maxDistanceField.setText("100.0");
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings)
			throws InvalidSettingsException {
		if (expressionTA.getText().trim().isEmpty())
			throw new InvalidSettingsException("Expression must not be empty!");

		jep.parseExpression(expressionTA.getText());
		if (jep.hasError())
			throw new InvalidSettingsException(
					"Expression could not be parsed: " + jep.getErrorInfo());
		SettingsModelString sms = AddDistanceEdgesNodeModel
				.createExpressionModel();
		sms.setStringValue(expressionTA.getText());
		sms.saveSettingsTo(settings);

		SettingsModelDouble smd = AddDistanceEdgesNodeModel
				.createMaxDistanceModel();
		if (maxDistanceField.getText().trim().isEmpty())
			throw new InvalidSettingsException(
					"Max. Distance must not be empty.");
		double value = Double.parseDouble(maxDistanceField.getText());
		if (value <= 0)
			throw new InvalidSettingsException(
					"Max. Distance must be greater than 0.");
		smd.setDoubleValue(value);
		smd.saveSettingsTo(settings);
	}
}
