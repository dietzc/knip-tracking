package org.knime.knip.trackingrevised.nodes.trackletnetwork2labeling;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.Named;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.IntType;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.core.data.img.GeneralMetadata;
import org.knime.knip.core.data.img.ImgMetadataImpl;
import org.knime.knip.trackingrevised.util.OffsetHandling;
import org.knime.knip.trackingrevised.util.TrackingConstants;
import org.knime.network.core.algorithm.search.dfs.WeakConnectedComponent;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.InvalidFeatureException;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.filter.GraphFilter;
import org.knime.network.core.knime.node.KPartiteGraphView2TableNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of TrackletNetwork2Labeling. Extracts the
 * tracklets (parts of trajectories) of a tracking network and creates a
 * labeling image.
 * 
 * @author Stephan Sellien
 */
public class TrackletNetwork2LabelingNodeModel extends
		KPartiteGraphView2TableNodeModel implements BufferedDataTableHolder {

	// needed for TableCellView
	private BufferedDataTable m_data;

	@Override
	protected BufferedDataTable execute(ExecutionContext exec,
			KPartiteGraphView<PersistentObject, Partition> net)
			throws Exception {

		FileStoreFactory fileStoreFactory = FileStoreFactory
				.createWorkflowFileStoreFactory(exec);

		long[] dims = OffsetHandling.decode(net.getFeatureString(net,
				TrackingConstants.NETWORK_FEATURE_DIMENSION));

		NativeImgLabeling<String, IntType> res = new NativeImgLabeling<String, IntType>(
				new ArrayImgFactory<IntType>().create(dims, new IntType()));

		RandomAccess<LabelingType<String>> resRA = res.randomAccess();

		net = new GraphFilter(exec, net, null, null,
				TrackingConstants.DISTANCE_EDGE_PARTITION);

		WeakConnectedComponent wcc = new WeakConnectedComponent(exec, net);
		wcc.start(exec);

		// from time to time to simplify traversing
		for (PersistentObject node : net.getNodes()) {
			String componentStart = wcc.getComponent(node.getId());

			String trackletStartNode = componentStart;

			String temp = trackletStartNode;
			while ((temp = net.getStringFeature(net.getNode(temp),
					TrackingConstants.FEATURE_TRACKLETSTARTNODE)) != null) {
				trackletStartNode = temp;
			}

			String label = "Track " + trackletStartNode;

			if (net.isFeatureDefined(TrackingConstants.FEATURE_TRACKLET_NUMBER)
					&& net.getIntegerFeature(net.getNode(trackletStartNode),
							TrackingConstants.FEATURE_TRACKLET_NUMBER) == -1) {
				// ignore false positives
				// continue;
				label = "false positive";
			}

			drawNodes(net, node, resRA, fileStoreFactory, res, label);

			exec.checkCanceled();
		}

		DataContainer cont = exec.createDataContainer(createOutSpec());
		Named n = new DefaultNamed();
		String rowKey = net.getFeatureString(net,
				TrackingConstants.NETWORK_FEATURE_IMAGE_ROWKEY);
		n.setName(rowKey);
		GeneralMetadata mdata = new ImgMetadataImpl(dims.length);
		mdata.setSource(new DefaultSourced().getSource());

		// mdata.setAxis(Axes.get("X"), 0);
		// mdata.setAxis(Axes.get("Y"), 1);
		// mdata.setAxis(Axes.get("Time"), 2);

		String[] parts = net.getStringFeature(net,
				TrackingConstants.NETWORK_FEATURE_IMAGE_AXES).split("\\|");
		for (int d = 0; d < dims.length; d++) {
			mdata.setAxis(new DefaultCalibratedAxis(Axes.get(parts[d])), d);
		}

		cont.addRowToTable(new DefaultRow(rowKey, new LabelingCellFactory(exec)
				.createCell(res, mdata)));

		cont.close();

		m_data = exec.createBufferedDataTable(cont.getTable(), exec);

		return m_data;
	}

	private DataTableSpec createOutSpec() {
		return new DataTableSpec(new DataColumnSpecCreator("Labeling",
				LabelingCell.TYPE).createSpec());
	}

	@Override
	protected DataTableSpec getTableSpec(GraphPortObjectSpec viewSpec)
			throws InvalidSettingsException {
		return createOutSpec();
	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	private void drawNodes(KPartiteGraphView<PersistentObject, Partition> net,
			PersistentObject node, RandomAccess<LabelingType<String>> resRA,
			FileStoreFactory fileStoreFactory,
			NativeImgLabeling<String, IntType> res, String label)
			throws PersistenceException, InvalidFeatureException {

		// System.out.println(node);

		@SuppressWarnings("unchecked")
		ImgPlus<BitType> seg = ((ImgPlusValue<BitType>) net.getFeatureCell(
				fileStoreFactory, node, TrackingConstants.FEATURE_BITMASK))
				.getImgPlus();

		long[] segMin = new long[seg.numDimensions()];
		seg.min(segMin);

		Cursor<BitType> segCursor = seg.localizingCursor();
		long[] offset = OffsetHandling.decode(net.getStringFeature(node,
				TrackingConstants.FEATURE_BITMASK_OFFSET));
		while (segCursor.hasNext()) {
			segCursor.fwd();
			if (!segCursor.get().get())
				continue;
			for (int d = 0; d < offset.length; d++) {
				if (seg.numDimensions() > d)
					resRA.setPosition(
							Math.min(res.dimension(d), Math.max(0, offset[d]
									+ segCursor.getLongPosition(d))), d);
				else
					resRA.setPosition(
							Math.min(res.dimension(d), Math.max(0, offset[d])),
							d);

			}

			if (resRA.get().getLabeling().isEmpty()) {
				resRA.get().setLabel(label);
			} else {
				ArrayList<String> tmp = new ArrayList<String>(resRA.get()
						.getLabeling());
				tmp.add(label);
				resRA.get().setLabeling(tmp);
			}
		}
	}

	// for Table Cell view
	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	@Override
	public void setInternalTables(BufferedDataTable[] tables) {
		m_data = tables[0];
	}

}
