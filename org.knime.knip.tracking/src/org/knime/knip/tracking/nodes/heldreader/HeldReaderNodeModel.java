/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   30 May 2010 (hornm): created
 */
package org.knime.knip.tracking.nodes.heldreader;

import java.io.File;
import java.io.IOException;

import net.imglib2.RandomAccess;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.util.ImgUtils;

/**
 * 
 * @author dietzc, wildnerm, University of Konstanz
 * @param <T>
 * @param <V>
 */
public class HeldReaderNodeModel<T extends RealType<T>> extends NodeModel {

	private SettingsModelString m_smImgCol = createSMSourcesColImg();
	private SettingsModelString m_smXCol = createSMSourcesColX();
	private SettingsModelString m_smYCol = createSMSourcesColY();
	private SettingsModelString m_smTCol = createSMSourcesColT();
	private SettingsModelString m_smClassCol = createSMSourcesColClass();

	static SettingsModelString createSMSourcesColImg() {
		return new SettingsModelString("sourceColImg", "");
	}

	static SettingsModelString createSMSourcesColX() {
		return new SettingsModelString("sourceColX", "MarkerX");
	}

	static SettingsModelString createSMSourcesColY() {
		return new SettingsModelString("sourceColY", "MarkerY");
	}

	static SettingsModelString createSMSourcesColT() {
		return new SettingsModelString("sourceColT", "MarkerZ");
	}

	static SettingsModelString createSMSourcesColClass() {
		return new SettingsModelString("sourceColClass", "Type");
	}

	protected HeldReaderNodeModel() {
		// 2 input ports
		super(2, 1);
	}

	/**
	 * DataTableSpec
	 */
	private DataTableSpec m_outSpec;
	private int m_imgColIdx;
	private int m_xColIdx;
	private int m_yColIdx;
	private int m_tColIdx;
	private int m_classColIdx;

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// output consists of image and labeling
		m_outSpec = new DataTableSpec(DataTableSpec.createColumnSpecs(
				new String[] { "Img", "Labeling" }, new DataType[] {
						ImgPlusCell.TYPE, LabelingCell.TYPE }));
		return new DataTableSpec[] { m_outSpec };

	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		ImgPlusCellFactory factoryImg = new ImgPlusCellFactory(exec);
		LabelingCellFactory factoryLab = new LabelingCellFactory(exec);

		// column index of image column
		m_imgColIdx = inData[0].getDataTableSpec().findColumnIndex(
				m_smImgCol.getStringValue());
		// column index of x,y,t and class column
		m_xColIdx = inData[1].getDataTableSpec().findColumnIndex(
				m_smXCol.getStringValue());
		m_yColIdx = inData[1].getDataTableSpec().findColumnIndex(
				m_smYCol.getStringValue());
		m_tColIdx = inData[1].getDataTableSpec().findColumnIndex(
				m_smTCol.getStringValue());
		m_classColIdx = inData[1].getDataTableSpec().findColumnIndex(
				m_smClassCol.getStringValue());

		if (m_imgColIdx == -1) {

			if ((m_imgColIdx = NodeUtils.silentOptionalAutoColumnSelection(
					inData[0].getDataTableSpec(), m_smImgCol,
					ImgPlusValue.class)) >= 0) {
				setWarningMessage("Auto-configure Column: "
						+ m_smImgCol.getStringValue());
			} else {
				throw new InvalidSettingsException(
						"No column for img selected!");
			}

		}
		if (m_xColIdx == -1) {
			throw new InvalidSettingsException(
					"No column for x-value selected!");
		}
		if (m_yColIdx == -1) {
			throw new InvalidSettingsException(
					"No column for y-value selected!");
		}
		if (m_tColIdx == -1) {
			throw new InvalidSettingsException(
					"No column for t-value selected!");
		}
		if (m_classColIdx == -1) {
			throw new InvalidSettingsException("No column for class selected!");
		}

		// Get Img from table. We assume that table has only one row so take the
		// first one

		DataRow imgrow = inData[0].iterator().next();
		@SuppressWarnings("unchecked")
		ImgPlus<T> imgPlus = ((ImgPlusValue<T>) imgrow.getCell(m_imgColIdx))
				.getImgPlus();

		// Create labeling to be filled
		Labeling<String> labeling = new NativeImgLabeling<String, ShortType>(
				ImgUtils.createEmptyCopy(imgPlus, new ShortType()));

		RandomAccess<LabelingType<String>> rndAccess = labeling.randomAccess();

		RowIterator pointIterator = inData[1].iterator();
		BufferedDataContainer con = exec.createDataContainer(m_outSpec);

		int total = inData[1].getRowCount();
		int i = 0;

		long[] dims = new long[labeling.numDimensions()];
		labeling.dimensions(dims);

		long[] pos = new long[labeling.numDimensions()];

		int segId = 0;

		DataRow currRow;
		while (pointIterator.hasNext()) {
			currRow = pointIterator.next();

			int x = ((IntValue) currRow.getCell(m_xColIdx)).getIntValue();
			int y = ((IntValue) currRow.getCell(m_yColIdx)).getIntValue();
			int t = ((IntValue) currRow.getCell(m_tColIdx)).getIntValue();

			if (x > dims[0] || y > dims[1] || t > dims[2] || x < 0 || y < 0
					|| t < 0) {
				System.out.println("marker is out of bounces");
				continue;
			}

//			String className = ((StringValue) currRow.getCell(m_classColIdx))
//					.getStringValue();
			pos[0] = x;
			pos[1] = y;
			pos[2] = t;

			rndAccess.setPosition(pos);
			rndAccess.get().setLabeling(new String[] { "Seg:" + segId++ });
		}
		con.addRowToTable(new DefaultRow(imgrow.getKey(), factoryImg
				.createCell(imgPlus), factoryLab.createCell(labeling,
				new DefaultLabelingMetadata(imgPlus, imgPlus, imgPlus,
						new DefaultLabelingColorTable()))));
		exec.setProgress(i++ / total);
		con.close();

		return new BufferedDataTable[] { con.getTable() };
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_smImgCol.saveSettingsTo(settings);
		m_smXCol.saveSettingsTo(settings);
		m_smYCol.saveSettingsTo(settings);
		m_smTCol.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_smImgCol.validateSettings(settings);
		m_smXCol.validateSettings(settings);
		m_smYCol.validateSettings(settings);
		m_smTCol.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_smImgCol.loadSettingsFrom(settings);
		m_smXCol.loadSettingsFrom(settings);
		m_smYCol.loadSettingsFrom(settings);
		m_smTCol.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// Nothing to do here, yet
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

	}
}
