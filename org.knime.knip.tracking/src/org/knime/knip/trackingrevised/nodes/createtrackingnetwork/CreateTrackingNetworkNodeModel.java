package org.knime.knip.trackingrevised.nodes.createtrackingnetwork;

import java.util.HashSet;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.trackingrevised.util.OffsetHandling;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.core.GraphMetaData;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.feature.FeatureTypeFactory;
import org.knime.network.core.knime.node.AbstractGraphNodeModel;
import org.knime.network.core.knime.port.GraphPortObject;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

import cern.colt.Arrays;

/**
 * This is the model implementation of CreateTrackingNetwork. Creates a tracking
 * network out of a labeling cell and a corresponding segmentation table.
 * 
 * @author Stephan Sellien
 */
public class CreateTrackingNetworkNodeModel<L extends Comparable<L>, T extends RealType<T>>
		extends AbstractGraphNodeModel {

	private static final String CFG_FEATURECOLS = "featureCols";

	SettingsModelFilterString m_featureColumns = featureColumnSettings();

	static SettingsModelFilterString featureColumnSettings() {
		return new SettingsModelFilterString(CFG_FEATURECOLS);
	}

	private static final String CFG_LABELCOL = "labelCol";

	SettingsModelColumnName m_labelColumn = labelColumnSettings();

	static SettingsModelColumnName labelColumnSettings() {
		return new SettingsModelColumnName(CFG_LABELCOL, "Label");
	}

	private static final String CFG_BITMASKCOL = "bitmaskCol";

	SettingsModelColumnName m_bitmaskColumn = bitmaskColumnSettings();

	static SettingsModelColumnName bitmaskColumnSettings() {
		return new SettingsModelColumnName(CFG_BITMASKCOL, "Bitmask");
	}

	private static final String CFG_TIMECOL = "timeColumn";

	SettingsModelColumnName m_timeColumn = timeColumnSettings();

	static SettingsModelColumnName timeColumnSettings() {
		return new SettingsModelColumnName(CFG_TIMECOL, "Centroid Time");
	}

	NodeLogger logger = NodeLogger
			.getLogger(CreateTrackingNetworkNodeModel.class);

	private static final String NETWORK_NAME = "TrackingNetwork";
	private static final String NETWORK_URI = "anyuri";

	/**
	 * Constructor for the node model.
	 */
	protected CreateTrackingNetworkNodeModel() {
		super(new PortType[] { new PortType(BufferedDataTable.class),
				new PortType(BufferedDataTable.class) },
				new PortType[] { GraphPortObject.TYPE });
	}

	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {

		if (!((DataTableSpec) inSpecs[0])
				.containsCompatibleType(ImgPlusValue.class)) {
			throw new InvalidSettingsException(
					"Table must contain a img plus value column containing the bitmasks.");
		}

		if (!((DataTableSpec) inSpecs[0])
				.containsCompatibleType(LabelingValue.class)) {
			throw new InvalidSettingsException(
					"At least labeling feature must be contained.");
		}

		if (!((DataTableSpec) inSpecs[0])
				.containsCompatibleType(IntervalValue.class)) {
			throw new InvalidSettingsException(
					"A IntervalValue compatible value must be contained in second input.");
		}

		return new PortObjectSpec[] { new GraphPortObjectSpec(
				new GraphMetaData(NETWORK_NAME, NETWORK_URI)) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_featureColumns.saveSettingsTo(settings);
		m_labelColumn.saveSettingsTo(settings);
		m_bitmaskColumn.saveSettingsTo(settings);
		m_timeColumn.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_featureColumns.loadSettingsFrom(settings);
		m_labelColumn.loadSettingsFrom(settings);
		m_bitmaskColumn.loadSettingsFrom(settings);
		m_timeColumn.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_featureColumns.validateSettings(settings);
		m_labelColumn.validateSettings(settings);
		m_bitmaskColumn.validateSettings(settings);
		m_timeColumn.validateSettings(settings);
	}

	@Override
	protected PortObject[] executeInternal(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		BufferedDataTable inDataAsBDT[] = { (BufferedDataTable) inData[0],
				(BufferedDataTable) inData[1] };

		KPartiteGraph<PersistentObject, Partition> net = GraphFactory
				.createNet(NETWORK_NAME, NETWORK_URI);

		int labelIdx = inDataAsBDT[0].getDataTableSpec().findColumnIndex(
				m_labelColumn.getColumnName());
		int timeIdx = inDataAsBDT[0].getDataTableSpec().findColumnIndex(
				m_timeColumn.getColumnName());
		int bitmaskIdx = inDataAsBDT[0].getDataTableSpec().findColumnIndex(
				m_bitmaskColumn.getColumnName());

		Set<Integer> featureIndices = new HashSet<Integer>();
		for (String colName : m_featureColumns.getIncludeList()) {
			featureIndices.add(inDataAsBDT[0].getDataTableSpec()
					.findColumnIndex(colName));
		}

		net.defineFeature(FeatureTypeFactory.getBooleanType(),
				TrackingConstants.FEATURE_ISTRACKLETEND);
		net.defineFeature(FeatureTypeFactory.getType(ImgPlusCell.TYPE),
				TrackingConstants.FEATURE_BITMASK);
		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.FEATURE_BITMASK_OFFSET);

		if (inDataAsBDT[1].getRowCount() != 1) {
			throw new InvalidSettingsException(
					"Second table must contain exactly one image.");
		}

		DataRow imgrow = inDataAsBDT[1].iterator().next();
		DataCell imgcell = imgrow.getCell(0);
		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.NETWORK_FEATURE_IMAGE_ROWKEY);
		net.addFeature(net, TrackingConstants.NETWORK_FEATURE_IMAGE_ROWKEY,
				imgrow.getKey().getString());
		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.NETWORK_FEATURE_DIMENSION);

		// Maximum + 1 = dimensions;
		long[] dimensions = ((IntervalValue) imgcell).getMaximum();
		for (int d = 0; d < dimensions.length; d++) {
			dimensions[d]++;
		}

		net.addFeature(net, TrackingConstants.NETWORK_FEATURE_DIMENSION,
				OffsetHandling.encode(dimensions));
		net.defineFeature(FeatureTypeFactory.getIntegerType(),
				TrackingConstants.NETWORK_FEATURE_TIME_AXIS);
		net.addFeature(
				net,
				TrackingConstants.NETWORK_FEATURE_TIME_AXIS,
				((ImgPlusValue<?>) imgcell).getImgPlus().dimensionIndex(
						Axes.TIME));

		net.defineFeature(FeatureTypeFactory.getStringType(),
				TrackingConstants.NETWORK_FEATURE_IMAGE_AXES);
		String axes = "";
		IntervalValue iv = ((IntervalValue) imgcell);
		for (int d = 0; d < iv.getCalibratedSpace().numDimensions(); d++) {
			axes += iv.getCalibratedSpace().axis(d).type().getLabel();
			if (d < iv.getCalibratedSpace().numDimensions() - 1)
				axes += "|";
		}
		net.addFeature(net, TrackingConstants.NETWORK_FEATURE_IMAGE_AXES, axes);

		net.defineFeature(FeatureTypeFactory.getType(ImgPlusCell.TYPE),
				TrackingConstants.FEATURE_SEGMENT_IMG);

		FileStoreFactory fileStore = FileStoreFactory
				.createNotInWorkflowFileStoreFactory();
		double count = 1.0;
		for (DataRow row : inDataAsBDT[0]) {
			Integer label = Integer
					.parseInt(((org.knime.core.data.StringValue) row
							.getCell(labelIdx)).toString());
			int t = (int) ((DoubleValue) row.getCell(timeIdx)).getDoubleValue();
			// Partition
			String partitionName = "t" + t;
			Partition partition = net.getCreatePartition(partitionName,
					PartitionType.NODE);
			PersistentObject node = net.createNode(label.toString(), partition);
			// every node starts as tracklet end
			net.addFeature(node, TrackingConstants.FEATURE_ISTRACKLETEND, true);
			// net.addFeature(node, TrackingConstants.FEATURE_BITMASK,
			// ((ImgPlusValue<? extends RealType<?>>)row.getCell(bitmaskIdx)));
			net.addFeature(fileStore, node, TrackingConstants.FEATURE_BITMASK,
					row.getCell(bitmaskIdx));
			long[] offset = ((ImgPlusValue<?>) row.getCell(bitmaskIdx))
					.getMinimum();

			net.addFeature(node, TrackingConstants.FEATURE_BITMASK_OFFSET,
					OffsetHandling.encode(offset));
			for (int c = 0; c < row.getNumCells(); c++) {
				if (!featureIndices.contains(c)) {
					continue;
				}
				DataCell cell = row.getCell(c);
				String featureName = inDataAsBDT[0].getDataTableSpec()
						.getColumnSpec(c).getName();
				if (!net.isFeatureDefined(featureName)) {
					net.defineFeature(
							FeatureTypeFactory.getType(cell.getType()),
							featureName);
				}
				net.addFeature(node, featureName, cell);
			}

			// cut out bitmask expression
			ImgPlusCell<BitType> bitmaskCell = (ImgPlusCell<BitType>) row
					.getCell(bitmaskIdx);
			ImgPlus<T> cutImg = cutBitmask(
					((ImgPlusValue<T>) imgcell).getImgPlus(), bitmaskCell);
//			if(count == 1) {
//				AWTImageTools.showInFrame(((ImgPlusValue<T>) imgcell).getImgPlus(), "blubb!");
//				AWTImageTools.showInFrame(cutImg, "bla");
//			}
			net.addFeature(node, TrackingConstants.FEATURE_SEGMENT_IMG, cutImg);

			exec.checkCanceled();
			exec.setProgress(count / inDataAsBDT[0].getRowCount());
			count++;
		}

		net.commit();

		return new PortObject[] { new GraphPortObject<KPartiteGraph<PersistentObject, Partition>>(
				net) };
	}
	
	private ImgPlus<T> cutBitmask(ImgPlus<T> img,
			ImgPlusCell<BitType> bitmaskCell) {
		Img<T> resultImg = ImgUtils.<BitType, T> createEmptyCopy(bitmaskCell
				.getImgPlus().getImg(), img.firstElement().createVariable());
		long[] min = bitmaskCell.getMinimum();
		long[] max = bitmaskCell.getMaximum();
		//min contains offset, max does not!
		for(int d = 0; d < min.length; d++) {
			max[d] += min[d];
		}
		System.out.println(Arrays.toString(min) + " " + Arrays.toString(max));
		IntervalView<T> iv = Views.interval(img, min, max);
		Cursor<T> cursor = iv.cursor();
		for(T pixel : resultImg) {
			cursor.next();
			pixel.set(cursor.get());
		}
		return new ImgPlus<T>(resultImg);
	}

	@Override
	protected void resetInternal() {
		// TODO Auto-generated method stub

	}

}
