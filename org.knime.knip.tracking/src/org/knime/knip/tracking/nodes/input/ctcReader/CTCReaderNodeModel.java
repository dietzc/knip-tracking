package org.knime.knip.tracking.nodes.input.ctcReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.PartitionSorter;
import org.knime.knip.tracking.util.TransitionGraphUtil;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.knime.node.KPartiteGraphViewAndTable2TableNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of TiffFolder2TransitionGraph. Reads a
 * solved tracking problem as TransitionGraphs. The problem must have the
 * following structure: -raw/ image data -seg/ segmentation -training/ tracking
 * info
 * 
 * @author Stephan Sellien
 */
public class CTCReaderNodeModel<T extends NativeType<T> & IntegerType<T>>
		extends KPartiteGraphViewAndTable2TableNodeModel {
	private SettingsModelString folderSetting = createFolderSetting();

	static SettingsModelString createFolderSetting() {
		return new SettingsModelString("ctcFolderSetting", "");
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		folderSetting.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		folderSetting.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		folderSetting.loadSettingsFrom(settings);
	}

	private DataTableSpec createOutputSpec() {
		return TransitionGraphUtil.createOutSpec();
	}

	@Override
	protected void resetInternal() {

	}

	@Override
	protected BufferedDataTable execute(ExecutionContext exec,
			KPartiteGraphView<PersistentObject, Partition> netView,
			BufferedDataTable table) throws Exception {
		File maindir = new File(folderSetting.getStringValue());
		File trackFile = new File(maindir, "tra/man_track.txt");
		
		KPartiteGraph<PersistentObject, Partition> net = GraphFactory.createIncrementalNet(netView);

		if (!trackFile.exists())
			throw new InvalidSettingsException(
					"man_track.txt was not found in "
							+ trackFile.getAbsolutePath());

		if (table.getRowCount() != 1) {
			throw new IllegalArgumentException("Table must contain base image.");
		}
		@SuppressWarnings("unchecked")
		ImgPlus<T> baseImg = ((ImgPlusValue<T>) table.iterator().next()
				.getCell(0)).getImgPlus();
		
		ImgPlusCellFactory ipcf = new ImgPlusCellFactory(exec);

		DataContainer cont = exec.createDataContainer(createOutputSpec());

		BufferedReader in = new BufferedReader(new FileReader(trackFile));
		String line = null;

		Map<String, Integer> counts = new HashMap<String, Integer>();

		// at first, connect tracks by same label
		List<Partition> partitions = new ArrayList<Partition>(
				net.getPartitions());
		partitions = PartitionSorter.sortTimePartitions(partitions);
		// from time to time
		for (int p = 0; p < partitions.size(); p++) {
			exec.checkCanceled();
			exec.setProgress((double) p / partitions.size());
			// stop if we reach last partition
			if (p == partitions.size() - 1)
				break;
			Partition t0 = partitions.get(p);
			Partition t1 = partitions.get(p + 1);

			exec.setMessage("Working with partition " + t0.getId() + "...");

			// extract frame name (label prefix)
			GraphObjectIterator<PersistentObject> goi = net.getNodes(t0);
			if (!goi.hasNext())
				continue;
			String nodeId = goi.next().getId();
			String frameName = nodeId.substring(0, nodeId.indexOf("_"));
			goi = net.getNodes(t1);
			if (!goi.hasNext())
				continue;
			nodeId = goi.next().getId();
			String frameName1 = nodeId.substring(0, nodeId.indexOf("_"));

			int count = 0;

			for (PersistentObject node : net.getNodes(t0)) {
				String sourceId = node.getId();
				String targetId = sourceId.replace(frameName, frameName1);
				if (net.nodeExists(targetId)) {
					PersistentObject targetNode = net.getNode(targetId);
					TransitionGraph tg = TransitionGraphUtil
							.createTransitionGraphForNetwork(net, t0, t1);
					TrackedNode source = new TrackedNode(net, node);
					TrackedNode target = new TrackedNode(net, targetNode);
					source = source.createCopyIn(tg);
					target = target.createCopyIn(tg);
					tg.createEdge(source, target);
					cont.addRowToTable(new DefaultRow(frameName + "#" + count,
							TransitionGraphUtil.transitionGraph2DataCells(tg,
									baseImg, ipcf)));
					count++;
				}
			}
			counts.put(frameName, count);
		}

		Map<Integer, Integer> trackNoToEndFrame = new HashMap<Integer, Integer>();
		Map<TrackedNode, List<TrackedNode>> toConnect = new HashMap<TrackedNode, List<TrackedNode>>();
		while ((line = in.readLine()) != null) {
			String[] parts = line.split("\\s+");
			int trackNo = Integer.parseInt(parts[0]);
			//int trackStartTime = Integer.parseInt(parts[1]);
			int trackEndTime = Integer.parseInt(parts[2]);
			int parentTrack = Integer.parseInt(parts[3]);

			// remember trackNo and end time to connect later

			trackNoToEndFrame.put(trackNo, trackEndTime);

			if (parentTrack != 0) {
				// TODO: connect them
				int endFrame = trackNoToEndFrame.get(parentTrack);
				Partition t0 = net.getPartition("t" + endFrame);
				Partition t1 = partitions.get(partitions.indexOf(t0) + 1);
				TrackedNode source = null, target = null;
				for (PersistentObject po : net.getNodes(t0)) {
					String id = po.getId();
					int number = Integer
							.parseInt(id.substring(id.indexOf("_") + 1));
					if (number == parentTrack) {
						if(source != null)
							System.err.println("THIS SHOULD NOT HAPPEN! More than one source.");
						source = new TrackedNode(net, po);
					}
				}
				List<TrackedNode> targets = toConnect.get(source);
				if(targets == null) {
					targets = new LinkedList<TrackedNode>();
					toConnect.put(source, targets);
				}
				for (PersistentObject po : net.getNodes(t1)) {
					String id = po.getId();
					int number = Integer
							.parseInt(id.substring(id.indexOf("_") + 1));
					if (number == trackNo) {
						target = new TrackedNode(net, po);
						targets.add(target);
					}
				}

			}
		}

		for (Entry<TrackedNode, List<TrackedNode>> entry : toConnect.entrySet()) {
			// finally create transitiongraph
			TrackedNode source = entry.getKey();
			Partition t0 = net.getPartition(source.getPartition());
			Partition t1 = partitions.get(partitions.indexOf(t0)+1);
			String frameName = source.getID().substring(0,
					source.getID().indexOf("_"));
			TransitionGraph tg = TransitionGraphUtil
					.createTransitionGraphForNetwork(net, t0, t1);
			source = source.createCopyIn(tg);
			for (TrackedNode target : entry.getValue()) {
				target = target.createCopyIn(tg);
				tg.createEdge(source, target);
			}
			// to count further
			int count = counts.get(frameName);
			count++;
			counts.put(frameName, count);
			cont.addRowToTable(new DefaultRow(frameName + "#" + count,
					TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg,
							ipcf)));
		}

		cont.close();

		return exec.createBufferedDataTable(cont.getTable(), exec);
	}

	@Override
	protected DataTableSpec getTableSpec(GraphPortObjectSpec viewSpec,
			DataTableSpec tableSpec) throws InvalidSettingsException {
		File maindir = new File(folderSetting.getStringValue());
		File trackFile = new File(maindir, "tra/man_track.txt");

		if (!trackFile.exists())
			throw new InvalidSettingsException(
					"man_track.txt was not found in "
							+ trackFile.getAbsolutePath());

		if (!tableSpec.containsCompatibleType(ImgPlusValue.class)) {
			throw new InvalidSettingsException(
					"Table must contain ImgPlusValue.");
		}

		return createOutputSpec();
	}
}
