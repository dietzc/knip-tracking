package org.knime.knip.tracking.nodes.createtrackingnetwork;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CreateTrackingNetwork" Node. Creates a
 * tracking network out of a labeling cell and a corresponding segmentation
 * table.
 * 
 * @author Stephan Sellien
 */
public class CreateTrackingNetworkNodeFactory<L extends Comparable<L>, T extends RealType<T>>
		extends NodeFactory<CreateTrackingNetworkNodeModel<L, T>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CreateTrackingNetworkNodeModel<L, T> createNodeModel() {
		return new CreateTrackingNetworkNodeModel<L, T>();
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
	public NodeView<CreateTrackingNetworkNodeModel<L, T>> createNodeView(
			final int viewIndex,
			final CreateTrackingNetworkNodeModel<L, T> nodeModel) {
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
		return new CreateTrackingNetworkNodeDialog();
	}

}
