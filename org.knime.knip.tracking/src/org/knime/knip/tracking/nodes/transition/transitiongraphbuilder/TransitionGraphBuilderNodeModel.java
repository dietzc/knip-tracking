package org.knime.knip.tracking.nodes.transition.transitiongraphbuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.collection.KDTree;
import net.imglib2.meta.ImgPlus;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.util.PartitionComparator;
import org.knime.knip.tracking.util.TransitionGraphUtil;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.core.PartitionType;
import org.knime.network.core.knime.node.KPartiteGraphViewAndTable2TableNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of TransitionGraphBuilder.
 * 
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphBuilderNodeModel<T extends NativeType<T> & IntegerType<T>>
		extends KPartiteGraphViewAndTable2TableNodeModel {

	// the logger instance
	// private static final NodeLogger logger = NodeLogger
	// .getLogger(TransitionGraphBuilderNodeModel.class);

	/**
	 * the settings key which is used to retrieve and store the settings (from
	 * the dialog or from a settings file) (package visibility to be usable from
	 * the dialog).
	 */
	static final String CFGKEY_DISTANCE = "distance";

	/** initial default distance value. */
	static final double DEFAULT_DISTANCE = 100.0;

	private final SettingsModelDouble m_distance = createDistanceSettings();

	static SettingsModelDouble createDistanceSettings() {
		return new SettingsModelDouble(
				TransitionGraphBuilderNodeModel.CFGKEY_DISTANCE,
				TransitionGraphBuilderNodeModel.DEFAULT_DISTANCE);
	}

	private final static String CENTROID_X = "Centroid X";
	private final static String CENTROID_Y = "Centroid Y";

	/**
	 * Constructor for the node model.
	 */
	protected TransitionGraphBuilderNodeModel() {
	}

	// TODO: reenable check
	protected void checkConfigure(GraphPortObjectSpec spec)
			throws InvalidSettingsException {
		String[] neededFeatures = { CENTROID_X, CENTROID_Y };
		for (String s : neededFeatures) {
			if (!spec.getMetaData().containsFeatureName(s))
				throw new InvalidSettingsException("Feature " + s
						+ " not found.");
		}

	}

	@Override
	protected void resetInternal() {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_distance.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_distance.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_distance.loadSettingsFrom(settings);
	}

	@Override
	protected BufferedDataTable execute(ExecutionContext exec,
			KPartiteGraphView<PersistentObject, Partition> netView,
			BufferedDataTable table) throws Exception {
		KPartiteGraph<PersistentObject, Partition> net = GraphFactory.createIncrementalNet(netView);
		ImgPlusCellFactory ipcf = new ImgPlusCellFactory(exec);
		DataContainer cont = exec.createDataContainer(TransitionGraphUtil
				.createOutSpec());
		List<Partition> partitions = new ArrayList<Partition>(
				net.getPartitions(PartitionType.NODE));
		Collections.sort(partitions, new PartitionComparator());

		@SuppressWarnings("unchecked")
		ImgPlus<T> baseImg = ((ImgPlusValue<T>) table.iterator().next()
				.getCell(0)).getImgPlus();

		List<KDTree<TrackedNode>> trees = new LinkedList<KDTree<TrackedNode>>();
		// build up kdtrees
		for (Partition partition : partitions) {
			exec.checkCanceled();
			List<TrackedNode> objects = new LinkedList<TrackedNode>();
			List<TrackedNode> locations = new LinkedList<TrackedNode>();
			for (PersistentObject pobj : net.getNodes(partition)) {
				TrackedNode node = new TrackedNode(net, pobj);
				objects.add(node);
				locations.add(node);
			}
			trees.add(new KDTree<TrackedNode>(objects, locations));
		}

		// build up transition graphs in respect to distance
		for (int p = 0; p < trees.size() - 1; p++) {
			exec.checkCanceled();
			exec.setProgress((double) p / trees.size(), (p + 1) + " / "
					+ (trees.size() + 1));
			// System.out.println("Partition #" + (p+1));

			KDTree<TrackedNode> tree = trees.get(p);
			KDTree<TrackedNode> nextTree = trees.get(p + 1);
			Partition t0 = net.getPartition(tree.firstElement().getPartition());
			Partition t1 = net.getPartition(nextTree.firstElement()
					.getPartition());

			KDTree<TrackedNode>.KDTreeCursor cursor = tree.cursor();
			int count = 0;
			RadiusNeighborSearchOnKDTree<TrackedNode> rns = new RadiusNeighborSearchOnKDTree<TrackedNode>(nextTree);
			while (cursor.hasNext()) {
				exec.checkCanceled();
				TrackedNode startNode = cursor.next();				
				rns.search(startNode, m_distance.getDoubleValue(), true);
				double lastDist = -1;
				List<TrackedNode> otherNodes = new ArrayList<TrackedNode>();
				for(int n = 0; n < rns.numNeighbors(); n++) {
					double dist = rns.getSquareDistance(n);
					if(lastDist != -1 && dist > 10*lastDist) {
						//System.out.println("break early on " + startNode + " due to: " + dist + " vs " + lastDist);
						break;
					}
					lastDist = dist;
					TrackedNode endNode = rns.getSampler(n).get();
					otherNodes.add(endNode);
				}
				if(otherNodes.isEmpty()) {
					//TODO: readd?
//					TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
//					startNode.createCopyIn(tg);
//					cont.addRowToTable(new DefaultRow(t0.getId() + "#" + count, TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg, ipcf)));
//					count++;
				}
				for(int i = 0; i < otherNodes.size(); i++) {
					TrackedNode endNode1 = otherNodes.get(i);
					{
						TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
						TrackedNode sN = startNode.createCopyIn(tg);
						TrackedNode eN = endNode1.createCopyIn(tg);
						tg.createEdge(sN, eN);
						cont.addRowToTable(new DefaultRow(t0.getId() + "#" + count, TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg, ipcf)));
						count++;
					}
					for(int j = i+1; j < otherNodes.size(); j++) {
						TrackedNode endNode2 = otherNodes.get(j);
						{
							TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
							TrackedNode sN = startNode.createCopyIn(tg);
							TrackedNode eN1 = endNode1.createCopyIn(tg);
							TrackedNode eN2 = endNode2.createCopyIn(tg);
							tg.createEdge(sN, eN1);
							tg.createEdge(sN, eN2);
							cont.addRowToTable(new DefaultRow(t0.getId() + "#" + count, TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg, ipcf)));
							count++;
						}
					}
				}
			}
			//now 
			cursor = nextTree.cursor();
			rns = new RadiusNeighborSearchOnKDTree<TrackedNode>(tree);
			while(cursor.hasNext()) {
				TrackedNode endNode = cursor.next();
				rns.search(endNode, m_distance.getDoubleValue(), true);
				List<TrackedNode> otherNodes = new ArrayList<TrackedNode>();
				double lastDist = -1;
				for(int n = 0; n < rns.numNeighbors(); n++) {
					double dist = rns.getSquareDistance(n);
					if(lastDist != -1 && dist > 10*lastDist) {
						//System.out.println("break early on " + endNode + " due to: " + dist + " vs " + lastDist);
						break;
					}
					lastDist = dist;
					TrackedNode startNode = rns.getSampler(n).get();
					otherNodes.add(startNode);
				}
				if(otherNodes.isEmpty()) {
					//TODO: readd?
//					TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
//					endNode.createCopyIn(tg);
//					cont.addRowToTable(new DefaultRow(t0.getId() + "#" + count, TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg, ipcf)));
//					count++;
				}
				for(int i = 0; i < otherNodes.size(); i++) {
					TrackedNode startNode1 = otherNodes.get(i);
					//1-1 connections already handled in first loop
					for(int j = i+1; j < otherNodes.size(); j++) {
						TrackedNode startNode2 = otherNodes.get(j);
						{
							TransitionGraph tg = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
							TrackedNode eN = endNode.createCopyIn(tg);
							TrackedNode sN1 = startNode1.createCopyIn(tg);
							TrackedNode sN2 = startNode2.createCopyIn(tg);
							tg.createEdge(sN1, eN);
							tg.createEdge(sN2, eN);
							cont.addRowToTable(new DefaultRow(t0.getId() + "#" + count, TransitionGraphUtil.transitionGraph2DataCells(tg, baseImg, ipcf)));
							count++;
						}
					}
				}
			}
		}
		cont.close();

		return exec.createBufferedDataTable(cont.getTable(), exec);
	}

	@Override
	protected DataTableSpec getTableSpec(GraphPortObjectSpec viewSpec,
			DataTableSpec tableSpec) throws InvalidSettingsException {
		return TransitionGraphUtil.createOutSpec();
	}
}
