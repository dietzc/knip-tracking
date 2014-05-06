package org.knime.knip.tracking.nodes.transition.transitionPatch;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.tracking.data.graph.TrackedNode;
import org.knime.knip.tracking.data.graph.TransitionGraph;
import org.knime.knip.tracking.data.graph.renderer.TransitionGraphRenderer;
import org.knime.knip.tracking.util.TransitionGraphUtil;
import org.knime.network.core.api.KPartiteGraph;
import org.knime.network.core.api.Partition;
import org.knime.network.core.api.PersistentObject;
import org.knime.network.core.core.exception.PersistenceException;
import org.knime.network.core.knime.GraphRepository;
import org.knime.network.core.knime.cell.GraphCellFactory;
import org.knime.network.core.knime.cell.GraphValue;
import org.knime.network.core.knime.port.GraphPortObject;

/**
 * This is the model implementation of TransitionPatchCreator. Creates a
 * transition graph containing the surrounding patch
 * 
 * @author Stephan Sellien
 */
public class TransitionPatchCreatorNodeModel <T extends NativeType<T> & IntegerType<T>> extends
		NodeModel {
	
	public TransitionPatchCreatorNodeModel() {
		super(new PortType[]{GraphPortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
	}

	private SettingsModelInteger m_margin = createMarginModel();

	static SettingsModelInteger createMarginModel() {
		return new SettingsModelInteger("smiMargin", 250);
	}
	
	private SettingsModelBoolean m_renderTG = createRenderModel();
	
	static SettingsModelBoolean createRenderModel() {
		return new SettingsModelBoolean("smbRender", true);
	}

	protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
		@SuppressWarnings("unchecked")
		KPartiteGraph<PersistentObject, Partition> net = (KPartiteGraph<PersistentObject, Partition>) GraphRepository.getInstance().getIncrementalNet(inObjects); 
		BufferedDataTable table = (BufferedDataTable)inObjects[1];
		BufferedDataTable imgTable = (BufferedDataTable)inObjects[2];
		
		@SuppressWarnings("unchecked")
		ImgPlus<T> baseImg = ((ImgPlusValue<T>)imgTable.iterator().next().getCell(0)).getImgPlus();
		
		ImgPlusCellFactory ipcf = new ImgPlusCellFactory(exec);
		
		DataContainer cont = exec.createDataContainer(createOutspec());
		int tgIdx = NodeUtils.firstCompatibleColumn(table.getDataTableSpec(),
				GraphValue.class, new Integer[0]);
		for (DataRow row : table) {
			TransitionGraph tg = new TransitionGraph(
					((GraphValue) row.getCell(tgIdx)).getView());
			TransitionGraph patch = createPatch(tg, net);
			if(!m_renderTG.getBooleanValue()) {
				cont.addRowToTable(new DefaultRow(row.getKey().getString().replaceAll("f", "-"), GraphCellFactory.createCell(patch.getNet())));
			} else {
				ImgPlus<T> img = TransitionGraphRenderer.renderTransitionGraph(patch, baseImg, patch);
				cont.addRowToTable(new DefaultRow(row.getKey().getString().replaceAll("f", "-"), GraphCellFactory.createCell(patch.getNet()), ipcf.createCell(img)));
			}
		}
		cont.close();
		return new PortObject[]{exec.createBufferedDataTable(cont.getTable(), exec)};
	}

	private TransitionGraph createPatch(TransitionGraph tg,
			KPartiteGraph<PersistentObject, Partition> net) {
		try {
			Partition t0 = net.getPartition(tg.getFirstPartition());
			Partition t1 = net.getPartition(tg.getLastPartition());
			Rectangle2D rect = null;
			for(TrackedNode node : tg.getNodes(tg.getFirstPartition())) {
				if(rect == null) rect = node.getImageRectangle();
				else {
					rect.add(node.getImageRectangle());
				}
			}
			for(TrackedNode node : tg.getNodes(tg.getLastPartition())) {
				if(rect == null) rect = node.getImageRectangle();
				else {
					rect.add(node.getImageRectangle());
				}
			}
			//increase by margin
			int margin = m_margin.getIntValue();
			rect.add(rect.getMinX() - margin, rect.getMinY() - margin);
			rect.add(rect.getMaxX() + margin, rect.getMaxY() + margin);
			
			//create resulting tg
			TransitionGraph patch = TransitionGraphUtil.createTransitionGraphForNetwork(net, t0, t1);
			
			//add all nodes in that rectangle
			for(PersistentObject po : net.getNodes(t0,t1)) {
				TrackedNode node = new TrackedNode(net, po);
				if(rect.contains(node.getImageRectangle())) {
					node.createCopyIn(patch);
				}
			}
			
			return patch;
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
		return null;
	}

	private DataTableSpec createOutspec() {
		DataColumnSpec tg = new DataColumnSpecCreator("Transition Graph",
				GraphCellFactory.getType()).createSpec();
		if(!m_renderTG.getBooleanValue())
			return new DataTableSpec(tg);
		else {
			DataColumnSpec img = new DataColumnSpecCreator("Rendered image", ImgPlusCell.TYPE).createSpec();
			return new DataTableSpec(tg,img);
		}
	}
	
	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return new PortObjectSpec[]{createOutspec()};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_margin.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_margin.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_margin.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void reset() {
		// TODO Auto-generated method stub
		
	}
}
