package org.knime.knip.tracking.nodes.transition.TransitionGraphs2DistanceMatrix;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "TransitionGraphs2DistanceMatrix" Node. Create
 * a distance matrix out of a bunch of transition graphs.
 * 
 * @author Stephan Sellien
 */
public class TransitionGraphs2DistanceMatrixNodeView extends
		NodeView<TransitionGraphs2DistanceMatrixNodeModel> {

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class:
	 *            {@link TransitionGraphs2DistanceMatrixNodeModel})
	 */
	protected TransitionGraphs2DistanceMatrixNodeView(
			final TransitionGraphs2DistanceMatrixNodeModel nodeModel) {
		super(nodeModel);

		// TODO instantiate the components of the view here.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {

		// TODO retrieve the new model from your nodemodel and
		// update the view.
		TransitionGraphs2DistanceMatrixNodeModel nodeModel = (TransitionGraphs2DistanceMatrixNodeModel) getNodeModel();
		assert nodeModel != null;

		// be aware of a possibly not executed nodeModel! The data you retrieve
		// from your nodemodel could be null, emtpy, or invalid in any kind.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {

		// TODO things to do when closing the view
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {

		// TODO things to do when opening the view
	}

}
