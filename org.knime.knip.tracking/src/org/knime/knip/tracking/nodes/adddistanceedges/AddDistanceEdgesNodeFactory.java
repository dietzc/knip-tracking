package org.knime.knip.tracking.nodes.adddistanceedges;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "AddDistanceEdges" Node. Adds edges with
 * distances to the given tracklet network.
 * 
 * @author Stephan Sellien
 */
public class AddDistanceEdgesNodeFactory extends
		NodeFactory<AddDistanceEdgesNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AddDistanceEdgesNodeModel createNodeModel() {
		return new AddDistanceEdgesNodeModel();
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
	public NodeView<AddDistanceEdgesNodeModel> createNodeView(
			final int viewIndex, final AddDistanceEdgesNodeModel nodeModel) {
		return null;
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
		return new AddDistanceEdgesNodeDialog();
	}

}
