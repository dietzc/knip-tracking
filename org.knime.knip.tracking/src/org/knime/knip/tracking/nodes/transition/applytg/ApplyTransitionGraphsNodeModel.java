package org.knime.knip.tracking.nodes.transition.applytg;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.tracking.data.graph.Edge;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.nodes.trackletcombiner.solver.LPSolveSolver;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.knime.cell.GraphValue;
import org.knime.network.core.knime.node.KPartiteGraphViewAndTable2KPartiteGraphViewNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;

/**
 * This is the model implementation of ApplyTransitionGraphs. Applies transition
 * graphs to a tracking network after a frame based optimisation
 * 
 * @author Stephan Sellien
 */
public class ApplyTransitionGraphsNodeModel extends
		KPartiteGraphViewAndTable2KPartiteGraphViewNodeModel {

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraphView<PersistentObject, Partition> view,
			BufferedDataTable table) throws Exception {

		KPartiteGraph<PersistentObject, Partition> net = GraphFactory
				.createIncrementalNet(view);
		int tgIdx = NodeUtils.firstCompatibleColumn(table.getDataTableSpec(),
				GraphValue.class, new Integer[0]);

		// indexlist
		List<PersistentObject> nodes = new LinkedList<PersistentObject>();
		GraphObjectIterator<PersistentObject> goi = net.getNodes();
		while (goi.hasNext()) {
			nodes.add(goi.next());
		}
		int nodeCount = nodes.size();

		// each tg is a hypothesis + each node as start AND as end for incomplete tracking problems
		int hypoCount = table.getRowCount() + 2*nodeCount;

		double[] propabilities = new double[hypoCount];
		Arrays.fill(propabilities, 1);


		@SuppressWarnings("unchecked")
		List<Integer>[] transposedIndices = new List[2 * nodeCount];
		for (int i = 0; i < transposedIndices.length; i++) {
			transposedIndices[i] = new LinkedList<Integer>();
		}

		int hypoIndex = 0;
		// create optimisation problem
		for (DataRow row : table) {
			TransitionGraph tg = new TransitionGraph(
					((GraphValue) row.getCell(tgIdx)).getView());

			// first partition = start nodes [1st half of array]
			for (TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				int nodeIdx = nodes.indexOf(node.getPersistentObject());
				transposedIndices[nodeIdx].add(hypoIndex);
			}

			// last partition = end nodes [2nd half of array]
			for (TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				int nodeIdx = nodes.indexOf(node.getPersistentObject());
				transposedIndices[nodeIdx + nodeCount].add(hypoIndex);
			}

			hypoIndex++;
		}
		
		//generate each node as start and end hypothesis to make model feasible
		for(PersistentObject node : nodes) {
			int nodeIdx = nodes.indexOf(node);
			transposedIndices[nodeIdx].add(hypoIndex);
			transposedIndices[nodeIdx + nodeCount].add(hypoIndex+1);
			//low value
			propabilities[hypoIndex] = 0.00001;
			propabilities[hypoIndex+1] = 0.00001;
			hypoIndex += 2;
		}

		try {
			double[] result = new LPSolveSolver().solve(transposedIndices, hypoCount,
					propabilities, 0, exec);
			
			RowIterator it = table.iterator();
			for(int hypoIdx = 0; hypoIdx < table.getRowCount(); hypoIdx++) {
				//only apply tgs of input table, not artifical start or end hypos
				DataRow row = it.next();
				TransitionGraph tg = new TransitionGraph(((GraphValue)row.getCell(tgIdx)).getView());
				if(result[hypoIdx] > 0.5) {
					apply(net, tg);
				}
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return net;
	}

	private void apply(KPartiteGraph<PersistentObject, Partition> net,
			TransitionGraph tg) throws PersistenceException {
		for(Edge edge : tg.getEdges()) {
			String start = edge.getStartNode().getID();
			String end = edge.getEndNode().getID();
			PersistentObject source = net.getNode(start);
			PersistentObject target = net.getNode(end);
			net.createEdge(edge.getID(), source, target);
		}
	}

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec viewSpec,
			DataTableSpec tableSpec) throws InvalidSettingsException {

		if (NodeUtils.firstCompatibleColumn(tableSpec, GraphValue.class,
				new Integer[0]) == -1) {
			throw new InvalidSettingsException(
					"Table must contain transition graphs");
		}

		return viewSpec;
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
}
