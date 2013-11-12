package org.knime.knip.tracking.nodes.transition.TransitionGraphs2DistanceMatrix;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionGraphs2DistanceMatrix" Node.
 * Create a distance matrix out of a bunch of transition graphs.
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphs2DistanceMatrixNodeFactory extends
		NodeFactory<TransitionGraphs2DistanceMatrixNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransitionGraphs2DistanceMatrixNodeModel createNodeModel() {
		return new TransitionGraphs2DistanceMatrixNodeModel();
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
	public NodeView<TransitionGraphs2DistanceMatrixNodeModel> createNodeView(
			final int viewIndex,
			final TransitionGraphs2DistanceMatrixNodeModel nodeModel) {
		return new TransitionGraphs2DistanceMatrixNodeView(nodeModel);
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
		return new TransitionGraphs2DistanceMatrixNodeDialog();
	}

}
