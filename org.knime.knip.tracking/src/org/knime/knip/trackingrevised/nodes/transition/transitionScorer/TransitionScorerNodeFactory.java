package org.knime.knip.trackingrevised.nodes.transition.transitionScorer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransitionScorer" Node. Judges the
 * classified transition results and get the optimal version of each transition
 * graph
 * 
 * @author Stephan Sellien
 */
public class TransitionScorerNodeFactory extends
		NodeFactory<TransitionScorerNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TransitionScorerNodeModel createNodeModel() {
		return new TransitionScorerNodeModel();
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
	public NodeView<TransitionScorerNodeModel> createNodeView(
			final int viewIndex, final TransitionScorerNodeModel nodeModel) {
		return new TransitionScorerNodeView(nodeModel);
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
		return new TransitionScorerNodeDialog();
	}

}
