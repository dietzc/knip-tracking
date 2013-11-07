package org.knime.knip.tracking.nodes.transition.hungarian;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "HungarianOptimizer" Node. Optimizes given
 * transitions in pairwise frames with the hungarian method to clear equal
 * situations.
 * 
 * @author Stephan Sellien
 */
public class HungarianOptimizerNodeView extends
		NodeView<HungarianOptimizerNodeModel> {

	/**
	 * Creates a new view.
	 * 
	 * @param nodeModel
	 *            The model (class: {@link HungarianOptimizerNodeModel})
	 */
	protected HungarianOptimizerNodeView(
			final HungarianOptimizerNodeModel nodeModel) {
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
