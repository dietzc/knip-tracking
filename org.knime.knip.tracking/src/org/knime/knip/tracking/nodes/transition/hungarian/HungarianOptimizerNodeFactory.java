package org.knime.knip.tracking.nodes.transition.hungarian;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "HungarianOptimizer" Node. Optimizes given
 * transitions in pairwise frames with the hungarian method to clear equal
 * situations.
 * 
 * @author Stephan Sellien
 */
public class HungarianOptimizerNodeFactory extends
		NodeFactory<HungarianOptimizerNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HungarianOptimizerNodeModel createNodeModel() {
		return new HungarianOptimizerNodeModel();
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
	public NodeView<HungarianOptimizerNodeModel> createNodeView(
			final int viewIndex, final HungarianOptimizerNodeModel nodeModel) {
		return new HungarianOptimizerNodeView(nodeModel);
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
		return new HungarianOptimizerNodeDialog();
	}

}
