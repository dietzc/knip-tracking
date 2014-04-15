package org.knime.knip.tracking.nodes.input.botReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
import org.knime.knip.tracking.util.PartitionComparator;
import org.knime.knip.tracking.util.TransitionGraphUtil;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.core.exception.PersistenceException;
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
public class BOTReaderNodeModel<T extends NativeType<T> & IntegerType<T>>
		extends KPartiteGraphViewAndTable2TableNodeModel {
	private SettingsModelString folderSetting = createFolderSetting();

	static SettingsModelString createFolderSetting() {
		return new SettingsModelString("tf2tgFolderSetting", "");
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

//	private List<TrackedNode> avgNodesToUpdate = new LinkedList<TrackedNode>();
	//TODO: create dummy for avgNode
//	private TrackedNode avgNode;

	private void parse(File file,
			KPartiteGraph<PersistentObject, Partition> net, Partition t0,
			Partition t1, String frameName, DataContainer cont, int count,
			ExecutionContext exec, ImgPlus<T> baseImg) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
//		String category = "missing";
		
		ImgPlusCellFactory ipcf = new ImgPlusCellFactory(exec);

		// extract 2nd frameName
		String tmpId, frameName1 = frameName;
		try {
			GraphObjectIterator<PersistentObject> iterator = net.getNodes(t1);
			if (iterator.hasNext()) {
				tmpId = iterator.next().getId();
				frameName1 = tmpId.substring(0, tmpId.indexOf("_"));
			}
		} catch (PersistenceException e1) {
			e1.printStackTrace();
		}

		while ((line = in.readLine()) != null) {
			if (line.startsWith("[")) {
//				category = line.replaceAll("[\\]\\[]", "");
				continue;
			}
			if (line.isEmpty())
				continue;
			String[] parts = line.trim().split(" -> ");
			//System.out.println(Arrays.toString(parts));
			try {
				TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
				// from
				String[] fromParts = parts[0].trim().split("\\s+");
				List<TrackedNode> from = new LinkedList<TrackedNode>();
				for (String fr : fromParts) {
					String nodeName = frameName + "_" + fr;
					PersistentObject pObj = net.getNode(nodeName);
					TrackedNode tn;
					if (fr.equals("-1")) {
						continue;
						//TODO: avgNode
//						tn = avgNode;
					} else {
						tn = new TrackedNode(net, pObj);
					}
					TrackedNode node = tn.createCopyIn(tg);
					from.add(node);
				}
				// to
				String[] toParts = parts[1].trim().split("\\s+");
				List<TrackedNode> to = new LinkedList<TrackedNode>();
				for (String t : toParts) {
					String nodeName = frameName1 + "_" + t;
					PersistentObject pObj = net.getNode(nodeName);
					TrackedNode tn;
					if (t.equals("-1")) {
						continue;
						//TODO: avgNode
						//tn = avgNode;
					} else {
						tn = new TrackedNode(net, pObj);
					}
					TrackedNode node = tn.createCopyIn(tg);
					to.add(node);
				}
				//System.out.println(from + " " + to);
				for (TrackedNode frNode : from) {
					for (TrackedNode toNode : to) {
						tg.createEdge(frNode, toNode);
					}
				}
				cont.addRowToTable(new DefaultRow(frameName + "#" + count,
						TransitionGraphUtil.transitionGraph2DataCells(tg,
								baseImg, ipcf)));
				count++;
			} catch (PersistenceException e) {
				e.printStackTrace();
			}
		}
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
		KPartiteGraph<PersistentObject, Partition> net = GraphFactory.createIncrementalNet(netView);
		File maindir = new File(folderSetting.getStringValue());
		File trainDir = new File(maindir, "training");

		FilenameFilter txtFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		};

		if (table.getRowCount() != 1) {
			throw new IllegalArgumentException("Table must contain base image.");
		}
		@SuppressWarnings("unchecked")
		ImgPlus<T> baseImg = ((ImgPlusValue<T>) table.iterator().next()
				.getCell(0)).getImgPlus();

		DataContainer cont = exec.createDataContainer(createOutputSpec());

		for (File file : trainDir.listFiles(txtFilter)) {
			String frameName = file.getName().substring(0,
					file.getName().indexOf("."));
			System.out.println(file + " -> " + frameName);

			Partition t0 = null, t1 = null;
			int count = 0;

			List<Partition> partitions = new LinkedList<Partition>(
					net.getPartitions());
			Collections.sort(partitions, new PartitionComparator());
			for (Partition partition : partitions) {
				if (t0 != null) {
					t1 = partition;
					break;
				}
				if (partition.getType() != PartitionType.NODE
						|| !partition.getId().startsWith("t"))
					continue;
				GraphObjectIterator<PersistentObject> it = net
						.getNodes(partition);
				if (it.hasNext() && it.next().getId().startsWith(frameName)) {
					System.out.println("Partition found: " + partition.getId());
					t0 = partition;
					continue;
				}
			}
			if (t0 == null || t1 == null)
				throw new IllegalArgumentException(
						"Partitions must not be null: " + t0 + " or" + t1);

			parse(file, net, t0, t1, frameName, cont, count, exec, baseImg);
			count++;
		}

		cont.close();

		return exec.createBufferedDataTable(cont.getTable(), exec);
	}

	@Override
	protected DataTableSpec getTableSpec(GraphPortObjectSpec viewSpec,
			DataTableSpec tableSpec) throws InvalidSettingsException {
		File maindir = new File(folderSetting.getStringValue());
		if (!maindir.exists()) {
			throw new InvalidSettingsException(maindir + " does not exist");
		}
		if (!maindir.isDirectory()) {
			throw new InvalidSettingsException(maindir + " is no directory");
		}
		File segDir = new File(maindir, "seg");
		if (!segDir.exists())
			throw new InvalidSettingsException(segDir + " does not exist");
		File trainDir = new File(maindir, "training");
		if (!trainDir.exists())
			throw new InvalidSettingsException(trainDir + " does not exist");

		if (!tableSpec.containsCompatibleType(ImgPlusValue.class)) {
			throw new InvalidSettingsException(
					"Table must contain ImgPlusValue.");
		}

		return createOutputSpec();
	}
}
