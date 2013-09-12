package org.knime.knip.trackingrevised.nodes.transition.transitionEnumerator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionEnumerator" Node. Enumerates all
 * possible transitions of a given transition graph.
 * 
 * @author Stephan Sellien
 */
public class TransitionEnumeratorNodeFactory extends
		NodeFactory<TransitionEnumeratorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransitionEnumeratorNodeModel createNodeModel() {
		return new TransitionEnumeratorNodeModel();
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
	public NodeView<TransitionEnumeratorNodeModel> createNodeView(
			final int viewIndex, final TransitionEnumeratorNodeModel nodeModel) {
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
		return new TransitionEnumeratorNodeDialog();
	}

}
