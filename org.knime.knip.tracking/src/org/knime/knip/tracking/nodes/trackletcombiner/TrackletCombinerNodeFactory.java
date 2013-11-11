package org.knime.knip.tracking.nodes.trackletcombiner;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TrackletCombiner" Node. Combines tracklets
 * to complete tracks.
 * 
 * @author Stephan Sellien
 */
public class TrackletCombinerNodeFactory extends
		NodeFactory<TrackletCombinerNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TrackletCombinerNodeModel createNodeModel() {
		return new TrackletCombinerNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<TrackletCombinerNodeModel> createNodeView(
			final int viewIndex, final TrackletCombinerNodeModel nodeModel) {
		return new TrackletCombinerNodeView(nodeModel);
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
		return new TrackletCombinerNodeDialog();
	}

}
