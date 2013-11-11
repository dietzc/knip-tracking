package org.knime.knip.tracking.nodes.trackingimagegenerator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

/**
 * <code>NodeFactory</code> for the "TrackletImageGenerator" Node. Creates a
 * tracking image out of a defined 'scripting language'
 * 
 * @author Stephan Sellien
 */
public class TrackingImageGeneratorNodeFactory extends
		NodeFactory<TrackingImageGeneratorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TrackingImageGeneratorNodeModel createNodeModel() {
		return new TrackingImageGeneratorNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<TrackingImageGeneratorNodeModel> createNodeView(
			final int viewIndex, final TrackingImageGeneratorNodeModel nodeModel) {
		return new TableCellViewNodeView<TrackingImageGeneratorNodeModel>(
				nodeModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		return new TrackingImageGeneratorNodeDialog();
	}

}
