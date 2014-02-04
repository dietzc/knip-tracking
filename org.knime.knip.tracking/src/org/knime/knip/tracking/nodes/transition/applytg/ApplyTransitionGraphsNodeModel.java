package org.knime.knip.tracking.nodes.transition.applytg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.network.core.api.GraphObjectIterator;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.KPartiteGraphView;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.GraphFactory;
import org.knime.network.core.knime.cell.GraphValue;
import org.knime.network.core.knime.node.KPartiteGraphViewAndTable2KPartiteGraphViewNodeModel;
import org.knime.network.core.knime.port.GraphPortObjectSpec;


/**
 * This is the model implementation of ApplyTransitionGraphs.
 * Applies transition graphs to a tracking network after a frame based optimisation
 *
 * @author Stephan Sellien
 */
public class ApplyTransitionGraphsNodeModel extends KPartiteGraphViewAndTable2KPartiteGraphViewNodeModel {

	@Override
	protected KPartiteGraphView<PersistentObject, Partition> execute(
			ExecutionContext exec,
			KPartiteGraphView<PersistentObject, Partition> view,
			BufferedDataTable table) throws Exception {
		
		KPartiteGraph<PersistentObject, Partition> net = GraphFactory.createIncrementalNet(view);
		int tgIdx = NodeUtils.firstCompatibleColumn(table.getDataTableSpec(), GraphValue.class, new Integer[0]);
				
		Map<Integer, List<TransitionGraph>> framepairs = new HashMap<Integer, List<TransitionGraph>>();
		
		for(DataRow row : table) {
			TransitionGraph tg = new TransitionGraph(((GraphValue)row.getCell(tgIdx)).getView());
			int startTime = (int)tg.getStartTime();
			List<TransitionGraph> list = framepairs.get(startTime);
			if(list == null) {
				list = new LinkedList<TransitionGraph>();
				framepairs.put(startTime,list);
			}
			list.add(tg);
		}
		
		for(List<TransitionGraph> framepair : framepairs.values()) {
			String pName0 = framepair.get(0).getFirstPartition();
			String pName1 = framepair.get(0).getLastPartition();
			Partition p0 = net.getPartition(pName0);
			Partition p1 = net.getPartition(pName1);
			
			List<PersistentObject> p0Nodes = new LinkedList<PersistentObject>();
			GraphObjectIterator<PersistentObject> it = net.getNodes(p0);
			while(it.hasNext())
				p0Nodes.add(it.next());
			List<PersistentObject> p1Nodes = new LinkedList<PersistentObject>();
			it = net.getNodes(p1);
			while(it.hasNext())
				p1Nodes.add(it.next());
			
			int noNodes = p0Nodes.size() + p1Nodes.size();
						
			for(TransitionGraph tg : framepair) {
				
			}
		}
		
		return net;
	}

	@Override
	protected GraphPortObjectSpec getResultSpec(GraphPortObjectSpec viewSpec,
			DataTableSpec tableSpec) throws InvalidSettingsException {
		
		if(NodeUtils.firstCompatibleColumn(tableSpec, GraphValue.class, new Integer[0]) == -1) {
			throw new InvalidSettingsException("Table must contain transition graphs");
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

