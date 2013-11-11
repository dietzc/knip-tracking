package org.knime.knip.tracking.nodes.trackingimagegenerator;

import java.io.File;
import java.io.IOException;

import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
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
import org.knime.knip.tracking.nodes.trackingimagegenerator.generator.TrackingImageGenerator;

/**
 * This is the model implementation of TrackletImageGenerator. Creates a
 * tracking image out of a defined 'scripting language'
 * 
 * @author Stephan Sellien
 */
public class TrackingImageGeneratorNodeModel extends NodeModel implements
		BufferedDataTableHolder {

	private final static String CFG_CODE = "cfgCode";

	private SettingsModelString m_code = createCodeSetting();

	static SettingsModelString createCodeSetting() {
		return new SettingsModelString(CFG_CODE,
				TrackingImageGenerator.TESTCODE);
	}

	// for TableCellView
	private BufferedDataTable m_data;

	/**
	 * Constructor for the node model.
	 */
	protected TrackingImageGeneratorNodeModel() {
		super(0, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		TrackingImageGenerator tig = new TrackingImageGenerator();
		ImgPlus<UnsignedByteType> img = ImgPlus.wrap(tig.parse(m_code
				.getStringValue()));
		img.setAxis(new DefaultLinearAxis(Axes.TIME), img.numDimensions() - 1);
		DataContainer cont = exec.createDataContainer(createOutSpec());
		cont.addRowToTable(new DefaultRow("TrackingImage#1",
				new ImgPlusCellFactory(exec).createCell(img)));
		cont.close();
		// for TableCellView
		m_data = exec.createBufferedDataTable(cont.getTable(), exec);
		return new BufferedDataTable[] { m_data };
	}

	private DataTableSpec createOutSpec() {
		return new DataTableSpec(new DataColumnSpecCreator("Image",
				ImgPlusCell.TYPE).createSpec());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		return new DataTableSpec[] { createOutSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_code.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_code.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		SettingsModelString testSetting = createCodeSetting();
		testSetting.loadSettingsFrom(settings);
		new TrackingImageGenerator().parse(testSetting.getStringValue());
		m_code.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	public void setInternalTables(BufferedDataTable[] tables) {
		m_data = tables[0];
	};

}
