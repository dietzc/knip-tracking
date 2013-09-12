package org.knime.knip.trackingrevised.nodes.transition.transitionScorer;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "TransitionScorer" Node. Judges the classified
 * transition results and get the optimal version of each transition graph
 * 
 * @author Stephan Sellien
 */
public class TransitionScorerNodeView extends
		NodeView<TransitionScorerNodeModel> {

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link TransitionScorerNodeModel})
	 */
	protected TransitionScorerNodeView(final TransitionScorerNodeModel nodeModel) {
		super(nodeModel);
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		// TODO: generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// TODO: generated method stub
	}

}
